

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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyDataElemCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataElemCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyDataElemCatService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据元类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "数据元类目管理")
@RestController
@RequestMapping("/tax/dataElemCat")
@Validated
public class TaxonomyDataElemCatController extends BaseController {
    @Resource
    private ITaxonomyDataElemCatService TaxonomyDataElemCatService;

    @Operation(summary = "查询数据元类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:list')")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyDataElemCatRespVO>> list(TaxonomyDataElemCatPageReqVO TaxonomyDataElemCat) {
        List<TaxonomyDataElemCatDO> TaxonomyDataElemCatList = TaxonomyDataElemCatService.getAttDataElemCatList(TaxonomyDataElemCat);
        return CommonResult.success(BeanUtils.toBean(TaxonomyDataElemCatList, TaxonomyDataElemCatRespVO.class));
    }

    @Operation(summary = "导出数据元类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:export')")
    @Log(title = "数据元类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyDataElemCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyDataElemCatDO> list = (List<TaxonomyDataElemCatDO>) TaxonomyDataElemCatService.getAttDataElemCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyDataElemCatRespVO> util = new ExcelUtil<>(TaxonomyDataElemCatRespVO.class);
        util.exportExcel(response, TaxonomyDataElemCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据元类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:import')")
    @Log(title = "数据元类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyDataElemCatRespVO> util = new ExcelUtil<>(TaxonomyDataElemCatRespVO.class);
        List<TaxonomyDataElemCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyDataElemCatService.importAttDataElemCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据元类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyDataElemCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyDataElemCatDO TaxonomyDataElemCatDO = TaxonomyDataElemCatService.getAttDataElemCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyDataElemCatDO, TaxonomyDataElemCatRespVO.class));
    }

    @Operation(summary = "新增数据元类目管理")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:add')")
    @Log(title = "数据元类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyDataElemCatSaveReqVO TaxonomyDataElemCat) {
        TaxonomyDataElemCat.setCreatorId(getUserId());
        TaxonomyDataElemCat.setCreateBy(getNickName());
        TaxonomyDataElemCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyDataElemCatService.createAttDataElemCat(TaxonomyDataElemCat));
    }

    @Operation(summary = "修改数据元类目管理")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:edit')")
    @Log(title = "数据元类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyDataElemCatSaveReqVO TaxonomyDataElemCat) {
        TaxonomyDataElemCat.setUpdatorId(getUserId());
        TaxonomyDataElemCat.setUpdateBy(getNickName());
        TaxonomyDataElemCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyDataElemCatService.updateAttDataElemCat(TaxonomyDataElemCat));
    }

    @Operation(summary = "删除数据元类目管理")
    @PreAuthorize("@ss.hasPermi('att:dataElemCat:remove')")
    @Log(title = "数据元类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyDataElemCatService.removeAttDataElemCat(Arrays.asList(ids)));
    }

}
