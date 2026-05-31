package com.datamaster.module.catalog.controller.admin.task;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.catalog.controller.admin.task.vo.*;
import com.datamaster.module.catalog.convert.task.CatalogTaskConvert;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskDO;
import com.datamaster.module.catalog.service.task.ICatalogTaskService;
import com.datamaster.security.annotation.BizDataScope;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 采集任务Controller
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Tag(name = "采集任务")
@RestController
@RequestMapping("/mc/task")
@Validated
public class CatalogTaskController extends BaseController {
    @Resource
    private ICatalogTaskService CatalogTaskService;

    @Operation(summary = "查询采集任务列表")
    @BizDataScope(code = "Catalog_task_list", userField = "leader", deptField = "responsibleDept")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTaskRespVO>> list(CatalogTaskPageReqVO CatalogTask) {
        PageResult<CatalogTaskDO> page = CatalogTaskService.getCatalogTaskPage(CatalogTask);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTaskRespVO.class));
    }

    @Operation(summary = "导出采集任务列表")
    @BizDataScope(code = "Catalog_task_list", userField = "leader", deptField = "responsibleDept")
    @Log(title = "采集任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTaskPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTaskDO> list = (List<CatalogTaskDO>) CatalogTaskService.getCatalogTaskPage(exportReqVO).getRows();
        ExcelUtil<CatalogTaskRespVO> util = new ExcelUtil<>(CatalogTaskRespVO.class);
        util.exportExcel(response, CatalogTaskConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入采集任务列表")
    @Log(title = "采集任务", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CatalogTaskRespVO> util = new ExcelUtil<>(CatalogTaskRespVO.class);
        List<CatalogTaskRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CatalogTaskService.importCatalogTask(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取采集任务详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTaskRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(CatalogTaskService.getCatalogTaskByIdNew(id));
    }

    @Operation(summary = "新增采集任务")
    @Log(title = "采集任务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTaskSaveReqVO CatalogTask) {
        CatalogTask.setCreatorId(getUserId());
        CatalogTask.setCreateBy(getNickName());
        CatalogTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskService.createCatalogTask(CatalogTask));
    }

    @Operation(summary = "修改采集任务")
    @Log(title = "采集任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTaskSaveReqVO CatalogTask) {
        CatalogTask.setUpdatorId(getUserId());
        CatalogTask.setUpdateBy(getNickName());
        CatalogTask.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskService.updateCatalogTask(CatalogTask));
    }

    @Operation(summary = "删除采集任务")
    @Log(title = "采集任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTaskService.removeCatalogTask(Arrays.asList(ids)));
    }

    @Operation(summary = "删除采集任务")
    @Log(title = "采集任务", businessType = BusinessType.DELETE)
    @GetMapping("/batchDeleteCheck/{ids}")
    public CommonResult<BatchDeleteCheck<Long>> batchDeleteCheck(@PathVariable Long[] ids) {
        BatchDeleteCheck<Long> result = CatalogTaskService.batchDeleteCheck(Arrays.asList(ids));
        return CommonResult.success(result);
    }

    @Operation(summary = "获取采集范围信息")
    @GetMapping(value = "/getRealtimeCatalogTaskScopeList/{id}")
    public CommonResult<List<CatalogTaskScopeRespVO>> getRealtimeCatalogTaskScopeList(@PathVariable("id") Long id) {
        return CommonResult.success(BeanUtils.toBean(CatalogTaskService.getRealtimeCatalogTaskScopeList(id), CatalogTaskScopeRespVO.class));
    }



    @Operation(summary = "上下线-任务")
    @PostMapping("/updateReleaseJobTask")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult updateReleaseJobTask(@Valid @RequestBody CatalogTaskSaveReqVO CatalogTask) {
        Map<String, Object> result = CatalogTaskService.updateReleaseJobTask(CatalogTask);
        return CommonResult.success(result);
    }

    @Operation(summary = "上下线-调度")
    @PostMapping("/updateReleaseSchedule")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult updateReleaseSchedule(@Valid @RequestBody CatalogTaskSaveReqVO CatalogTask) {
        Map<String, Object> result = CatalogTaskService.updateReleaseSchedule(CatalogTask);
        return CommonResult.success(result);
    }

    @Operation(summary = "上下线-调度")
    @PostMapping("/runJobOnce")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult runJobOnce(@Valid @RequestBody CatalogTaskSaveReqVO CatalogTask) {
        CatalogTask.setCreatorId(super.getUserId());
        CatalogTask.setCreateBy(super.getNickName());
        Map<String, Object> result = CatalogTaskService.runJobOnce(CatalogTask);
        return CommonResult.success(result);
    }

    @Operation(summary = "获取来源系统树形结构")
    @GetMapping("/sourceSystemTree")
    public CommonResult<List<CatalogTaskSourceTreeRespVO>> getSourceSystemTree() {
        List<CatalogTaskSourceTreeRespVO> treeList = CatalogTaskService.getSourceSystemTree();
        return CommonResult.success(treeList);
    }

    @GetMapping("/test")
    public void test() {
        System.out.println("33333333333333");
        CatalogTaskService.runDaDiscoveryTask(3L);
    }
}
