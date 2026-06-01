

package com.datamaster.module.taxonomy.controller.admin.cat;

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
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyDataDevCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataDevCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyDataDevCatService;

/**
 * 数据开发类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Tag(name = "数据开发类目管理")
@RestController
@RequestMapping("/tax/dataDevCat")
@Validated
public class TaxonomyDataDevCatController extends BaseController {
    @Resource
    private ITaxonomyDataDevCatService TaxonomyDataDevCatService;

    @Operation(summary = "查询数据开发类目管理列表")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyDataDevCatRespVO>> list(TaxonomyDataDevCatPageReqVO TaxonomyDataDevCat) {
        List<TaxonomyDataDevCatDO> TaxonomyDataDevCatDOList = TaxonomyDataDevCatService.getAttDataDevCatList(TaxonomyDataDevCat);
        return CommonResult.success(BeanUtils.toBean(TaxonomyDataDevCatDOList, TaxonomyDataDevCatRespVO.class));
    }

    @Operation(summary = "导出数据开发类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:dataDevCat:export')")
    @Log(title = "数据开发类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyDataDevCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyDataDevCatDO> list = (List<TaxonomyDataDevCatDO>) TaxonomyDataDevCatService.getAttDataDevCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyDataDevCatRespVO> util = new ExcelUtil<>(TaxonomyDataDevCatRespVO.class);
        util.exportExcel(response, TaxonomyDataDevCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据开发类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:dataDevCat:import')")
    @Log(title = "数据开发类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyDataDevCatRespVO> util = new ExcelUtil<>(TaxonomyDataDevCatRespVO.class);
        List<TaxonomyDataDevCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyDataDevCatService.importAttDataDevCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据开发类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:dataDevCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyDataDevCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyDataDevCatDO TaxonomyDataDevCatDO = TaxonomyDataDevCatService.getAttDataDevCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyDataDevCatDO, TaxonomyDataDevCatRespVO.class));
    }

    @Operation(summary = "新增数据开发类目管理")
    @PreAuthorize("@ss.hasPermi('att:dataDevCat:add')")
    @Log(title = "数据开发类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyDataDevCatSaveReqVO TaxonomyDataDevCat) {
        TaxonomyDataDevCat.setCreatorId(getUserId());
        TaxonomyDataDevCat.setCreateBy(getNickName());
        TaxonomyDataDevCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyDataDevCatService.createAttDataDevCat(TaxonomyDataDevCat));
    }

    @Operation(summary = "修改数据开发类目管理")
    @PreAuthorize("@ss.hasPermi('att:dataDevCat:edit')")
    @Log(title = "数据开发类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyDataDevCatSaveReqVO TaxonomyDataDevCat) {
        TaxonomyDataDevCat.setUpdatorId(getUserId());
        TaxonomyDataDevCat.setUpdateBy(getNickName());
        TaxonomyDataDevCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyDataDevCatService.updateAttDataDevCat(TaxonomyDataDevCat));
    }

    @Operation(summary = "删除数据开发类目管理")
    @PreAuthorize("@ss.hasPermi('att:dataDevCat:remove')")
    @Log(title = "数据开发类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyDataDevCatService.removeAttDataDevCat(Arrays.asList(ids)));
    }

}
