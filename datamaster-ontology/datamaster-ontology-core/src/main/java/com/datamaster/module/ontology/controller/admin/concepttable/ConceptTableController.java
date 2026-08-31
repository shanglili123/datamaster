package com.datamaster.module.ontology.controller.admin.concepttable;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePreviewRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO;
import com.datamaster.module.ontology.service.IConceptTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "概念表绑定管理")
@RestController
@RequestMapping("/ont/concept-table")
@Validated
public class ConceptTableController {

    @Resource
    private IConceptTableService conceptTableService;

    @Operation(summary = "分页查询概念表绑定")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<ConceptTableRespVO>> page(ConceptTablePageReqVO pageReqVO) {
        return CommonResult.success(conceptTableService.getConceptTablePage(pageReqVO));
    }

    @Operation(summary = "按概念ID查询表绑定列表")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:list')")
    @GetMapping("/list")
    public CommonResult<List<ConceptTableRespVO>> list(@RequestParam Long conceptId) {
        return CommonResult.success(conceptTableService.getConceptTableByConceptId(conceptId));
    }

    @Operation(summary = "获取表绑定详情")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:query')")
    @GetMapping("/{id}")
    public CommonResult<ConceptTableRespVO> get(@PathVariable Long id) {
        return CommonResult.success(conceptTableService.getConceptTableById(id));
    }

    @Operation(summary = "预览表绑定数据")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:list')")
    @GetMapping("/preview/{id}")
    public CommonResult<ConceptTablePreviewRespVO> preview(@PathVariable Long id,
                                                           @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
                                                           @RequestParam(value = "filters", required = false) String filters) {
        return CommonResult.success(conceptTableService.previewData(id, limit, filters));
    }

    @Operation(summary = "新增表绑定")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ConceptTableSaveReqVO createReqVO) {
        return CommonResult.success(conceptTableService.createConceptTable(createReqVO));
    }

    @Operation(summary = "修改表绑定")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ConceptTableSaveReqVO updateReqVO) {
        return CommonResult.success(conceptTableService.updateConceptTable(updateReqVO));
    }

    @Operation(summary = "删除表绑定")
    @PreAuthorize("@ss.hasPermi('ont:concept-table:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(conceptTableService.deleteConceptTable(id));
    }
}
