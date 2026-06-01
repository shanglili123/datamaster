

package com.datamaster.module.taxonomy.controller.admin.cat;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyAssetCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyAssetCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyAssetCatService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据资产类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "数据资产类目管理")
@RestController
@RequestMapping("/tax/assetCat")
@Validated
public class TaxonomyAssetCatController extends BaseController {
    @Resource
    private ITaxonomyAssetCatService TaxonomyAssetCatService;

    @Operation(summary = "查询数据资产类目管理列表")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyAssetCatRespVO>> list(TaxonomyAssetCatPageReqVO TaxonomyAssetCat) {
        List<TaxonomyAssetCatDO> TaxonomyAssetCatList = TaxonomyAssetCatService.getAttAssetCatList(TaxonomyAssetCat);
        return CommonResult.success(BeanUtils.toBean(TaxonomyAssetCatList, TaxonomyAssetCatRespVO.class));
    }

    @Operation(summary = "导出数据资产类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:assetCat:export')")
    @Log(title = "数据资产类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyAssetCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyAssetCatDO> list = (List<TaxonomyAssetCatDO>) TaxonomyAssetCatService.getAttAssetCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyAssetCatRespVO> util = new ExcelUtil<>(TaxonomyAssetCatRespVO.class);
        util.exportExcel(response, TaxonomyAssetCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:assetCat:import')")
    @Log(title = "数据资产类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyAssetCatRespVO> util = new ExcelUtil<>(TaxonomyAssetCatRespVO.class);
        List<TaxonomyAssetCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyAssetCatService.importAttAssetCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:assetCat:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyAssetCatRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyAssetCatDO TaxonomyAssetCatDO = TaxonomyAssetCatService.getAttAssetCatById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyAssetCatDO, TaxonomyAssetCatRespVO.class));
    }

    @Operation(summary = "新增数据资产类目管理")
    @PreAuthorize("@ss.hasPermi('att:assetCat:add')")
    @Log(title = "数据资产类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyAssetCatSaveReqVO TaxonomyAssetCat) {
        TaxonomyAssetCat.setCreatorId(getUserId());
        TaxonomyAssetCat.setCreateBy(getNickName());
        TaxonomyAssetCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyAssetCatService.createAttAssetCat(TaxonomyAssetCat));
    }

    @Operation(summary = "修改数据资产类目管理")
    @PreAuthorize("@ss.hasPermi('att:assetCat:edit')")
    @Log(title = "数据资产类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyAssetCatSaveReqVO TaxonomyAssetCat) {
        TaxonomyAssetCat.setUpdatorId(getUserId());
        TaxonomyAssetCat.setUpdateBy(getNickName());
        TaxonomyAssetCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyAssetCatService.updateAttAssetCat(TaxonomyAssetCat));
    }

    @Operation(summary = "删除数据资产类目管理")
    @PreAuthorize("@ss.hasPermi('att:assetCat:remove')")
    @Log(title = "数据资产类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyAssetCatService.removeAttAssetCat(Arrays.asList(ids)));
    }


}
