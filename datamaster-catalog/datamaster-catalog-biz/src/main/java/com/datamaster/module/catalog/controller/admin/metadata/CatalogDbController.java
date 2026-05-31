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
import com.datamaster.module.catalog.controller.admin.metadata.vo.*;
import com.datamaster.module.catalog.convert.metadata.CatalogDbConvert;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.datamaster.module.catalog.service.metadata.ICatalogDbService;
import com.datamaster.security.annotation.BizDataScope;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据库Controller
 *
 * @author DATAMASTER
 * @date 2026-02-11
 */
@Tag(name = "数据库")
@RestController
@RequestMapping("/mc/db")
@Validated
public class CatalogDbController extends BaseController {
    @Resource
    private ICatalogDbService CatalogDbService;

    @Operation(summary = "查询数据库列表")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogDbRespVO>> list(CatalogDbPageReqVO CatalogDb) {
        PageResult<CatalogDbDO> page = CatalogDbService.getCatalogDbPage(CatalogDb);
        return CommonResult.success(BeanUtils.toBean(page, CatalogDbRespVO.class));
    }

    @Operation(summary = "查询库元数据列表")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @GetMapping("/getCatalogDbList")
    public CommonResult<List<CatalogDbRespVO>> getCatalogDbList(CatalogDbPageReqVO CatalogDb) {
        List<CatalogDbDO> page = CatalogDbService.getCatalogDbList(CatalogDb);
        return CommonResult.success(BeanUtils.toBean(page, CatalogDbRespVO.class));
    }


    @Operation(summary = "导出数据库列表")
    @BizDataScope(code = "Catalog_metadata_list", userField = "businessLeader", deptField = "responsibleDept")
    @Log(title = "数据库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogDbPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogDbDO> list = (List<CatalogDbDO>) CatalogDbService.getCatalogDbPage(exportReqVO).getRows();
        ExcelUtil<CatalogDbRespVO> util = new ExcelUtil<>(CatalogDbRespVO.class);
        util.exportExcel(response, CatalogDbConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取数据库详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogDbRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogDbRespVO respVO = CatalogDbService.getCatalogDbById(id);
        return CommonResult.success(respVO);
    }

    @Operation(summary = "新增数据库")
    @Log(title = "数据库", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogDbSaveReqVO CatalogDb) {
        CatalogDb.setCreatorId(getUserId());
        CatalogDb.setCreateBy(getNickName());
        CatalogDb.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogDbService.createCatalogDb(CatalogDb));
    }

    @Operation(summary = "修改数据库")
    @Log(title = "数据库", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogDbSaveReqVO CatalogDb) {
        CatalogDb.setUpdatorId(getUserId());
        CatalogDb.setUpdateBy(getNickName());
        CatalogDb.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogDbService.updateCatalogDb(CatalogDb));
    }

    @Operation(summary = "停启用库元数据")
    @Log(title = "库元数据", businessType = BusinessType.UPDATE)
    @PostMapping("toggle")
    public CommonResult<Integer> toggle(@Valid @RequestBody ToggleStatusVO param) {
        return CommonResult.toAjax(CatalogDbService.toggle(param.getId(), param.getStatus()));
    }


    @Operation(summary = "停启用库元数据")
    @Log(title = "库元数据", businessType = BusinessType.UPDATE)
    @PostMapping("/editPortalVisible")
    public CommonResult<Integer> editPortalVisible(@Valid @RequestBody CatalogDbSaveReqVO param) {
        return CommonResult.toAjax(CatalogDbService.editPortalVisible(param.getId(), param.getPortalVisible()));
    }




    @Operation(summary = "删除数据库")
    @Log(title = "数据库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogDbService.removeCatalogDb(Arrays.asList(ids)));
    }


    @Operation(summary = "批量删除检查库元数据")
    @GetMapping("/batchDeleteCheck/{ids}")
    public CommonResult<BatchDeleteCheck<Long>> batchDeleteCheck(@PathVariable Long[] ids) {
        BatchDeleteCheck<Long> result = CatalogDbService.batchDeleteCheck(Arrays.asList(ids));
        return CommonResult.success(result);
    }


    @Operation(summary = "查询库元数据列表")
    @GetMapping("/selectMetaSearchPage")
    public CommonResult<PageResult<CatalogMetaSearchRespDTO>> selectMetaSearchPage(CatalogMetaSearchRespDTO mdMetaSearchRespDTO) {
        PageResult<CatalogMetaSearchRespDTO> page = CatalogDbService.selectMetaSearchPage(mdMetaSearchRespDTO);
        return CommonResult.success(page);
    }


}
