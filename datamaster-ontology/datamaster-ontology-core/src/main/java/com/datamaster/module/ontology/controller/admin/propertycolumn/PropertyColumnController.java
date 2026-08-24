package com.datamaster.module.ontology.controller.admin.propertycolumn;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnRespVO;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnSaveReqVO;
import com.datamaster.module.ontology.service.IPropertyColumnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "属性字段绑定管理")
@RestController
@RequestMapping("/ont/property-column")
@Validated
public class PropertyColumnController {

    @Resource
    private IPropertyColumnService propertyColumnService;

    @Operation(summary = "按概念表绑定ID查询列映射列表")
    @PreAuthorize("@ss.hasPermi('ont:property-column:list')")
    @GetMapping("/list")
    public CommonResult<List<PropertyColumnRespVO>> list(@RequestParam Long conceptTableId) {
        return CommonResult.success(propertyColumnService.getPropertyColumnsByConceptTableId(conceptTableId));
    }

    @Operation(summary = "批量保存列映射")
    @PreAuthorize("@ss.hasPermi('ont:property-column:edit')")
    @PostMapping("/batch")
    public CommonResult<Boolean> batchSave(@RequestParam Long conceptTableId,
                                           @Valid @RequestBody List<PropertyColumnSaveReqVO> list) {
        propertyColumnService.batchSavePropertyColumns(conceptTableId, list);
        return CommonResult.success(true);
    }

    @Operation(summary = "删除列映射")
    @PreAuthorize("@ss.hasPermi('ont:property-column:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(propertyColumnService.deletePropertyColumn(id));
    }
}
