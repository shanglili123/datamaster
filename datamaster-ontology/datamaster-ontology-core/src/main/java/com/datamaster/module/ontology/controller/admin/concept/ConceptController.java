package com.datamaster.module.ontology.controller.admin.concept;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptPageReqVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptRespVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;
import com.datamaster.module.ontology.service.IConceptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 本体概念 Controller
 */
@Tag(name = "本体概念管理")
@RestController
@RequestMapping("/ont/concept")
@Validated
public class ConceptController {

    @Resource
    private IConceptService conceptService;

    @Operation(summary = "分页查询概念")
    @PreAuthorize("@ss.hasPermi('ont:concept:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<ConceptRespVO>> page(ConceptPageReqVO pageReqVO) {
        return CommonResult.success(conceptService.getConceptPage(pageReqVO));
    }

    @Operation(summary = "获取概念详情")
    @PreAuthorize("@ss.hasPermi('ont:concept:query')")
    @GetMapping("/{id}")
    public CommonResult<ConceptRespVO> get(@PathVariable Long id) {
        return CommonResult.success(conceptService.getConceptById(id));
    }

    @Operation(summary = "新增概念")
    @PreAuthorize("@ss.hasPermi('ont:concept:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ConceptSaveReqVO createReqVO) {
        return CommonResult.success(conceptService.createConcept(createReqVO));
    }

    @Operation(summary = "修改概念")
    @PreAuthorize("@ss.hasPermi('ont:concept:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ConceptSaveReqVO updateReqVO) {
        return CommonResult.success(conceptService.updateConcept(updateReqVO));
    }

    @Operation(summary = "删除概念")
    @PreAuthorize("@ss.hasPermi('ont:concept:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(conceptService.deleteConcept(id));
    }
}
