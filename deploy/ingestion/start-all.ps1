[CmdletBinding()]
param(
    [string]$Config = "",
    [string]$Limit = "",
    [string]$Sudo = "sudo",
    [switch]$Check
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$shellScript = Join-Path $scriptDir "deploy.sh"
if (-not (Get-Command bash -ErrorAction SilentlyContinue)) {
    throw "bash was not found. Run deploy\ingestion\deploy.sh from WSL/Git Bash, or install Git for Windows."
}

$arguments = @($shellScript)
if ($Config) { $arguments += @("--config", $Config) }
if ($Limit) { $arguments += @("--limit", $Limit) }
if ($Sudo) { $arguments += @("--sudo", $Sudo) }
if ($Check) { $arguments += "--check" }
& bash @arguments
if ($LASTEXITCODE -ne 0) { throw "ingestion deploy.sh failed with exit code $LASTEXITCODE." }
