package com.datamaster.module.assets.controller.admin.assetColumn;

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
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRespVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.convert.assetColumn.AssetsAssetColumnConvert;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.service.assetColumn.IAssetsAssetColumnService;

/**
 * 数据资产字段Controller * * @author lhs * @date 2025-01-21
 */
@Tag(name = "数据资产字段")
@RestController
@RequestMapping("/ast/assetColumn")
@Validated
public class AssetsAssetColumnController extends BaseController {
    @Resource
    private IAssetsAssetColumnService AssetsAssetColumnService;

    @Operation(summary = "查询数据资产字段列表")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetColumnRespVO>> list(AssetsAssetColumnPageReqVO AssetsAssetColumn) {
        PageResult<AssetsAssetColumnDO> page = AssetsAssetColumnService.getAssetColumnPage(AssetsAssetColumn);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetColumnRespVO.class));
    }

    @Operation(summary = "根据选择的资产表id信息查询字段信息")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:list')")
    @GetMapping("/getColumnByAssetId")
    public AjaxResult getColumnByAssetId(AssetsAssetColumnPageReqVO AssetsAssetColumn) {
        return AssetsAssetColumnService.getColumnByAssetId(AssetsAssetColumn);
    }

    @Operation(summary = "导出数据资产字段列表")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:export')")
    @Log(title = "数据资产字段", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetColumnPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetColumnDO> list = (List<AssetsAssetColumnDO>) AssetsAssetColumnService.getAssetColumnPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetColumnRespVO> util = new ExcelUtil<>(AssetsAssetColumnRespVO.class);
        util.exportExcel(response, AssetsAssetColumnConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产字段列表")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:import')")
    @Log(title = "数据资产字段", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetColumnRespVO> util = new ExcelUtil<>(AssetsAssetColumnRespVO.class);
        List<AssetsAssetColumnRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetColumnService.importAssetColumn(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产字段详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetColumnRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetColumnDO AssetsAssetColumnDO = AssetsAssetColumnService.getAssetColumnById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetColumnDO, AssetsAssetColumnRespVO.class));
    }

    @Operation(summary = "新增数据资产字段")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:add')")
    @Log(title = "数据资产字段", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetColumnSaveReqVO AssetsAssetColumn) {
        AssetsAssetColumn.setCreatorId(getUserId());
        AssetsAssetColumn.setCreateBy(getNickName());
        AssetsAssetColumn.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetColumnService.createAssetColumn(AssetsAssetColumn));
    }

    @Operation(summary = "修改数据资产字段")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:edit')")
    @Log(title = "数据资产字段", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetColumnSaveReqVO AssetsAssetColumn) {
        AssetsAssetColumn.setUpdatorId(getUserId());
        AssetsAssetColumn.setUpdateBy(getNickName());
        AssetsAssetColumn.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetColumnService.updateAssetColumn(AssetsAssetColumn));
    }

    @Operation(summary = "删除数据资产字段")
    @PreAuthorize("@ss.hasPermi('da:assetColumn:remove')")
    @Log(title = "数据资产字段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetColumnService.removeAssetColumn(Arrays.asList(ids)));
    }
}