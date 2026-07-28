package com.datamaster.module.assets.controller.admin.discovery;

import cn.hutool.core.date.DateUtil;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskSaveReqVO;
import com.datamaster.module.assets.convert.discovery.AssetsDiscoveryTaskConvert;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.module.assets.service.discovery.IAssetsDiscoveryTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据发现任务Controller
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Tag(name = "数据发现任务")
@RestController
@RequestMapping("/ast/discoveryTask")
@Validated
public class AssetsDiscoveryTaskController extends BaseController {

    @Resource
    private IAssetsDiscoveryTaskService assetsDiscoveryTaskService;

    @Operation(summary = "查询数据发现任务列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsDiscoveryTaskRespVO>> list(AssetsDiscoveryTaskPageReqVO assetsDiscoveryTask) {
        PageResult<AssetsDiscoveryTaskDO> page = assetsDiscoveryTaskService.getDaDiscoveryTaskPage(assetsDiscoveryTask);
        return CommonResult.success(BeanUtils.toBean(page, AssetsDiscoveryTaskRespVO.class));
    }

    @Operation(summary = "查询数据发现任务列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:list')")
    @GetMapping("/getDaDiscoveryTaskListPage")
    public CommonResult<PageResult<AssetsDiscoveryTaskRespVO>> getDaDiscoveryTaskListPage(AssetsDiscoveryTaskPageReqVO assetsDiscoveryTask) {
        return CommonResult.success(assetsDiscoveryTaskService.getDaDiscoveryTaskListPage(assetsDiscoveryTask));
    }

    @Operation(summary = "导出数据发现任务列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:export')")
    @Log(title = "数据发现任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsDiscoveryTaskPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsDiscoveryTaskDO> list = (List<AssetsDiscoveryTaskDO>) assetsDiscoveryTaskService
                .getDaDiscoveryTaskPage(exportReqVO).getRows();
        ExcelUtil<AssetsDiscoveryTaskRespVO> util = new ExcelUtil<>(AssetsDiscoveryTaskRespVO.class);
        util.exportExcel(response, AssetsDiscoveryTaskConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据发现任务列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:import')")
    @Log(title = "数据发现任务", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsDiscoveryTaskRespVO> util = new ExcelUtil<>(AssetsDiscoveryTaskRespVO.class);
        List<AssetsDiscoveryTaskRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = assetsDiscoveryTaskService.importDaDiscoveryTask(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据发现任务详细信息")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsDiscoveryTaskRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(assetsDiscoveryTaskService.getDaDiscoveryTaskById(id));
    }

    @Operation(summary = "新增数据发现任务")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:add')")
    @Log(title = "数据发现任务", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsDiscoveryTaskSaveReqVO assetsDiscoveryTask) {
        assetsDiscoveryTask.setCreatorId(getUserId());
        assetsDiscoveryTask.setCreateBy(getNickName());
        assetsDiscoveryTask.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(assetsDiscoveryTaskService.createDaDiscoveryTask(assetsDiscoveryTask));
    }

    @Operation(summary = "修改数据发现任务")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:edit')")
    @Log(title = "数据发现任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsDiscoveryTaskSaveReqVO assetsDiscoveryTask) {
        assetsDiscoveryTask.setUpdatorId(getUserId());
        assetsDiscoveryTask.setUpdateBy(getNickName());
        assetsDiscoveryTask.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(assetsDiscoveryTaskService.updateDaDiscoveryTask(assetsDiscoveryTask));
    }

    @Operation(summary = "删除数据发现任务")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:remove')")
    @Log(title = "数据发现任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(assetsDiscoveryTaskService.removeDaDiscoveryTask(Arrays.asList(ids)));
    }

    @Log(title = "数据发现任务", businessType = BusinessType.UPDATE)
    @PutMapping("/runDaDiscoveryTask/{id}")
    public AjaxResult runDaDiscoveryTask(@PathVariable("id") Long id) {
        boolean result = assetsDiscoveryTaskService.runDaDiscoveryTask(id);
        return result ? success() : error("任务不存在或已过期！");
    }

    @Log(title = "触发一次数据发现任务", businessType = BusinessType.UPDATE)
    @PutMapping("/startDaDiscoveryTask/{id}")
    public AjaxResult startDaDiscoveryTask(@PathVariable("id") Long id) {
        return assetsDiscoveryTaskService.startDaDiscoveryTask(id);
    }

    @Log(title = "数据发现任务状态修改", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:edit')")
    @PostMapping("/updateDaDiscoveryTaskStatus")
    public AjaxResult updateDaDiscoveryTaskStatus(@RequestBody AssetsDiscoveryTaskSaveReqVO assetsDiscoveryTask) {
        boolean result = assetsDiscoveryTaskService.updateDaDiscoveryTaskStatus(assetsDiscoveryTask);
        return result ? success() : error("任务不存在或已过期！");
    }

    @Log(title = "数据发现任务调度周期修改", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('ast:discoveryTask:edit')")
    @PostMapping("/updateDaDiscoveryTaskCronExpression")
    public AjaxResult updateDaDiscoveryTaskCronExpression(@RequestBody AssetsDiscoveryTaskSaveReqVO assetsDiscoveryTask) {
        boolean result = assetsDiscoveryTaskService.updateDaDiscoveryTaskCronExpression(assetsDiscoveryTask);
        return result ? success() : error("任务不存在或已过期！");
    }
}
