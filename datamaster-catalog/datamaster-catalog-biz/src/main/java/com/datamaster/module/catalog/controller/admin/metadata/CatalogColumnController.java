package com.datamaster.module.catalog.controller.admin.metadata;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnPageReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnRespVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.CatalogColumnSaveReqVO;
import com.datamaster.module.catalog.controller.admin.metadata.vo.ToggleStatusVO;
import com.datamaster.module.catalog.convert.metadata.CatalogColumnConvert;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.catalog.service.metadata.ICatalogColumnService;
import com.datamaster.security.annotation.BizDataScope;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;

/**
 * 元数据字段信息Controller
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Tag(name = "元数据字段信息")
@RestController
@RequestMapping("/mc/column")
@Validated
public class CatalogColumnController extends BaseController {
    @Resource
    private ICatalogColumnService CatalogColumnService;

    @Operation(summary = "查询元数据字段信息列表")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogColumnRespVO>> list(CatalogColumnPageReqVO CatalogColumn) {
        PageResult<CatalogColumnDO> page = CatalogColumnService.getCatalogColumnPage(CatalogColumn);
        return CommonResult.success(BeanUtils.toBean(page, CatalogColumnRespVO.class));
    }

    @Operation(summary = "查询字段元数据列表")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/getMdColumnList")
    public CommonResult<List<CatalogColumnRespVO>> getMdColumnList(CatalogColumnPageReqVO mdColumn) {
        List<CatalogColumnDO> page = CatalogColumnService.getMdColumnList(mdColumn);
        return CommonResult.success(BeanUtils.toBean(page, CatalogColumnRespVO.class));
    }

    @Operation(summary = "导出元数据字段信息列表")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @Log(title = "元数据字段信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogColumnPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogColumnDO> list = (List<CatalogColumnDO>) CatalogColumnService.getCatalogColumnPage(exportReqVO).getRows();
        ExcelUtil<CatalogColumnRespVO> util = new ExcelUtil<>(CatalogColumnRespVO.class);
        util.exportExcel(response, CatalogColumnConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取元数据字段信息详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogColumnRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogColumnRespVO respVO = CatalogColumnService.getCatalogColumnById(id);
        return CommonResult.success(respVO);
    }

    @Operation(summary = "新增元数据字段信息")
    @Log(title = "元数据字段信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Integer> add(@Valid @RequestBody @NotEmpty List<@Valid CatalogColumnSaveReqVO> mdColumn) {
        for (CatalogColumnSaveReqVO saveReqVO : mdColumn) {
            saveReqVO.validate();
            saveReqVO.setCreatorId(getUserId());
            saveReqVO.setCreateBy(getNickName());
            saveReqVO.setCreateTime(DateUtil.date());
        }
        return CommonResult.toAjax(CatalogColumnService.createMdColumn(mdColumn));
    }

    @Operation(summary = "暂存字段元数据信息")
    @Log(title = "暂存表元数据信息", businessType = BusinessType.INSERT)
    @PostMapping("draft")
    public CommonResult<Integer> draft(@RequestBody @NotEmpty List<CatalogColumnSaveReqVO> saveReqVO) {
        return CommonResult.toAjax(CatalogColumnService.saveDraft(saveReqVO));
    }


    @Operation(summary = "修改元数据字段信息")
    @Log(title = "元数据字段信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogColumnSaveReqVO CatalogColumn) {
        CatalogColumn.setUpdatorId(getUserId());
        CatalogColumn.setUpdateBy(getNickName());
        CatalogColumn.setUpdateTime(DateUtil.date());
        CatalogColumn.validate();
        return CommonResult.toAjax(CatalogColumnService.updateCatalogColumn(CatalogColumn));
    }

    @Operation(summary = "停启用字段元数据")
    @Log(title = "字段元数据", businessType = BusinessType.UPDATE)
    @PostMapping("toggle")
    public CommonResult<Integer> toggle(@Valid @RequestBody ToggleStatusVO param) {
        return CommonResult.toAjax(CatalogColumnService.toggle(param.getId(), param.getStatus()));
    }


    @Operation(summary = "删除元数据字段信息")
    @Log(title = "元数据字段信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogColumnService.removeCatalogColumn(Arrays.asList(ids)));
    }

    @Operation(summary = "批量删除检查字段元数据")
    @GetMapping("/batchDeleteCheck/{ids}")
    public CommonResult<BatchDeleteCheck<Long>> batchDeleteCheck(@PathVariable Long[] ids) {
        BatchDeleteCheck<Long> result = CatalogColumnService.batchDeleteCheck(Arrays.asList(ids));
        return CommonResult.success(result);
    }

}
