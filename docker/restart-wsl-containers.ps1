[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$Distribution = "Ubuntu"
)

$ErrorActionPreference = "Stop"

$containers = @(
    "postgresql",
    "docker_mongodb_1",
    "docker_redis_1",
    "docker_rabbitmq_1",
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

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    throw "wsl.exe was not found. Install or enable Windows Subsystem for Linux first."
}

& wsl.exe -d $Distribution -- docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Cannot connect to Docker in WSL distribution '$Distribution'."
}

& wsl.exe -d $Distribution -- docker inspect @containers *> $null
if ($LASTEXITCODE -ne 0) {
    throw "One or more configured containers do not exist in WSL distribution '$Distribution'."
}

if ($PSCmdlet.ShouldProcess(($containers -join ", "), "Restart WSL Docker containers")) {
    Write-Host "Restarting WSL Docker containers..."
    Invoke-WslDocker -DockerArguments (@("restart") + $containers)
}

Write-Host ""
Write-Host "Container status:"
Invoke-WslDocker -DockerArguments (@("inspect", "--format", "{{.Name}}  {{.State.Status}}") + $containers)
