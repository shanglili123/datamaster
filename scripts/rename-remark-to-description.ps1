# rename-remark-to-description.ps1
# 删除业务 VO/DO 中重复的 remark 字段，保留 description
# 仅处理 Java 文件中的字段声明和 getter/setter

param(
    [switch]$DryRun,
    [string]$Root = "D:\dev\dataMaster"
)

$ErrorActionPreference = "Stop"

$excludeDirs = @(
    "datamaster-api-ds",
    "datamaster-etl",
    "node_modules", "target", "dist", ".git", ".idea",
    "docs", "sql", "deploy", "docker", "scripts", "upload",
    "logs", "tmp", "tmp-doris-diag", "dbgpt-configs"
)

$excludeFiles = @(
    "BaseEntity.java",
    "AiAskSessionDO.java",
    "AiAskMessageDO.java"
)

function Should-SkipFile {
    param([string]$filePath)
    if ($filePath -match '\\target\\') { return $true }
    foreach ($dir in $excludeDirs) {
        if ($filePath -match [regex]::Escape("\$dir\")) { return $true }
    }
    foreach ($f in $excludeFiles) {
        if ((Split-Path $filePath -Leaf) -eq $f) { return $true }
    }
    return $false
}

$changedFiles = @()
$count = 0

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Remark -> Description 规范化工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($DryRun) {
    Write-Host "[DRY RUN] 仅扫描，不修改文件" -ForegroundColor Yellow
}
Write-Host ""

$javaFiles = Get-ChildItem -Path $Root -Filter "*.java" -Recurse -File -ErrorAction SilentlyContinue

foreach ($file in $javaFiles) {
    $p = $file.FullName
    if (Should-SkipFile $p) { continue }

    $relativePath = $p.Substring($Root.Length + 1)
    $content = Get-Content -Path $p -Raw -Encoding UTF8
    $originalContent = $content

    # 检查文件是否同时有 remark 和 description
    $hasRemark = $content -match 'private String remark\b'
    $hasDescription = $content -match 'private String description\b'

    if (-not $hasRemark) { continue }

    # 情况1: 同时有 remark 和 description -> 删除 remark 字段及其 getter/setter
    if ($hasRemark -and $hasDescription) {
        # 删除 private String remark 字段声明（含上面的注解行）
        $content = $content -replace '(?m)\s*@Schema\(description = "备注"[^\)]*\)\s*\r?\n\s*@Size\([^\)]*\)\s*\r?\n\s*private String remark;', ''
        $content = $content -replace '(?m)\s*@Schema\(description = "备注"[^\)]*\)\s*\r?\n\s*private String remark;', ''
        $content = $content -replace '(?m)\s*private String remark;', ''

        # 删除 getter/setter
        $content = $content -replace '(?ms)\s*public String getRemark\(\) \{.*?return remark;\s*\}', ''
        $content = $content -replace '(?ms)\s*public void setRemark\(String remark\) \{.*?this\.remark = remark;\s*\}', ''

        # 删除 @Excel(name = "") 注解（如果在 remark 上面）
        $content = $content -replace '(?m)^\s*@Excel\(name = ""\)\s*$', ''

        if ($content -ne $originalContent) {
            $count++
            if ($DryRun) {
                Write-Host "  [DRY] $relativePath (remark+description 共存，删除 remark)" -ForegroundColor Yellow
            } else {
                try {
                    [System.IO.File]::WriteAllText($p, $content, [System.Text.Encoding]::UTF8)
                    Write-Host "  [OK] $relativePath (remark+description 共存，删除 remark)" -ForegroundColor Green
                } catch {
                    Write-Host "  [SKIP] $relativePath (locked)" -ForegroundColor DarkYellow
                }
            }
            $changedFiles += $relativePath
        }
    }
    # 情况2: 只有 remark 没有 description -> 重命名 remark 为 description
    elseif ($hasRemark -and -not $hasDescription) {
        $content = $content -replace 'private String remark;', 'private String description;'
        $content = $content -replace 'public String getRemark\(\)', 'public String getDescription()'
        $content = $content -replace 'return remark;', 'return description;'
        $content = $content -replace 'public void setRemark\(String remark\)', 'public void setDescription(String description)'
        $content = $content -replace 'this\.remark = remark;', 'this.description = description;'
        $content = $content -replace '@Schema\(description = "备注"', '@Schema(description = "描述"'

        if ($content -ne $originalContent) {
            $count++
            if ($DryRun) {
                Write-Host "  [DRY] $relativePath (只有 remark，重命名为 description)" -ForegroundColor Yellow
            } else {
                try {
                    [System.IO.File]::WriteAllText($p, $content, [System.Text.Encoding]::UTF8)
                    Write-Host "  [OK] $relativePath (只有 remark，重命名为 description)" -ForegroundColor Green
                } catch {
                    Write-Host "  [SKIP] $relativePath (locked)" -ForegroundColor DarkYellow
                }
            }
            $changedFiles += $relativePath
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 扫描完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 修改文件数: $count" -ForegroundColor $(if ($count -gt 0) { "Green" } else { "Gray" })

if ($count -gt 0) {
    Write-Host ""
    Write-Host "已修改的文件:" -ForegroundColor Green
    foreach ($f in $changedFiles) {
        Write-Host "  $f" -ForegroundColor White
    }
}

if ($DryRun) {
    Write-Host ""
    Write-Host "确认无误后，去掉 -DryRun 参数执行实际替换" -ForegroundColor Yellow
}
