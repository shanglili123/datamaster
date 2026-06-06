[CmdletBinding()]
param(
    [string]$Config = "",
    [string]$Limit = "",
    [string]$Sudo = "sudo"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$shellScript = Join-Path $scriptDir "start-all.sh"

if (-not (Test-Path -LiteralPath $shellScript)) {
    throw "Shell deploy script not found: $shellScript"
}

if (-not (Get-Command bash -ErrorAction SilentlyContinue)) {
    throw "bash was not found. Run deploy\start-all.sh from WSL/Git Bash, or install Git for Windows."
}

$arguments = @($shellScript)
if (-not [string]::IsNullOrWhiteSpace($Config)) {
    $arguments += @("--config", $Config)
}
if (-not [string]::IsNullOrWhiteSpace($Limit)) {
    $arguments += @("--limit", $Limit)
}
if (-not [string]::IsNullOrWhiteSpace($Sudo)) {
    $arguments += @("--sudo", $Sudo)
}

& bash @arguments
if ($LASTEXITCODE -ne 0) {
    throw "deploy/start-all.sh failed with exit code $LASTEXITCODE."
}
