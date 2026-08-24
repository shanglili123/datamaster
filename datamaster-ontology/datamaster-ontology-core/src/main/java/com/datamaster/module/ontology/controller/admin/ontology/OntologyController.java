package com.datamaster.module.ontology.controller.admin.ontology;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.service.IOntologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 本体 Controller
 */
@Tag(name = "本体管理")
@RestController
@RequestMapping("/ont/ontology")
@Validated
public class OntologyController {

    @Resource
    private IOntologyService ontologyService;

    @Operation(summary = "分页查询本体")
    @PreAuthorize("@ss.hasPermi('ont:ontology:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<OntologyRespVO>> page(OntologyPageReqVO pageReqVO) {
        return CommonResult.success(ontologyService.getOntologyPage(pageReqVO));
    }

    @Operation(summary = "获取本体详情")
    @PreAuthorize("@ss.hasPermi('ont:ontology:query')")
    @GetMapping("/{id}")
    public CommonResult<OntologyRespVO> get(@PathVariable Long id) {
        return CommonResult.success(ontologyService.getOntologyById(id));
    }

    @Operation(summary = "新增本体")
    @PreAuthorize("@ss.hasPermi('ont:ontology:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody OntologySaveReqVO createReqVO) {
        return CommonResult.success(ontologyService.createOntology(createReqVO));
    }

    @Operation(summary = "修改本体")
    @PreAuthorize("@ss.hasPermi('ont:ontology:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody OntologySaveReqVO updateReqVO) {
        return CommonResult.success(ontologyService.updateOntology(updateReqVO));
    }

    @Operation(summary = "删除本体")
    @PreAuthorize("@ss.hasPermi('ont:ontology:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(ontologyService.deleteOntology(id));
    }
}
