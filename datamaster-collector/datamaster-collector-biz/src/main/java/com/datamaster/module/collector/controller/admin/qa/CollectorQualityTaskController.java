

package com.datamaster.module.collector.controller.admin.qa;

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
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskAssetReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskPageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskSaveReqVO;
import com.datamaster.module.collector.convert.qa.CollectorQualityTaskConvert;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskDO;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskService;

/**
 * 数据质量任务Controller
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Tag(name = "数据质量任务")
@RestController
@RequestMapping("/dpp/qualityTask")
@Validated
public class CollectorQualityTaskController extends BaseController {
    @Resource
    private ICollectorQualityTaskService CollectorQualityTaskService;

    @Operation(summary = "查询数据质量任务列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorQualityTaskRespVO>> list(CollectorQualityTaskPageReqVO CollectorQualityTask) {
        PageResult<CollectorQualityTaskDO> page = CollectorQualityTaskService.getCollectorQualityTaskPage(CollectorQualityTask);
        return CommonResult.success(BeanUtils.toBean(page, CollectorQualityTaskRespVO.class));
    }

    @Operation(summary = "导出数据质量任务列表")
    @Log(title = "数据质量任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorQualityTaskPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorQualityTaskDO> list = (List<CollectorQualityTaskDO>) CollectorQualityTaskService.getCollectorQualityTaskPage(exportReqVO).getRows();
        ExcelUtil<CollectorQualityTaskRespVO> util = new ExcelUtil<>(CollectorQualityTaskRespVO.class);
        util.exportExcel(response, CollectorQualityTaskConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据质量任务列表")
    @Log(title = "数据质量任务", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorQualityTaskRespVO> util = new ExcelUtil<>(CollectorQualityTaskRespVO.class);
        List<CollectorQualityTaskRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorQualityTaskService.importCollectorQualityTask(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据质量任务详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorQualityTaskRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorQualityTaskRespVO CollectorQualityTaskDO = CollectorQualityTaskService.getCollectorQualityTaskById(id);
        return CommonResult.success(CollectorQualityTaskDO);
    }

    @Operation(summary = "获取数据质量任务详细信息")
    @GetMapping( "/getQualityTaskAsset")
    public CommonResult<CollectorQualityTaskRespVO> getQualityTaskAsset(CollectorQualityTaskAssetReqVO CollectorQualityTaskAssetReqVO) {
        CollectorQualityTaskRespVO CollectorQualityTaskDO = CollectorQualityTaskService.getQualityTaskAsset(CollectorQualityTaskAssetReqVO);
        return CommonResult.success(CollectorQualityTaskDO);
    }

    @Operation(summary = "新增数据质量任务")
    @Log(title = "数据质量任务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorQualityTaskSaveReqVO CollectorQualityTask) {
        CollectorQualityTask.setCreatorId(getUserId());
        CollectorQualityTask.setCreateBy(getNickName());
        CollectorQualityTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityTaskService.createCollectorQualityTask(CollectorQualityTask));
    }

    @Operation(summary = "修改数据质量任务")
    @Log(title = "数据质量任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorQualityTaskSaveReqVO CollectorQualityTask) {
        CollectorQualityTask.setUpdatorId(getUserId());
        CollectorQualityTask.setUpdateBy(getNickName());
        CollectorQualityTask.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityTaskService.updateCollectorQualityTask(CollectorQualityTask));
    }

    @Operation(summary = "删除数据质量任务")
    @Log(title = "数据质量任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorQualityTaskService.removeCollectorQualityTask(Arrays.asList(ids)));
    }


    @Operation(summary = "修改数据质量任务")
    @PostMapping("/updateCollectorQualityTaskStatus")
    public AjaxResult updateDaDiscoveryTaskStatus(@RequestBody CollectorQualityTaskSaveReqVO daDiscoveryTask)
    {
        boolean result = CollectorQualityTaskService.updateCollectorQualityTaskStatus(daDiscoveryTask);
        return result ? success() : error("任务不存在或已过期！");
    }

    @Log(title = "触发一次定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/startCollectorQualityTask/{id}")
    public AjaxResult startDaDiscoveryTask(@PathVariable("id") Long id)
    {
        return CollectorQualityTaskService.startCollectorQualityTask(id);
    }


    @Log(title = "数据质量任务状态修改", businessType = BusinessType.UPDATE)
    @PostMapping("/updateDaDiscoveryTaskCronExpression")
    public AjaxResult updateDaDiscoveryTaskCronExpression(@RequestBody CollectorQualityTaskSaveReqVO daDiscoveryTask)
    {
        boolean result = CollectorQualityTaskService.updateDaDiscoveryTaskCronExpression(daDiscoveryTask);
        return result ? success() : error("任务不存在或已过期！");
    }

}
