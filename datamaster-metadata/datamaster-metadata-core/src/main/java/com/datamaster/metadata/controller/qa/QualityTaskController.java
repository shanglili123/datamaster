

package com.datamaster.metadata.controller.qa;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import cn.hutool.core.date.DateUtil;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.metadata.controller.qa.vo.QualityTaskAssetReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskSaveReqVO;
import com.datamaster.metadata.controller.qa.vo.QualitySummaryQueryVO;
import com.datamaster.metadata.convert.qa.QualityTaskConvert;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;
import com.datamaster.metadata.service.qa.IQualityTaskService;

import java.util.HashMap;
import java.util.Map;

/**
 * 质量探查任务Controller
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Tag(name = "质量探查任务")
@RestController
@RequestMapping("/metadata/qualityTask")
@Validated
public class QualityTaskController extends BaseController {
    @Resource
    private IQualityTaskService QualityTaskService;

    @Operation(summary = "查询质量探查任务列表")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityTaskRespVO>> list(QualityTaskPageReqVO QualityTask) {
        PageResult<QualityTaskDO> page = QualityTaskService.getQualityTaskPage(QualityTask);
        return CommonResult.success(BeanUtils.toBean(page, QualityTaskRespVO.class));
    }

    @Operation(summary = "导出质量探查任务列表")
    @Log(title = "质量探查任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityTaskPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityTaskDO> list = (List<QualityTaskDO>) QualityTaskService.getQualityTaskPage(exportReqVO).getRows();
        ExcelUtil<QualityTaskRespVO> util = new ExcelUtil<>(QualityTaskRespVO.class);
        util.exportExcel(response, QualityTaskConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入质量探查任务列表")
    @Log(title = "质量探查任务", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityTaskRespVO> util = new ExcelUtil<>(QualityTaskRespVO.class);
        List<QualityTaskRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = QualityTaskService.importQualityTask(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取质量探查任务详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityTaskRespVO> getInfo(@PathVariable("id") Long id) {
        QualityTaskRespVO QualityTaskDO = QualityTaskService.getQualityTaskById(id);
        return CommonResult.success(QualityTaskDO);
    }

    @Operation(summary = "获取质量探查任务详细信息")
    @GetMapping( "/getQualityTaskAsset")
    public CommonResult<QualityTaskRespVO> getQualityTaskAsset(QualityTaskAssetReqVO QualityTaskAssetReqVO) {
        QualityTaskRespVO QualityTaskDO = QualityTaskService.getQualityTaskAsset(QualityTaskAssetReqVO);
        return CommonResult.success(QualityTaskDO);
    }

    @Operation(summary = "获取数据源表的最近一次质量探查结果摘要")
    @GetMapping("/latestQualitySummary")
    public CommonResult<com.datamaster.metadata.api.qa.dto.QualitySummaryRespDTO> latestQualitySummary(
            @RequestParam("datasourceId") Long datasourceId,
            @RequestParam("tableName") String tableName) {
        return CommonResult.success(QualityTaskService.getLatestQualitySummary(datasourceId, tableName));
    }

    @Operation(summary = "批量获取数据源表的最近一次质量探查结果摘要")
    @PostMapping("/batchQualitySummary")
    public CommonResult<Map<String, com.datamaster.metadata.api.qa.dto.QualitySummaryRespDTO>> batchQualitySummary(
            @RequestBody List<QualitySummaryQueryVO> queryList) {
        Map<String, com.datamaster.metadata.api.qa.dto.QualitySummaryRespDTO> result = new HashMap<>();
        if (queryList != null && !queryList.isEmpty()) {
            for (QualitySummaryQueryVO query : queryList) {
                result.put(buildQualitySummaryKey(query.getDatasourceId(), query.getTableName()),
                        QualityTaskService.getLatestQualitySummary(query.getDatasourceId(), query.getTableName()));
            }
        }
        return CommonResult.success(result);
    }

    private String buildQualitySummaryKey(Long datasourceId, String tableName) {
        return datasourceId + ":" + (tableName == null ? "" : tableName);
    }

    @Operation(summary = "新增质量探查任务")
    @Log(title = "质量探查任务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityTaskSaveReqVO QualityTask) {
        QualityTask.setCreatorId(getUserId());
        QualityTask.setCreateBy(getNickName());
        QualityTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(QualityTaskService.createQualityTask(QualityTask));
    }

    @Operation(summary = "修改质量探查任务")
    @Log(title = "质量探查任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityTaskSaveReqVO QualityTask) {
        QualityTask.setUpdatorId(getUserId());
        QualityTask.setUpdateBy(getNickName());
        QualityTask.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(QualityTaskService.updateQualityTask(QualityTask));
    }

    @Operation(summary = "删除质量探查任务")
    @Log(title = "质量探查任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(QualityTaskService.removeQualityTask(Arrays.asList(ids)));
    }


    @Operation(summary = "修改质量探查任务")
    @PostMapping("/updateQualityTaskStatus")
    public AjaxResult updateDaDiscoveryTaskStatus(@RequestBody QualityTaskSaveReqVO daDiscoveryTask)
    {
        boolean result = QualityTaskService.updateQualityTaskStatus(daDiscoveryTask);
        return result ? success() : error("任务不存在或已过期！");
    }

    @Log(title = "触发一次定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/startQualityTask/{id}")
    public AjaxResult startDaDiscoveryTask(@PathVariable("id") Long id)
    {
        return QualityTaskService.startQualityTask(id);
    }


    @Log(title = "质量探查任务状态修改", businessType = BusinessType.UPDATE)
    @PostMapping("/updateDaDiscoveryTaskCronExpression")
    public AjaxResult updateDaDiscoveryTaskCronExpression(@RequestBody QualityTaskSaveReqVO daDiscoveryTask)
    {
        boolean result = QualityTaskService.updateDaDiscoveryTaskCronExpression(daDiscoveryTask);
        return result ? success() : error("任务不存在或已过期！");
    }

}
