package com.datamaster.module.assets.controller.admin.datasource;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelSaveReqVO;
import com.datamaster.module.assets.convert.datasource.AssetsDatasourceSpaceRelConvert;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceSpaceRelService;

/**
 * 数据源与空间关联关系Controller * * @author DATAMASTER * @date 2025-03-13
 */
@Tag(name = "数据源与空间关联关系")
@RestController
@RequestMapping("/ast/dataSourceSpaceRel")
@Validated
public class AssetsDatasourceSpaceRelController extends BaseController {
    @Resource
    private IAssetsDatasourceSpaceRelService AssetsDatasourceSpaceRelService;

    @Operation(summary = "查询数据源与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsDatasourceSpaceRelRespVO>> list(AssetsDatasourceSpaceRelPageReqVO AssetsDatasourceSpaceRel) {
        PageResult<AssetsDatasourceSpaceRelDO> page = AssetsDatasourceSpaceRelService.getDatasourceSpaceRelPage(AssetsDatasourceSpaceRel);
        return CommonResult.success(BeanUtils.toBean(page, AssetsDatasourceSpaceRelRespVO.class));
    }

    @Operation(summary = "导出数据源与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:export')")
    @Log(title = "数据源与空间关联关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsDatasourceSpaceRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsDatasourceSpaceRelDO> list = (List<AssetsDatasourceSpaceRelDO>) AssetsDatasourceSpaceRelService.getDatasourceSpaceRelPage(exportReqVO).getRows();
        ExcelUtil<AssetsDatasourceSpaceRelRespVO> util = new ExcelUtil<>(AssetsDatasourceSpaceRelRespVO.class);
        util.exportExcel(response, AssetsDatasourceSpaceRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据源与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:import')")
    @Log(title = "数据源与空间关联关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsDatasourceSpaceRelRespVO> util = new ExcelUtil<>(AssetsDatasourceSpaceRelRespVO.class);
        List<AssetsDatasourceSpaceRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsDatasourceSpaceRelService.importDatasourceSpaceRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据源与空间关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsDatasourceSpaceRelRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsDatasourceSpaceRelDO AssetsDatasourceSpaceRelDO = AssetsDatasourceSpaceRelService.getDatasourceSpaceRelById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsDatasourceSpaceRelDO, AssetsDatasourceSpaceRelRespVO.class));
    }

    @Operation(summary = "新增数据源与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:add')")
    @Log(title = "数据源与空间关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsDatasourceSpaceRelSaveReqVO AssetsDatasourceSpaceRel) {
        AssetsDatasourceSpaceRel.setCreatorId(getUserId());
        AssetsDatasourceSpaceRel.setCreateBy(getNickName());
        AssetsDatasourceSpaceRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsDatasourceSpaceRelService.createDatasourceSpaceRel(AssetsDatasourceSpaceRel));
    }

    @Operation(summary = "修改数据源与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:edit')")
    @Log(title = "数据源与空间关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsDatasourceSpaceRelSaveReqVO AssetsDatasourceSpaceRel) {
        AssetsDatasourceSpaceRel.setUpdatorId(getUserId());
        AssetsDatasourceSpaceRel.setUpdateBy(getNickName());
        AssetsDatasourceSpaceRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsDatasourceSpaceRelService.updateDatasourceSpaceRel(AssetsDatasourceSpaceRel));
    }

    @Operation(summary = "删除数据源与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:dataSource:remove')")
    @Log(title = "数据源与空间关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsDatasourceSpaceRelService.removeDatasourceSpaceRel(Arrays.asList(ids)));
    }
}
