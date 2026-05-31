

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
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelPageReqVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelRespVO;
import com.datamaster.module.modeling.controller.admin.businessCategory.vo.ModelingBusinessDomainRelSaveReqVO;
import com.datamaster.module.modeling.convert.businessCategory.ModelingBusinessDomainRelConvert;
import com.datamaster.module.modeling.dal.dataobject.businessCategory.ModelingBusinessDomainRelDO;
import com.datamaster.module.modeling.service.businessCategory.IModelingBusinessDomainRelService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 业务分类数据域关联关系Controller
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Tag(name = "业务分类数据域关联关系")
@RestController
@RequestMapping("/dm/businessDomainRel")
@Validated
public class ModelingBusinessDomainRelController extends BaseController {
    @Resource
    private IModelingBusinessDomainRelService ModelingBusinessDomainRelService;

    @Operation(summary = "查询业务分类数据域关联关系列表")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelingBusinessDomainRelRespVO>> list(ModelingBusinessDomainRelPageReqVO ModelingBusinessDomainRel) {
        PageResult<ModelingBusinessDomainRelDO> page = ModelingBusinessDomainRelService.getModelingBusinessDomainRelPage(ModelingBusinessDomainRel);
        return CommonResult.success(BeanUtils.toBean(page, ModelingBusinessDomainRelRespVO.class));
    }

    @Operation(summary = "导出业务分类数据域关联关系列表")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:export')")
    @Log(title = "业务分类数据域关联关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelingBusinessDomainRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelingBusinessDomainRelDO> list = (List<ModelingBusinessDomainRelDO>) ModelingBusinessDomainRelService.getModelingBusinessDomainRelPage(exportReqVO).getRows();
        ExcelUtil<ModelingBusinessDomainRelRespVO> util = new ExcelUtil<>(ModelingBusinessDomainRelRespVO.class);
        util.exportExcel(response, ModelingBusinessDomainRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入业务分类数据域关联关系列表")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:import')")
    @Log(title = "业务分类数据域关联关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelingBusinessDomainRelRespVO> util = new ExcelUtil<>(ModelingBusinessDomainRelRespVO.class);
        List<ModelingBusinessDomainRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ModelingBusinessDomainRelService.importModelingBusinessDomainRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取业务分类数据域关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelingBusinessDomainRelRespVO> getInfo(@PathVariable("id") Long id) {
        ModelingBusinessDomainRelDO ModelingBusinessDomainRelDO = ModelingBusinessDomainRelService.getModelingBusinessDomainRelById(id);
        return CommonResult.success(BeanUtils.toBean(ModelingBusinessDomainRelDO, ModelingBusinessDomainRelRespVO.class));
    }

    @Operation(summary = "新增业务分类数据域关联关系")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:add')")
    @Log(title = "业务分类数据域关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelingBusinessDomainRelSaveReqVO ModelingBusinessDomainRel) {
        ModelingBusinessDomainRel.setCreatorId(getUserId());
        ModelingBusinessDomainRel.setCreateBy(getNickName());
        ModelingBusinessDomainRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingBusinessDomainRelService.createModelingBusinessDomainRel(ModelingBusinessDomainRel));
    }

    @Operation(summary = "修改业务分类数据域关联关系")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:edit')")
    @Log(title = "业务分类数据域关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelingBusinessDomainRelSaveReqVO ModelingBusinessDomainRel) {
        ModelingBusinessDomainRel.setUpdatorId(getUserId());
        ModelingBusinessDomainRel.setUpdateBy(getNickName());
        ModelingBusinessDomainRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingBusinessDomainRelService.updateModelingBusinessDomainRel(ModelingBusinessDomainRel));
    }

    @Operation(summary = "删除业务分类数据域关联关系")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:remove')")
    @Log(title = "业务分类数据域关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ModelingBusinessDomainRelService.removeModelingBusinessDomainRel(Arrays.asList(ids)));
    }

    @Operation(summary = "删除业务分类数据域关联关系")
    @PreAuthorize("@ss.hasPermi('dm:businessdomainrel:remove')")
    @Log(title = "业务分类数据域关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/deletebyDomainId/{domainId}/{businessCategoryId}")
    public CommonResult<Integer> deletebyDomainId(@PathVariable Long domainId, @PathVariable Long businessCategoryId) {
        return CommonResult.toAjax(ModelingBusinessDomainRelService.removeModelingBusinessDomainRelByDomainId(domainId, businessCategoryId));
    }

}
