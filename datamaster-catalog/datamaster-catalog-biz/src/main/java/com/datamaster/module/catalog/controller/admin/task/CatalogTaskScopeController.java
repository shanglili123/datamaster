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
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.module.catalog.convert.task.CatalogTaskScopeConvert;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;
import com.datamaster.module.catalog.service.task.ICatalogTaskScopeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 采集范围Controller
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Tag(name = "采集范围")
@RestController
@RequestMapping("/cat/taskScope")
@Validated
public class CatalogTaskScopeController extends BaseController {
    @Resource
    private ICatalogTaskScopeService CatalogTaskScopeService;

    @Operation(summary = "查询采集范围列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTaskScopeRespVO>> list(CatalogTaskScopePageReqVO CatalogTaskScope) {
        PageResult<CatalogTaskScopeDO> page = CatalogTaskScopeService.getCatalogTaskScopePage(CatalogTaskScope);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTaskScopeRespVO.class));
    }

    @Operation(summary = "导出采集范围列表")
    @Log(title = "采集范围", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTaskScopePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTaskScopeDO> list = (List<CatalogTaskScopeDO>) CatalogTaskScopeService.getCatalogTaskScopePage(exportReqVO).getRows();
        ExcelUtil<CatalogTaskScopeRespVO> util = new ExcelUtil<>(CatalogTaskScopeRespVO.class);
        util.exportExcel(response, CatalogTaskScopeConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入采集范围列表")
    @Log(title = "采集范围", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CatalogTaskScopeRespVO> util = new ExcelUtil<>(CatalogTaskScopeRespVO.class);
        List<CatalogTaskScopeRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CatalogTaskScopeService.importCatalogTaskScope(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取采集范围详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTaskScopeRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTaskScopeDO CatalogTaskScopeDO = CatalogTaskScopeService.getCatalogTaskScopeById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogTaskScopeDO, CatalogTaskScopeRespVO.class));
    }

    @Operation(summary = "新增采集范围")
    @Log(title = "采集范围", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTaskScopeSaveReqVO CatalogTaskScope) {
        CatalogTaskScope.setCreatorId(getUserId());
        CatalogTaskScope.setCreateBy(getNickName());
        CatalogTaskScope.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskScopeService.createCatalogTaskScope(CatalogTaskScope));
    }

    @Operation(summary = "修改采集范围")
    @Log(title = "采集范围", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTaskScopeSaveReqVO CatalogTaskScope) {
        CatalogTaskScope.setUpdatorId(getUserId());
        CatalogTaskScope.setUpdateBy(getNickName());
        CatalogTaskScope.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskScopeService.updateCatalogTaskScope(CatalogTaskScope));
    }

    @Operation(summary = "删除采集范围")
    @Log(title = "采集范围", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTaskScopeService.removeCatalogTaskScope(Arrays.asList(ids)));
    }

}
