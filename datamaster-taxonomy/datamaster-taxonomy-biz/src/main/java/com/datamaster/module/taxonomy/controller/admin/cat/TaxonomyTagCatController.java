

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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTagCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyTagCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTagCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyTagCatService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * 标签类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Tag(name = "标签类目管理")
@RestController
@RequestMapping("/tax/tagCat")
@Validated
public class TaxonomyTagCatController extends BaseController {
    @Resource
    private ITaxonomyTagCatService TaxonomyTagCatService;

    @Operation(summary = "查询标签类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:tagCat:list')")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyTagCatRespVO>> list(TaxonomyTagCatPageReqVO TaxonomyTagCat) {
        List<TaxonomyTagCatDO> page = TaxonomyTagCatService.getAttTagCatLIst(TaxonomyTagCat);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyTagCatRespVO.class));
    }

    @Operation(summary = "导出标签类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:tagCat:export')")
    @Log(title = "标签类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyTagCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyTagCatDO> list = (List<TaxonomyTagCatDO>) TaxonomyTagCatService.getAttTagCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyTagCatRespVO> util = new ExcelUtil<>(TaxonomyTagCatRespVO.class);
        util.exportExcel(response, TaxonomyTagCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入标签类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:tagCat:import')")
    @Log(title = "标签类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyTagCatRespVO> util = new ExcelUtil<>(TaxonomyTagCatRespVO.class);
        List<TaxonomyTagCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyTagCatService.importAttTagCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取标签类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:tagCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyTagCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyTagCatDO TaxonomyTagCatDO = TaxonomyTagCatService.getAttTagCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyTagCatDO, TaxonomyTagCatRespVO.class));
    }

    @Operation(summary = "新增标签类目管理")
    @PreAuthorize("@ss.hasPermi('att:tagCat:add')")
    @Log(title = "标签类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyTagCatSaveReqVO TaxonomyTagCat) {
        TaxonomyTagCat.setCreatorId(getUserId());
        TaxonomyTagCat.setCreateBy(getNickName());
        TaxonomyTagCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTagCatService.createAttTagCat(TaxonomyTagCat));
    }

    @Operation(summary = "修改标签类目管理")
    @PreAuthorize("@ss.hasPermi('att:tagCat:edit')")
    @Log(title = "标签类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyTagCatSaveReqVO TaxonomyTagCat) {
        TaxonomyTagCat.setUpdatorId(getUserId());
        TaxonomyTagCat.setUpdateBy(getNickName());
        TaxonomyTagCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTagCatService.updateAttTagCat(TaxonomyTagCat));
    }

//    @Operation(summary = "删除标签类目管理")
//    @PreAuthorize("@ss.hasPermi('att:tagCat:remove')")
//    @Log(title = "标签类目管理", businessType = BusinessType.DELETE)
//    @DeleteMapping("/{ids}")
//    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
//        return CommonResult.toAjax(TaxonomyTagCatService.removeAttTagCat(Arrays.asList(ids)));
//    }

    //删除
    @Operation(summary = "删除标签类目管理")
    @PreAuthorize("@ss.hasPermi('att:tagCat:remove')")
    @Log(title = "标签类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ID}")
    public CommonResult<Integer> remove(@PathVariable Long ID) {
        return CommonResult.toAjax(TaxonomyTagCatService.removeAttTagCat(ID));
    }

}
