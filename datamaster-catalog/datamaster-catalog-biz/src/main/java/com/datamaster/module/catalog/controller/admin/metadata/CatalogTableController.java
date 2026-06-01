package com.datamaster.module.catalog.controller.admin.metadata;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTablePageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogTableSaveReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.ToggleStatusVO;
import com.datamaster.module.catalog.convert.metadata.CatalogTableConvert;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.module.catalog.service.metadata.ICatalogTableService;
import com.datamaster.security.annotation.BizDataScope;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 元数据信息Controller
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Tag(name = "元数据信息")
@RestController
@RequestMapping("/cat/table")
@Validated
public class CatalogTableController extends BaseController {
    @Resource
    private ICatalogTableService CatalogTableService;

    @Operation(summary = "查询元数据信息列表")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:list')")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTableRespVO>> list(CatalogTablePageReqVO CatalogTable) {
        PageResult<CatalogTableDO> page = CatalogTableService.getCatalogTablePage(CatalogTable);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTableRespVO.class));
    }

    @Operation(summary = "查询元数据信息列表")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:list')")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/getCatalogTablePageAsset")
    public CommonResult<PageResult<CatalogTableRespVO>> getCatalogTablePageAsset(CatalogTablePageReqVO CatalogTable) {
        return CommonResult.success(CatalogTableService.getCatalogTablePageAsset(CatalogTable));
    }

    @Operation(summary = "查询元数据信息列表")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:list')")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/getCatalogTableListAsset")
    public CommonResult<List<CatalogTableRespVO>> getCatalogTableListAsset(CatalogTablePageReqVO CatalogTable) {
        return CommonResult.success(CatalogTableService.getCatalogTableListAsset(CatalogTable));
    }

    @Operation(summary = "导出元数据信息列表")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:export')")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @Log(title = "元数据信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTablePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTableDO> list = (List<CatalogTableDO>) CatalogTableService.getCatalogTablePage(exportReqVO).getRows();
        ExcelUtil<CatalogTableRespVO> util = new ExcelUtil<>(CatalogTableRespVO.class);
        util.exportExcel(response, CatalogTableConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取元数据信息详细信息")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTableRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTableRespVO respVO = CatalogTableService.getCatalogTableById(id);
        return CommonResult.success(respVO);
    }

    @Operation(summary = "新增元数据信息")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:add')")
    @Log(title = "元数据信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTableSaveReqVO CatalogTable) {
        CatalogTable.setCreatorId(getUserId());
        CatalogTable.setCreateBy(getNickName());
        CatalogTable.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTableService.createCatalogTable(CatalogTable));
    }

    @Operation(summary = "修改元数据信息")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:edit')")
    @Log(title = "元数据信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTableSaveReqVO CatalogTable) {
        CatalogTable.setUpdatorId(getUserId());
        CatalogTable.setUpdateBy(getNickName());
        CatalogTable.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTableService.updateCatalogTable(CatalogTable));
    }

    @Operation(summary = "暂存表元数据")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:add')")
    @Log(title = "暂存表元数据", businessType = BusinessType.INSERT)
    @PostMapping("draft")
    public CommonResult<Long> draft(@RequestBody CatalogTableSaveReqVO saveReqVO) {
        return CommonResult.toAjax(CatalogTableService.saveDraft(saveReqVO));
    }



    @Operation(summary = "删除元数据信息")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:remove')")
    @Log(title = "元数据信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTableService.removeCatalogTable(Arrays.asList(ids)));
    }

    @Operation(summary = "批量删除检查表元数据")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:remove')")
    @GetMapping("/batchDeleteCheck/{ids}")
    public CommonResult<BatchDeleteCheck<Long>> batchDeleteCheck(@PathVariable Long[] ids) {
        BatchDeleteCheck<Long> result = CatalogTableService.batchDeleteCheck(Arrays.asList(ids));
        return CommonResult.success(result);
    }

    @Operation(summary = "停启用表元数据")
    @PreAuthorize("@ss.hasPermi('mc:metadata:table:edit')")
    @Log(title = "停启用表元数据", businessType = BusinessType.UPDATE)
    @PostMapping("/toggle")
    public CommonResult<Integer> toggle(@Valid @RequestBody ToggleStatusVO param) {
        return CommonResult.toAjax(CatalogTableService.toggle(param.getId(), param.getStatus()));
    }
}
