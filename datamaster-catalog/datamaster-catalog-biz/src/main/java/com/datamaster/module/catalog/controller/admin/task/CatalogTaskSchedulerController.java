package com.datamaster.module.catalog.controller.admin.task;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerSaveReqVO;
import com.datamaster.module.catalog.convert.task.CatalogTaskSchedulerConvert;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskSchedulerDO;
import com.datamaster.module.catalog.service.task.ICatalogTaskSchedulerService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据集成调度信息Controller
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Tag(name = "数据集成调度信息")
@RestController
@RequestMapping("/mc/taskScheduler")
@Validated
public class CatalogTaskSchedulerController extends BaseController {
    @Resource
    private ICatalogTaskSchedulerService CatalogTaskSchedulerService;

    @Operation(summary = "查询数据集成调度信息列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTaskSchedulerRespVO>> list(CatalogTaskSchedulerPageReqVO CatalogTaskScheduler) {
        PageResult<CatalogTaskSchedulerDO> page = CatalogTaskSchedulerService.getCatalogTaskSchedulerPage(CatalogTaskScheduler);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTaskSchedulerRespVO.class));
    }

    @Operation(summary = "导出数据集成调度信息列表")
    @Log(title = "数据集成调度信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTaskSchedulerPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTaskSchedulerDO> list = (List<CatalogTaskSchedulerDO>) CatalogTaskSchedulerService.getCatalogTaskSchedulerPage(exportReqVO).getRows();
        ExcelUtil<CatalogTaskSchedulerRespVO> util = new ExcelUtil<>(CatalogTaskSchedulerRespVO.class);
        util.exportExcel(response, CatalogTaskSchedulerConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成调度信息列表")
    @Log(title = "数据集成调度信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CatalogTaskSchedulerRespVO> util = new ExcelUtil<>(CatalogTaskSchedulerRespVO.class);
        List<CatalogTaskSchedulerRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CatalogTaskSchedulerService.importCatalogTaskScheduler(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成调度信息详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTaskSchedulerRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTaskSchedulerDO CatalogTaskSchedulerDO = CatalogTaskSchedulerService.getCatalogTaskSchedulerById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogTaskSchedulerDO, CatalogTaskSchedulerRespVO.class));
    }

    @Operation(summary = "新增数据集成调度信息")
    @Log(title = "数据集成调度信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTaskSchedulerSaveReqVO CatalogTaskScheduler) {
        CatalogTaskScheduler.setCreatorId(getUserId());
        CatalogTaskScheduler.setCreateBy(getNickName());
        CatalogTaskScheduler.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskSchedulerService.createCatalogTaskScheduler(CatalogTaskScheduler));
    }

    @Operation(summary = "修改数据集成调度信息")
    @Log(title = "数据集成调度信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTaskSchedulerSaveReqVO CatalogTaskScheduler) {
        CatalogTaskScheduler.setUpdatorId(getUserId());
        CatalogTaskScheduler.setUpdateBy(getNickName());
        CatalogTaskScheduler.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskSchedulerService.updateCatalogTaskScheduler(CatalogTaskScheduler));
    }

    @Operation(summary = "删除数据集成调度信息")
    @Log(title = "数据集成调度信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTaskSchedulerService.removeCatalogTaskScheduler(Arrays.asList(ids)));
    }

}
