package com.datamaster.module.assets.controller.admin.assetchild.spaceRel;

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
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.spaceRel.AssetsAssetSpaceRelConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;
import com.datamaster.module.assets.service.assetchild.spaceRel.IAssetsAssetSpaceRelService;

/**
 * 数据资产与空间关联关系Controller * * @author DATAMASTER * @date 2025-04-18
 */
@Tag(name = "数据资产与空间关联关系")
@RestController
@RequestMapping("/ast/assetSpaceRel")
@Validated
public class AssetsAssetSpaceRelController extends BaseController {
    @Resource
    private IAssetsAssetSpaceRelService AssetsAssetSpaceRelService;

    @Operation(summary = "查询数据资产与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetSpaceRelRespVO>> list(AssetsAssetSpaceRelPageReqVO AssetsAssetSpaceRel) {
        PageResult<AssetsAssetSpaceRelDO> page = AssetsAssetSpaceRelService.getAssetSpaceRelPage(AssetsAssetSpaceRel);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetSpaceRelRespVO.class));
    }

    @Operation(summary = "导出数据资产与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:export')")
    @Log(title = "数据资产与空间关联关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetSpaceRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetSpaceRelDO> list = (List<AssetsAssetSpaceRelDO>) AssetsAssetSpaceRelService.getAssetSpaceRelPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetSpaceRelRespVO> util = new ExcelUtil<>(AssetsAssetSpaceRelRespVO.class);
        util.exportExcel(response, AssetsAssetSpaceRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:import')")
    @Log(title = "数据资产与空间关联关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetSpaceRelRespVO> util = new ExcelUtil<>(AssetsAssetSpaceRelRespVO.class);
        List<AssetsAssetSpaceRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetSpaceRelService.importAssetSpaceRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产与空间关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetSpaceRelRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetSpaceRelDO AssetsAssetSpaceRelDO = AssetsAssetSpaceRelService.getAssetSpaceRelById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetSpaceRelDO, AssetsAssetSpaceRelRespVO.class));
    }

    @Operation(summary = "新增数据资产与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:add')")
    @Log(title = "数据资产与空间关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetSpaceRelSaveReqVO AssetsAssetSpaceRel) {
        AssetsAssetSpaceRel.setCreatorId(getUserId());
        AssetsAssetSpaceRel.setCreateBy(getNickName());
        AssetsAssetSpaceRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetSpaceRelService.createAssetSpaceRel(AssetsAssetSpaceRel));
    }

    @Operation(summary = "修改数据资产与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:edit')")
    @Log(title = "数据资产与空间关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetSpaceRelSaveReqVO AssetsAssetSpaceRel) {
        AssetsAssetSpaceRel.setUpdatorId(getUserId());
        AssetsAssetSpaceRel.setUpdateBy(getNickName());
        AssetsAssetSpaceRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetSpaceRelService.updateAssetSpaceRel(AssetsAssetSpaceRel));
    }

    @Operation(summary = "删除数据资产与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:assetSpaceRel:remove')")
    @Log(title = "数据资产与空间关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetSpaceRelService.removeAssetSpaceRel(Arrays.asList(ids)));
    }
}
