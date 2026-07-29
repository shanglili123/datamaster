# rename-project-to-space.ps1
# Project -> Space 规范化自动替换工具
# 排除: datamaster-api-ds 模块、AI 问数模块、DS 第三方协议引用

param(
    [switch]$DryRun,
    [string]$Root = "D:\dev\dataMaster"
)

$ErrorActionPreference = "Stop"
$changedFiles = @()
$skippedFiles = @()

# ============================================================
# 排除目录（整个目录跳过）
# ============================================================
$excludeDirs = @(
    "datamaster-api-ds",
    "datamaster-etl",
    "node_modules",
    "target",
    "dist",
    ".git",
    ".idea",
    "docs",
    "sql",
    "deploy",
    "docker",
    "scripts",
    "upload",
    "logs",
    "tmp",
    "tmp-doris-diag",
    "dbgpt-configs",
    "${project.build.directory}"
)

# 排除的文件（AI 问数模块、DS 第三方协议工具类）
$excludeFiles = @(
    # AI 问数模块
    "AiAskSessionDO.java",
    "AiAskMessageDO.java",
    "AiAskSessionMapper.java",
    "IAiAskSessionService.java",
    "AiAskSessionServiceImpl.java",
    "AiAskDataServiceImpl.java",
    "AiAskSessionController.java",
    "AiAskSessionSaveReqVO.java",
    "AiAskSessionRespVO.java",
    "AiAskDataPrepareReqVO.java",
    "AiAskDataReportReqVO.java",
    "AiAskDataSqlReqVO.java",
    # DS 第三方协议
    "DataMasterDSApiType.java",
    "DsRequestUtils.java",
    "DsProjectServiceImpl.java",
    "IDsProjectService.java",
    "DsProjectUpdateReqDTO.java",
    "DsProjectCreateReqDTO.java",
    "DsEtlTaskServiceImpl.java",
    "IDsEtlTaskService.java",
    "DsEtlSchedulerServiceImpl.java",
    "IDsEtlSchedulerService.java",
    "DsEtlExecutorServiceImpl.java",
    "IDsEtlExecutorService.java",
    "DsEtlNodeServiceImpl.java",
    "IDsEtlNodeService.java",
    "EtlApplication.java",
    "CatalogTaskDolphinSchedulerService.java"
)

# ============================================================
# 替换规则（按顺序执行，先长后短避免部分匹配）
# ============================================================
# 格式: @{Pattern = "正则"; Replace = "替换文本"}
$replacements = @(
    # ---- 方法名 ----
    @{Pattern = "selectMenuTreeByUserIdAndProjectId"; Replace = "selectMenuTreeByUserIdAndSpaceId"},
    @{Pattern = "selectUserByUserIdAndProjectId";     Replace = "selectUserByUserIdAndSpaceId"},
    @{Pattern = "selectRoleAllByProjectId";           Replace = "selectRoleAllBySpaceId"},
    @{Pattern = "getByUserIdAndProjectId";            Replace = "getByUserIdAndSpaceId"},
    @{Pattern = "getProjectUserRelPage";              Replace = "getSpaceUserRelPage"},
    @{Pattern = "checkRoleNameUniqueAndProjectId";    Replace = "checkRoleNameUniqueAndSpaceId"},
    @{Pattern = "checkRoleKeyUniqueAndProjectId";     Replace = "checkRoleKeyUniqueAndSpaceId"},
    @{Pattern = "getHomeStats";                       Replace = "getHomeStats"},  # param names handled below
    @{Pattern = "selectListByProjectId";              Replace = "selectListBySpaceId"},

    # ---- getter/setter 方法名 ----
    @{Pattern = "getProjectId";   Replace = "getSpaceId"},
    @{Pattern = "setProjectId";   Replace = "setSpaceId"},
    @{Pattern = "getProjectCode"; Replace = "getSpaceCode"},
    @{Pattern = "setProjectCode"; Replace = "setSpaceCode"},
    @{Pattern = "getProjectName"; Replace = "getSpaceName"},
    @{Pattern = "setProjectName"; Replace = "setSpaceName"},
    @{Pattern = "getProjectList"; Replace = "getSpaceList"},
    @{Pattern = "setProjectList"; Replace = "setSpaceList"},

    # ---- 字段名（驼峰） ----
    @{Pattern = "projectIdList";  Replace = "spaceIdList"},
    @{Pattern = "projectListOld"; Replace = "spaceListOld"},
    @{Pattern = "projectId";      Replace = "spaceId"},
    @{Pattern = "projectCode";    Replace = "spaceCode"},
    @{Pattern = "projectName";    Replace = "spaceName"},
    @{Pattern = "projectList";    Replace = "spaceList"},

    # ---- XML property / column 映射 ----
    @{Pattern = 'property="projectId"';  Replace = 'property="spaceId"'},
    @{Pattern = 'property="projectCode"'; Replace = 'property="spaceCode"'},
    @{Pattern = 'property="projectName"'; Replace = 'property="spaceName"'},

    # ---- XML / MyBatis 参数 ----
    @{Pattern = '#\{projectId';  Replace = '#{spaceId'},
    @{Pattern = '#\{projectCode'; Replace = '#{spaceCode'},
    @{Pattern = '#\{projectName'; Replace = '#{spaceName'},
    @{Pattern = '\$\{projectId'; Replace = '${spaceId'},
    @{Pattern = '\$\{projectCode'; Replace = '${spaceCode'},

    # ---- XML if-test ----
    @{Pattern = 'test="projectId';  Replace = 'test="spaceId'},
    @{Pattern = 'test="projectCode'; Replace = 'test="spaceCode'},

    # ---- @Param 注解 ----
    @{Pattern = '@Param\("projectId"\)';  Replace = '@Param("spaceId")'},
    @{Pattern = '@Param\("projectCode"\)'; Replace = '@Param("spaceCode")'},
    @{Pattern = '@Param\("projectName"\)'; Replace = '@Param("spaceName")'},

    # ---- @RequestParam 注解 ----
    @{Pattern = 'RequestParam\(value = "projectId"';  Replace = 'RequestParam(value = "spaceId"'},
    @{Pattern = 'RequestParam\(value = "projectCode"'; Replace = 'RequestParam(value = "spaceCode"'},
    @{Pattern = 'RequestParam\(value = "projectName"'; Replace = 'RequestParam(value = "spaceName"'},

    # ---- Map key 字符串 ----
    @{Pattern = '\.put\("projectId"';   Replace = '.put("spaceId"'},
    @{Pattern = '\.put\("projectCode"'; Replace = '.put("spaceCode"'},
    @{Pattern = '\.put\("projectName"'; Replace = '.put("spaceName"'},
    @{Pattern = '"projectId"';          Replace = '"spaceId"'},
    @{Pattern = '"projectCode"';        Replace = '"spaceCode"'},
    @{Pattern = '"projectName"';        Replace = '"spaceName"'},

    # ---- SQL 别名 ----
    @{Pattern = 'AS projectId';  Replace = 'AS spaceId'},
    @{Pattern = 'AS projectCode'; Replace = 'AS spaceCode'},
    @{Pattern = 'AS projectName'; Replace = 'AS spaceName'},

    # ---- SQL 列名（仅本系统） ----
    @{Pattern = 'PROJECT_ID';   Replace = 'SPACE_ID'},
    @{Pattern = 'PROJECT_CODE'; Replace = 'SPACE_CODE'},
    @{Pattern = 'project_id';   Replace = 'space_id'},
    @{Pattern = 'project_code'; Replace = 'space_code'},
    @{Pattern = 'TAX_PROJECT';  Replace = 'TAX_SPACE'}
)

# ============================================================
# 扫描并替换
# ============================================================
function Should-SkipFile {
    param([string]$filePath)
    # 快速跳过构建产物目录
    if ($filePath -match '\\target\\') { return $true }
    if ($filePath -match '\\\$\{project\.build\.directory\}\\') { return $true }
    foreach ($dir in $excludeDirs) {
        if ($filePath -match [regex]::Escape("\$dir\")) { return $true }
    }
    foreach ($f in $excludeFiles) {
        if ((Split-Path $filePath -Leaf) -eq $f) { return $true }
    }
    return $false
}

function Get-TargetFiles {
    $extensions = @("*.java", "*.xml", "*.vue", "*.js")
    $files = @()
    foreach ($ext in $extensions) {
        $files += Get-ChildItem -Path $Root -Filter $ext -Recurse -File -ErrorAction SilentlyContinue
    }
    return $files
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Project -> Space 自动化替换工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN] 仅扫描，不修改文件" -ForegroundColor Yellow
    Write-Host ""
}

$allFiles = Get-TargetFiles
Write-Host "扫描到 $($allFiles.Count) 个目标文件" -ForegroundColor Gray
Write-Host ""

$processedCount = 0
foreach ($file in $allFiles) {
    $relativePath = $file.FullName.Substring($Root.Length + 1)

    if (Should-SkipFile $file.FullName) {
        $skippedFiles += $relativePath
        continue
    }

    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $fileChanged = $false

    foreach ($rule in $replacements) {
        try {
            $newContent = [regex]::Replace($content, $rule.Pattern, $rule.Replace)
            if ($newContent -ne $content) {
                $content = $newContent
                $fileChanged = $true
            }
        } catch {
            Write-Host "  [WARN] 正则错误: $($rule.Pattern) in $relativePath : $_" -ForegroundColor Yellow
        }
    }

    if ($fileChanged) {
        $processedCount++
        if ($DryRun) {
            Write-Host "  [DRY] $relativePath" -ForegroundColor Yellow
        } else {
            try {
                [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
                Write-Host "  [OK] $relativePath" -ForegroundColor Green
            } catch {
                Write-Host "  [SKIP] $relativePath (locked: $($_.Exception.Message))" -ForegroundColor DarkYellow
            }
        }
        $changedFiles += $relativePath
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 扫描完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 修改文件数: $($changedFiles.Count)" -ForegroundColor $(if ($changedFiles.Count -gt 0) { "Green" } else { "Gray" })
Write-Host " 跳过文件数: $($skippedFiles.Count)" -ForegroundColor Gray
Write-Host ""

if ($changedFiles.Count -gt 0) {
    Write-Host "已修改的文件:" -ForegroundColor Green
    foreach ($f in $changedFiles) {
        Write-Host "  $f" -ForegroundColor White
    }
    Write-Host ""
}

Write-Host "跳过的文件 (AI问数/DS协议):" -ForegroundColor Gray
foreach ($f in $skippedFiles) {
    Write-Host "  $f" -ForegroundColor DarkGray
}

if ($DryRun) {
    Write-Host ""
    Write-Host "确认无误后，去掉 -DryRun 参数执行实际替换" -ForegroundColor Yellow
}
