

package com.datamaster.module.taxonomy.controller.admin.cat;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyDocumentCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDocumentCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyDocumentCatService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * 标准信息分类管理Controller
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Tag(name = "标准信息分类管理")
@RestController
@RequestMapping("/att/documentCat")
@Validated
public class TaxonomyDocumentCatController extends BaseController {
    @Resource
    private ITaxonomyDocumentCatService TaxonomyDocumentCatService;

    @Operation(summary = "查询标准信息分类管理列表")
    @PreAuthorize("@ss.hasPermi('att:documentCat:list')")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyDocumentCatRespVO>> list(TaxonomyDocumentCatPageReqVO TaxonomyDocumentCat) {
        TaxonomyDocumentCat.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyDocumentCatDO> list = (List<TaxonomyDocumentCatDO>) TaxonomyDocumentCatService.getAttDocumentCatPage(TaxonomyDocumentCat).getRows();
        return CommonResult.success(BeanUtils.toBean(list, TaxonomyDocumentCatRespVO.class));
    }

    @Operation(summary = "查询标准信息分类管理列表")
    @GetMapping("/getAttDocumentCatList")
    public CommonResult<List<TaxonomyDocumentCatRespVO>> getAttDocumentCatList(TaxonomyDocumentCatPageReqVO TaxonomyDocumentCat) {
        TaxonomyDocumentCat.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyDocumentCatDO> list = TaxonomyDocumentCatService.getAttDocumentCatList(TaxonomyDocumentCat);
        return CommonResult.success(BeanUtils.toBean(list, TaxonomyDocumentCatRespVO.class));
    }

    @Operation(summary = "导出标准信息分类管理列表")
    @PreAuthorize("@ss.hasPermi('att:documentCat:export')")
    @Log(title = "标准信息分类管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyDocumentCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyDocumentCatDO> list = (List<TaxonomyDocumentCatDO>) TaxonomyDocumentCatService.getAttDocumentCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyDocumentCatRespVO> util = new ExcelUtil<>(TaxonomyDocumentCatRespVO.class);
        util.exportExcel(response, TaxonomyDocumentCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }


    @Operation(summary = "获取标准信息分类管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:documentCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyDocumentCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyDocumentCatDO TaxonomyDocumentCatDO = TaxonomyDocumentCatService.getAttDocumentCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyDocumentCatDO, TaxonomyDocumentCatRespVO.class));
    }

    @Operation(summary = "新增标准信息分类管理")
    @PreAuthorize("@ss.hasPermi('att:documentCat:add')")
    @Log(title = "标准信息分类管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyDocumentCatSaveReqVO TaxonomyDocumentCat) {
        TaxonomyDocumentCat.setCreatorId(getUserId());
        TaxonomyDocumentCat.setCreateBy(getNickName());
        TaxonomyDocumentCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyDocumentCatService.createAttDocumentCat(TaxonomyDocumentCat));
    }

    @Operation(summary = "修改标准信息分类管理")
    @PreAuthorize("@ss.hasPermi('att:documentCat:edit')")
    @Log(title = "标准信息分类管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyDocumentCatSaveReqVO TaxonomyDocumentCat) {
        TaxonomyDocumentCat.setUpdatorId(getUserId());
        TaxonomyDocumentCat.setUpdateBy(getNickName());
        TaxonomyDocumentCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyDocumentCatService.updateAttDocumentCat(TaxonomyDocumentCat));
    }

    @Operation(summary = "删除标准信息分类管理")
    @PreAuthorize("@ss.hasPermi('att:documentCat:remove')")
    @Log(title = "标准信息分类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public CommonResult<Integer> remove(@PathVariable Long id) {
        if (TaxonomyDocumentCatService.hasChildByAttDocumentCatId(id)) {
            return CommonResult.error(GlobalErrorCodeConstants.ERROR.getCode(),"存在子标准信息分类管理，无法删除。");
        }
        return CommonResult.toAjax(TaxonomyDocumentCatService.removeAttDocumentCat(id));
    }

}
