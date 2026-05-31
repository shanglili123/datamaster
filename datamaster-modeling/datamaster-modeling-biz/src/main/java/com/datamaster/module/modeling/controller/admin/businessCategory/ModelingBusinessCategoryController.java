

package com.datamaster.module.modeling.controller.admin.businessCategory;

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
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategoryRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessCategorySaveReqVO;
import com.datamaster.module.modeling.convert.businessCategory.ModelingBusinessCategoryConvert;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessCategoryDO;
import com.datamaster.module.modeling.service.businessCategory.IModelingBusinessCategoryService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 业务分类Controller
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Tag(name = "业务分类")
@RestController
@RequestMapping("/dm/businessCategory")
@Validated
public class ModelingBusinessCategoryController extends BaseController {
    @Resource
    private IModelingBusinessCategoryService ModelingBusinessCategoryService;

    @Operation(summary = "查询业务分类列表")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:list')")
    @GetMapping("/listPage")
    public CommonResult<PageResult<ModelingBusinessCategoryRespVO>> list(ModelingBusinessCategoryPageReqVO ModelingBusinessCategory) {
        PageResult<ModelingBusinessCategoryDO> page = ModelingBusinessCategoryService.getModelingBusinessCategoryPage(ModelingBusinessCategory);
        return CommonResult.success(BeanUtils.toBean(page, ModelingBusinessCategoryRespVO.class));
    }

    @Operation(summary = "查询业务分类列表")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:list')")
    @GetMapping("/list")
    public CommonResult<List<ModelingBusinessCategoryRespVO>> listAll(ModelingBusinessCategoryPageReqVO ModelingBusinessCategory) {
        List<ModelingBusinessCategoryDO> page = ModelingBusinessCategoryService.getModelingBusinessCategoryList(ModelingBusinessCategory);
        return CommonResult.success(BeanUtils.toBean(page, ModelingBusinessCategoryRespVO.class));
    }


    @Operation(summary = "导出业务分类列表")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:export')")
    @Log(title = "业务分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelingBusinessCategoryPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelingBusinessCategoryDO> list = (List<ModelingBusinessCategoryDO>) ModelingBusinessCategoryService.getModelingBusinessCategoryPage(exportReqVO).getRows();
        ExcelUtil<ModelingBusinessCategoryRespVO> util = new ExcelUtil<>(ModelingBusinessCategoryRespVO.class);
        util.exportExcel(response, ModelingBusinessCategoryConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入业务分类列表")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:import')")
    @Log(title = "业务分类", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelingBusinessCategoryRespVO> util = new ExcelUtil<>(ModelingBusinessCategoryRespVO.class);
        List<ModelingBusinessCategoryRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ModelingBusinessCategoryService.importModelingBusinessCategory(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取业务分类详细信息")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelingBusinessCategoryRespVO> getInfo(@PathVariable("id") Long id) {
        ModelingBusinessCategoryDO ModelingBusinessCategoryDO = ModelingBusinessCategoryService.getModelingBusinessCategoryById(id);
        return CommonResult.success(BeanUtils.toBean(ModelingBusinessCategoryDO, ModelingBusinessCategoryRespVO.class));
    }

    @Operation(summary = "新增业务分类")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:add')")
    @Log(title = "业务分类", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelingBusinessCategorySaveReqVO ModelingBusinessCategory) {
        ModelingBusinessCategory.setCreatorId(getUserId());
        ModelingBusinessCategory.setCreateBy(getNickName());
        ModelingBusinessCategory.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingBusinessCategoryService.createModelingBusinessCategory(ModelingBusinessCategory));
    }

    @Operation(summary = "修改业务分类")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:edit')")
    @Log(title = "业务分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelingBusinessCategorySaveReqVO ModelingBusinessCategory) {
        ModelingBusinessCategory.setUpdatorId(getUserId());
        ModelingBusinessCategory.setUpdateBy(getNickName());
        ModelingBusinessCategory.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingBusinessCategoryService.updateModelingBusinessCategory(ModelingBusinessCategory));
    }

    @Operation(summary = "删除业务分类")
    @PreAuthorize("@ss.hasPermi('dm:businesscategory:remove')")
    @Log(title = "业务分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ModelingBusinessCategoryService.removeModelingBusinessCategory(Arrays.asList(ids)));
    }

}
