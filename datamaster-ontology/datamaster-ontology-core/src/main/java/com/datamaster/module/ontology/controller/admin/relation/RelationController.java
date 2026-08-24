package com.datamaster.module.ontology.controller.admin.relation;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationPageReqVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationRespVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationSaveReqVO;
import com.datamaster.module.ontology.service.IRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 本体关系 Controller
 */
@Tag(name = "本体关系管理")
@RestController
@RequestMapping("/ont/relation")
@Validated
public class RelationController {

    @Resource
    private IRelationService relationService;

    @Operation(summary = "分页查询关系")
    @PreAuthorize("@ss.hasPermi('ont:relation:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<RelationRespVO>> page(RelationPageReqVO pageReqVO) {
        return CommonResult.success(relationService.getRelationPage(pageReqVO));
    }

    @Operation(summary = "获取关系详情")
    @PreAuthorize("@ss.hasPermi('ont:relation:query')")
    @GetMapping("/{id}")
    public CommonResult<RelationRespVO> get(@PathVariable Long id) {
        return CommonResult.success(relationService.getRelationById(id));
    }

    @Operation(summary = "新增关系")
    @PreAuthorize("@ss.hasPermi('ont:relation:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody RelationSaveReqVO createReqVO) {
        return CommonResult.success(relationService.createRelation(createReqVO));
    }

    @Operation(summary = "修改关系")
    @PreAuthorize("@ss.hasPermi('ont:relation:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody RelationSaveReqVO updateReqVO) {
        return CommonResult.success(relationService.updateRelation(updateReqVO));
    }

    @Operation(summary = "删除关系")
    @PreAuthorize("@ss.hasPermi('ont:relation:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(relationService.deleteRelation(id));
    }
}
