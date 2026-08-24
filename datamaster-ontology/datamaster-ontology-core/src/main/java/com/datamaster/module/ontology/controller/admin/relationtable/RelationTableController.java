package com.datamaster.module.ontology.controller.admin.relationtable;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableRespVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableSaveReqVO;
import com.datamaster.module.ontology.service.IRelationTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "关系关联表绑定管理")
@RestController
@RequestMapping("/ont/relation-table")
@Validated
public class RelationTableController {

    @Resource
    private IRelationTableService relationTableService;

    @Operation(summary = "分页查询关系关联表绑定")
    @PreAuthorize("@ss.hasPermi('ont:relation-table:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<RelationTableRespVO>> page(RelationTablePageReqVO pageReqVO) {
        return CommonResult.success(relationTableService.getRelationTablePage(pageReqVO));
    }

    @Operation(summary = "按关系ID查询关联表列表")
    @PreAuthorize("@ss.hasPermi('ont:relation-table:list')")
    @GetMapping("/list")
    public CommonResult<List<RelationTableRespVO>> list(@RequestParam Long relationId) {
        return CommonResult.success(relationTableService.getRelationTablesByRelationId(relationId));
    }

    @Operation(summary = "获取关联表绑定详情")
    @PreAuthorize("@ss.hasPermi('ont:relation-table:query')")
    @GetMapping("/{id}")
    public CommonResult<RelationTableRespVO> get(@PathVariable Long id) {
        return CommonResult.success(relationTableService.getRelationTableById(id));
    }

    @Operation(summary = "新增关联表绑定")
    @PreAuthorize("@ss.hasPermi('ont:relation-table:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody RelationTableSaveReqVO createReqVO) {
        return CommonResult.success(relationTableService.createRelationTable(createReqVO));
    }

    @Operation(summary = "修改关联表绑定")
    @PreAuthorize("@ss.hasPermi('ont:relation-table:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody RelationTableSaveReqVO updateReqVO) {
        return CommonResult.success(relationTableService.updateRelationTable(updateReqVO));
    }

    @Operation(summary = "删除关联表绑定")
    @PreAuthorize("@ss.hasPermi('ont:relation-table:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(relationTableService.deleteRelationTable(id));
    }
}
