package com.datamaster.module.governance.controller.admin.cat;

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
import com.datamaster.common.category.convert.CategoryConvert;
import com.datamaster.common.category.dal.dataobject.TaxonomyCategoryDO;
import com.datamaster.common.category.service.ITaxonomyCategoryService;
import com.datamaster.common.category.vo.CategoryPageReqVO;
import com.datamaster.common.category.vo.CategoryRespVO;
import com.datamaster.common.category.vo.CategorySaveReqVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@Tag(name = "类目管理")
@RestController
@RequestMapping("/tax/category")
@Validated
public class TaxonomyCategoryController extends BaseController {
    @Resource
    private ITaxonomyCategoryService taxonomyCategoryService;

    @Operation(summary = "查询类目管理列表")
    @PreAuthorize("@ss.hasPermi('tax:category:list')")
    @GetMapping("/list/{catType}")
    public CommonResult<List<CategoryRespVO>> list(@PathVariable String catType, CategoryPageReqVO reqVO) {
        reqVO.setCatType(catType);
        List<TaxonomyCategoryDO> list = taxonomyCategoryService.getCategoryList(reqVO);
        return CommonResult.success(BeanUtils.toBean(list, CategoryRespVO.class));
    }

    @Operation(summary = "导出类目管理列表")
    @PreAuthorize("@ss.hasPermi('tax:category:export')")
    @Log(title = "类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export/{catType}")
    public void export(HttpServletResponse response, @PathVariable String catType, CategoryPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        exportReqVO.setCatType(catType);
        List<TaxonomyCategoryDO> list = (List<TaxonomyCategoryDO>) taxonomyCategoryService.getCategoryPage(exportReqVO).getRows();
        ExcelUtil<CategoryRespVO> util = new ExcelUtil<>(CategoryRespVO.class);
        util.exportExcel(response, CategoryConvert.INSTANCE.convertToRespVOList(list), "类目数据");
    }

    @Operation(summary = "导入类目管理列表")
    @PreAuthorize("@ss.hasPermi('tax:category:import')")
    @Log(title = "类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CategoryRespVO> util = new ExcelUtil<>(CategoryRespVO.class);
        List<CategoryRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = taxonomyCategoryService.importCategory(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('tax:category:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CategoryRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyCategoryDO category = taxonomyCategoryService.getCategoryById(id);
        return CommonResult.success(BeanUtils.toBean(category, CategoryRespVO.class));
    }

    @Operation(summary = "新增类目管理")
    @PreAuthorize("@ss.hasPermi('tax:category:add')")
    @Log(title = "类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CategorySaveReqVO reqVO) {
        reqVO.setCreatorId(getUserId());
        reqVO.setCreateBy(getNickName());
        reqVO.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(taxonomyCategoryService.createCategory(reqVO));
    }

    @Operation(summary = "修改类目管理")
    @PreAuthorize("@ss.hasPermi('tax:category:edit')")
    @Log(title = "类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CategorySaveReqVO reqVO) {
        reqVO.setUpdatorId(getUserId());
        reqVO.setUpdateBy(getNickName());
        reqVO.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(taxonomyCategoryService.updateCategory(reqVO));
    }

    @Operation(summary = "删除类目管理")
    @PreAuthorize("@ss.hasPermi('tax:category:remove')")
    @Log(title = "类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids,
                                        @RequestParam("catType") String catType) {
        return CommonResult.toAjax(taxonomyCategoryService.removeCategory(Arrays.asList(ids), catType));
    }

}
