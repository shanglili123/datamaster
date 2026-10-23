[CmdletBinding()]
param(
    [string]$Config = "",
    [string]$Sudo = "sudo",
    [string]$TargetHost = "",
    [switch]$Restore,
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Component,
    [Parameter(Mandatory = $true, Position = 1)]
    [string]$RemotePath
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$shellScript = Join-Path $scriptDir "rollback.sh"
if (-not (Test-Path -LiteralPath $shellScript)) {
    throw "Rollback shell script not found: $shellScript"
}
if (-not (Get-Command bash -ErrorAction SilentlyContinue)) {
    throw "bash was not found. Run deploy\rollback.sh from WSL/Git Bash, or install Git for Windows."
}

$arguments = @($shellScript)
if (-not [string]::IsNullOrWhiteSpace($Config)) {
    $arguments += @("--config", $Config)
}
if (-not [string]::IsNullOrWhiteSpace($Sudo)) {
    $arguments += @("--sudo", $Sudo)
}
if (-not [string]::IsNullOrWhiteSpace($TargetHost)) {
    $arguments += @("--host", $TargetHost)
}
if ($Restore) {
    $arguments += @("--restore")
} else {
    $arguments += @("--list")
}
$arguments += @($Component, $RemotePath)

& bash @arguments
if ($LASTEXITCODE -ne 0) {
    throw "deploy/rollback.sh failed with exit code $LASTEXITCODE."
}
