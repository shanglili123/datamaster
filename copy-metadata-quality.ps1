$SourceBase = "D:\dev\dataMaster\datamaster-collector\datamaster-collector-core\src\main\java"
$DestBase = "D:\dev\dataMaster\datamaster-metadata\datamaster-metadata-core\src\main\java"

# Define all source files relative to SourceBase
$SourceFiles = @(
    # qa controllers
    "com\datamaster\module\collector\controller\admin\qa\CollectorQualityTaskController.java",
    "com\datamaster\module\collector\controller\admin\qa\CollectorQualityTaskEvaluateController.java",
    "com\datamaster\module\collector\controller\admin\qa\CollectorQualityErrorStorageConfigController.java",
    "com\datamaster\module\collector\controller\admin\qa\CollectorQualityTaskObjController.java",
    # qa controller VOs
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskSaveReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskRespVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskPageReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskObjSaveReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskObjRespVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskObjPageReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskEvaluateSaveReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskEvaluateRespVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskEvaluatePageReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskAssetRespVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CollectorQualityTaskAssetReqVO.java",
    "com\datamaster\module\collector\controller\admin\qa\vo\CheckErrorDataReqDTO.java",
    # etl controllers (quality log)
    "com\datamaster\module\collector\controller\admin\etl\CollectorQualityLogController.java",
    "com\datamaster\module\collector\controller\admin\etl\CollectorEvaluateLogController.java",
    # etl controller VOs (quality log only)
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorQualityLogSaveReqVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorQualityLogRespVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorQualityLogPageReqVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorEvaluateLogSaveReqVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorEvaluateLogRespVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorEvaluateLogPageReqVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\CollectorEvaluateLogStatisticsVO.java",
    "com\datamaster\module\collector\controller\admin\etl\vo\LogResult.java",
    # qa services
    "com\datamaster\module\collector\service\qa\ICollectorQualityTaskService.java",
    "com\datamaster\module\collector\service\qa\ICollectorQualityTaskObjService.java",
    "com\datamaster\module\collector\service\qa\ICollectorQualityTaskEvaluateService.java",
    "com\datamaster\module\collector\service\qa\impl\CollectorQualityTaskServiceImpl.java",
    "com\datamaster\module\collector\service\qa\impl\CollectorQualityTaskObjServiceImpl.java",
    "com\datamaster\module\collector\service\qa\impl\CollectorQualityTaskEvaluateServiceImpl.java",
    # etl services (quality log)
    "com\datamaster\module\collector\service\etl\ICollectorQualityLogService.java",
    "com\datamaster\module\collector\service\etl\ICollectorEvaluateLogService.java",
    "com\datamaster\module\collector\service\etl\impl\CollectorQualityLogServiceImpl.java",
    "com\datamaster\module\collector\service\etl\impl\CollectorEvaluateLogServiceImpl.java",
    # qa DOs
    "com\datamaster\module\collector\dal\dataobject\qa\CollectorQualityTaskObjDO.java",
    "com\datamaster\module\collector\dal\dataobject\qa\CollectorQualityTaskEvaluateDO.java",
    "com\datamaster\module\collector\dal\dataobject\qa\CollectorQualityTaskDO.java",
    "com\datamaster\module\collector\dal\dataobject\qa\CollectorQualityErrorStorageConfigDO.java",
    # etl DOs (quality log)
    "com\datamaster\module\collector\dal\dataobject\etl\CollectorQualityLogDO.java",
    "com\datamaster\module\collector\dal\dataobject\etl\CollectorEvaluateLogDO.java",
    # qa mappers
    "com\datamaster\module\collector\dal\mapper\qa\CollectorQualityTaskObjMapper.java",
    "com\datamaster\module\collector\dal\mapper\qa\CollectorQualityTaskMapper.java",
    "com\datamaster\module\collector\dal\mapper\qa\CollectorQualityTaskEvaluateMapper.java",
    "com\datamaster\module\collector\dal\mapper\qa\CollectorQualityErrorStorageConfigMapper.java",
    # etl mappers (quality log)
    "com\datamaster\module\collector\dal\mapper\etl\CollectorQualityLogMapper.java",
    "com\datamaster\module\collector\dal\mapper\etl\CollectorEvaluateLogMapper.java",
    # qa converts
    "com\datamaster\module\collector\convert\qa\CollectorQualityTaskObjConvert.java",
    "com\datamaster\module\collector\convert\qa\CollectorQualityTaskEvaluateConvert.java",
    "com\datamaster\module\collector\convert\qa\CollectorQualityTaskConvert.java",
    # etl converts (quality log)
    "com\datamaster\module\collector\convert\etl\CollectorQualityLogConvert.java",
    "com\datamaster\module\collector\convert\etl\CollectorEvaluateLogConvert.java",
    # utils
    "com\datamaster\module\collector\utils\CollectorTaskConverter.java"
)

$CreatedFiles = @()

foreach ($RelPath in $SourceFiles) {
    $SourcePath = Join-Path $SourceBase $RelPath
    if (-not (Test-Path $SourcePath)) {
        Write-Warning "Source file not found: $SourcePath"
        continue
    }

    # Read content
    $Content = Get-Content -LiteralPath $SourcePath -Raw

    # 1. Replace package: com.datamaster.module.collector -> com.datamaster.metadata
    $Content = $Content -replace 'com\.datamaster\.module\.collector', 'com.datamaster.metadata'

    # 2. Remove .admin. from package-like references (e.g., com.datamaster.metadata.controller.admin.qa -> com.datamaster.metadata.controller.qa)
    $Content = $Content -replace '(?<=com\.datamaster\.metadata\.[a-zA-Z]+)\.admin(?=\.)', ''

    # 3. Replace CollectorQuality -> Quality, CollectorEvaluate -> Evaluate, CollectorError -> Error
    $Content = $Content -replace 'CollectorQuality', 'Quality'
    $Content = $Content -replace 'CollectorEvaluate', 'Evaluate'
    $Content = $Content -replace 'CollectorError', 'Error'

    # 4. Replace URL mapping /col/ -> /metadata/
    $Content = $Content -replace '@RequestMapping\("/col/"\)', '@RequestMapping("/metadata/")'

    # Determine new filename: remove "Collector" prefix
    $FileName = Split-Path $RelPath -Leaf
    if ($FileName -like "Collector*") {
        $NewFileName = $FileName.Substring("Collector".Length)
    } else {
        $NewFileName = $FileName
    }

    # Determine destination relative path:
    # - Replace "module\collector" with "metadata" in the path
    # - Remove "admin\" from the path
    # - Use new filename
    $DestRelPath = $RelPath -replace 'module\\collector', 'metadata'
    $DestRelPath = $DestRelPath -replace '\\admin\\', '\'

    # Replace filename at end
    $DestDir = Split-Path $DestRelPath -Parent
    $DestRelPath = "$DestDir\$NewFileName"

    $DestPath = Join-Path $DestBase $DestRelPath
    $DestDirPath = Split-Path $DestPath -Parent

    # Create destination directory if needed
    if (-not (Test-Path $DestDirPath)) {
        New-Item -ItemType Directory -Path $DestDirPath -Force | Out-Null
    }

    # Write transformed content
    $Utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($DestPath, $Content, $Utf8NoBom)

    $CreatedFiles += $DestPath
    Write-Output "Created: $DestPath"
}

Write-Output "`n=== Complete list of created files ==="
$CreatedFiles | ForEach-Object { Write-Output $_ }
Write-Output "`nTotal files created: $($CreatedFiles.Count)"
