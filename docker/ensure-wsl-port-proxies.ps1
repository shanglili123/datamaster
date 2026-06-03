[CmdletBinding()]
param(
    [string]$Distribution = "Ubuntu"
)

$ErrorActionPreference = "Stop"

function Get-WslPrimaryIp {
    param([string]$Distro)

    $rawIpOutput = & wsl.exe -d $Distro -- hostname -I
    $ip = @($rawIpOutput -split '\s+' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) | Select-Object -First 1
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($ip)) {
        throw "Cannot determine WSL IP for distribution '$Distro'."
    }
    return $ip.Trim()
}

function Test-TcpEndpoint {
    param(
        [string]$RemoteHost,
        [int]$Port
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync($RemoteHost, $Port)
        if (-not $task.Wait(1000)) {
            return $false
        }
        return $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Stop-ExistingProxy {
    param([string]$PidPath)

    if (-not (Test-Path -LiteralPath $PidPath)) {
        return
    }

    $existingPid = Get-Content -LiteralPath $PidPath -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($existingPid) {
        $existingProcess = Get-Process -Id ([int]$existingPid) -ErrorAction SilentlyContinue
        if ($existingProcess) {
            Stop-Process -Id $existingProcess.Id -Force
        }
    }

    Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue
}

$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCommand) {
    throw "Node.js is required to start local TCP proxies."
}

$targetHost = Get-WslPrimaryIp -Distro $Distribution
$proxyScript = Join-Path $PSScriptRoot "wsl-port-proxy.js"
$proxySpecs = @(
    @{ Name = "mongodb"; ListenPort = 27017; TargetPort = 27017 },
    @{ Name = "rabbitmq-amqp"; ListenPort = 5673; TargetPort = 5673 },
    @{ Name = "rabbitmq-ui"; ListenPort = 15673; TargetPort = 15673 },
    @{ Name = "zookeeper"; ListenPort = 2181; TargetPort = 2181 },
    @{ Name = "dolphinscheduler-api"; ListenPort = 12345; TargetPort = 12345 }
)

foreach ($proxy in $proxySpecs) {
    $listenPort = [int]$proxy.ListenPort
    $targetPort = [int]$proxy.TargetPort
    $name = [string]$proxy.Name
    $pidFile = Join-Path $PSScriptRoot (".wsl-port-proxy-{0}.pid" -f $listenPort)

    if (Test-TcpEndpoint -RemoteHost "127.0.0.1" -Port $listenPort) {
        Write-Host ("localhost:{0} already reachable, skip {1}" -f $listenPort, $name)
        continue
    }

    Stop-ExistingProxy -PidPath $pidFile

    $argumentList = @($proxyScript, "127.0.0.1", $listenPort, $targetHost, $targetPort)
    $process = Start-Process -FilePath $nodeCommand.Source -ArgumentList $argumentList -WindowStyle Hidden -PassThru
    Set-Content -LiteralPath $pidFile -Value $process.Id -NoNewline -Force

    $ready = $false
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        Start-Sleep -Milliseconds 500

        $runningProcess = Get-Process -Id $process.Id -ErrorAction SilentlyContinue
        if (-not $runningProcess) {
            break
        }

        if (Test-TcpEndpoint -RemoteHost "127.0.0.1" -Port $listenPort) {
            $ready = $true
            break
        }
    }

    if (-not $ready) {
        Stop-ExistingProxy -PidPath $pidFile
        throw ("Local port proxy did not come up on localhost:{0} for {1}" -f $listenPort, $name)
    }

    Write-Host ("Proxy ready: localhost:{0} -> {1}:{2} ({3})" -f $listenPort, $targetHost, $targetPort, $name)
}
