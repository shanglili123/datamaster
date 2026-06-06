[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$Distribution = "Ubuntu",
    [switch]$EnsureDolphinSchedulerLocalhost
)

$ErrorActionPreference = "Stop"

$containerAliases = @(
    "postgresql",
    "docker_redis_1",
    "dolphinscheduler-zookeeper",
    "dolphinscheduler-master",
    "dolphinscheduler-worker",
    "dolphinscheduler-alert",
    "dolphinscheduler-api"
)

function Invoke-WslDocker {
    param(
        [string[]]$DockerArguments
    )

    & wsl.exe -d $Distribution -- docker @DockerArguments
    if ($LASTEXITCODE -ne 0) {
        throw "WSL Docker command failed: docker $($DockerArguments -join ' ')"
    }
}

function Get-WslDockerNames {
    $names = & wsl.exe -d $Distribution -- docker ps -a --format "{{.Names}}"
    if ($LASTEXITCODE -ne 0) {
        throw "WSL Docker command failed: docker ps -a --format {{.Names}}"
    }
    return @($names | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Resolve-ContainerName {
    param(
        [string]$Alias,
        [string[]]$AvailableNames
    )

    $exactMatch = $AvailableNames | Where-Object { $_ -eq $Alias } | Select-Object -First 1
    if ($exactMatch) {
        return $exactMatch
    }

    $suffixMatch = $AvailableNames | Where-Object { $_ -like "*_$Alias" } | Select-Object -First 1
    if ($suffixMatch) {
        return $suffixMatch
    }

    throw "Configured container '$Alias' does not exist in WSL distribution '$Distribution'."
}

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    throw "wsl.exe was not found. Install or enable Windows Subsystem for Linux first."
}

& wsl.exe -d $Distribution -- docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Cannot connect to Docker in WSL distribution '$Distribution'."
}

$availableNames = Get-WslDockerNames
$containers = @($containerAliases | ForEach-Object { Resolve-ContainerName -Alias $_ -AvailableNames $availableNames })

if ($PSCmdlet.ShouldProcess(($containers -join ", "), "Restart WSL Docker containers")) {
    Write-Host "Restarting WSL Docker containers..."
    Invoke-WslDocker -DockerArguments (@("restart") + $containers)
}

Write-Host ""
Write-Host "Resolved container names:"
$containerAliases | ForEach-Object -Begin { $index = 0 } -Process {
    Write-Host ("{0} -> {1}" -f $_, $containers[$index])
    $index++
}

Write-Host ""
Write-Host "Container status:"
Invoke-WslDocker -DockerArguments (@("inspect", "--format", "{{.Name}}  {{.State.Status}}") + $containers)

if ($EnsureDolphinSchedulerLocalhost) {
    Write-Host ""
    Write-Host "Ensuring Windows localhost access to DolphinScheduler API..."
    & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "ensure-wsl-port-proxies.ps1") -Distribution $Distribution
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to ensure Windows localhost access to DolphinScheduler API."
    }
}
