package com.datamaster.module.ontology.controller.admin.property;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPageReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyRespVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertySaveReqVO;
import com.datamaster.module.ontology.service.IPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 本体属性 Controller
 */
@Tag(name = "本体属性管理")
@RestController
@RequestMapping("/ont/property")
@Validated
public class PropertyController {

    @Resource
    private IPropertyService propertyService;

    @Operation(summary = "分页查询属性")
    @PreAuthorize("@ss.hasPermi('ont:property:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<PropertyRespVO>> page(PropertyPageReqVO pageReqVO) {
        return CommonResult.success(propertyService.getPropertyPage(pageReqVO));
    }

    @Operation(summary = "获取属性详情")
    @PreAuthorize("@ss.hasPermi('ont:property:query')")
    @GetMapping("/{id}")
    public CommonResult<PropertyRespVO> get(@PathVariable Long id) {
        return CommonResult.success(propertyService.getPropertyById(id));
    }

    @Operation(summary = "新增属性")
    @PreAuthorize("@ss.hasPermi('ont:property:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody PropertySaveReqVO createReqVO) {
        return CommonResult.success(propertyService.createProperty(createReqVO));
    }

    @Operation(summary = "修改属性")
    @PreAuthorize("@ss.hasPermi('ont:property:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody PropertySaveReqVO updateReqVO) {
        return CommonResult.success(propertyService.updateProperty(updateReqVO));
    }

    @Operation(summary = "删除属性")
    @PreAuthorize("@ss.hasPermi('ont:property:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(propertyService.deleteProperty(id));
    }
}
