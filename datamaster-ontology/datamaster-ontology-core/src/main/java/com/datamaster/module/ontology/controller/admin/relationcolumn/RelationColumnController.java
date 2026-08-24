package com.datamaster.module.ontology.controller.admin.relationcolumn;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnRespVO;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnSaveReqVO;
import com.datamaster.module.ontology.service.IRelationColumnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "关系字段绑定管理")
@RestController
@RequestMapping("/ont/relation-column")
@Validated
public class RelationColumnController {

    @Resource
    private IRelationColumnService relationColumnService;

    @Operation(summary = "按关系ID查询列映射列表")
    @PreAuthorize("@ss.hasPermi('ont:relation-column:list')")
    @GetMapping("/list")
    public CommonResult<List<RelationColumnRespVO>> list(@RequestParam Long relationId) {
        return CommonResult.success(relationColumnService.getRelationColumnsByRelationId(relationId));
    }

    @Operation(summary = "批量保存关系列映射")
    @PreAuthorize("@ss.hasPermi('ont:relation-column:edit')")
    @PostMapping("/batch")
    public CommonResult<Boolean> batchSave(@RequestParam Long relationId,
                                           @Valid @RequestBody List<RelationColumnSaveReqVO> list) {
        relationColumnService.batchSaveRelationColumns(relationId, list);
        return CommonResult.success(true);
    }

    @Operation(summary = "删除关系列映射")
    @PreAuthorize("@ss.hasPermi('ont:relation-column:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(relationColumnService.deleteRelationColumn(id));
    }
}
