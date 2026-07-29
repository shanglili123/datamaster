# fix-encoding-bom.ps1
# 清除 Java/XML 文件中的 UTF-8 BOM 头

param([string]$Root = "D:\dev\dataMaster")

$ErrorActionPreference = "Stop"
$count = 0

$bom = [byte[]]@(0xEF, 0xBB, 0xBF)

$extensions = @("*.java", "*.xml")
foreach ($ext in $extensions) {
    Get-ChildItem -Path $Root -Filter $ext -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch '\\target\\' -and $_.FullName -notmatch '\\node_modules\\' } |
        ForEach-Object {
            $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
            if ($bytes.Length -ge 3 -and $bytes[0] -eq $bom[0] -and $bytes[1] -eq $bom[1] -and $bytes[2] -eq $bom[2]) {
                $newBytes = [byte[]]::new($bytes.Length - 3)
                [Array]::Copy($bytes, 3, $newBytes, 0, $bytes.Length - 3)
                [System.IO.File]::WriteAllBytes($_.FullName, $newBytes)
                $count++
                Write-Host "  [FIX] $($_.FullName.Substring($Root.Length+1))" -ForegroundColor Green
            }
        }
}

Write-Host ""
Write-Host "Fixed $count files with BOM" -ForegroundColor Cyan
