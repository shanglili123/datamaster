package com.datamaster.module.ontology.controller.admin.analysis;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "受控指标查询与本人保存分析")
@RestController
@RequestMapping("/ont/metric-analysis")
@Validated
public class MetricAnalysisController {
    @Resource private IMetricAnalysisService service;

    @PostMapping("/query")
    @PreAuthorize("@ss.hasPermi('ont:metric:execute')")
    public CommonResult<MetricQueryRespVO> query(@Valid @RequestBody MetricQueryReqVO req) {
        return CommonResult.success(service.query(req));
    }
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('ont:analysis:list')")
    public CommonResult<PageResult<MetricAnalysisRespVO>> page(@Valid MetricAnalysisPageReqVO req) {
        return CommonResult.success(service.page(req));
    }
    @PostMapping
    @PreAuthorize("@ss.hasPermi('ont:analysis:add')")
    public CommonResult<Long> create(@Valid @RequestBody MetricAnalysisSaveReqVO req) {
        return CommonResult.success(service.create(req));
    }
    @PutMapping
    @PreAuthorize("@ss.hasPermi('ont:analysis:edit')")
    public CommonResult<Integer> update(@Valid @RequestBody MetricAnalysisSaveReqVO req) {
        return CommonResult.success(service.update(req));
    }
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ont:analysis:query')")
    public CommonResult<MetricAnalysisRespVO> get(@PathVariable Long id) {
        return CommonResult.success(service.get(id));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ont:analysis:remove')")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(service.delete(id));
    }
}
