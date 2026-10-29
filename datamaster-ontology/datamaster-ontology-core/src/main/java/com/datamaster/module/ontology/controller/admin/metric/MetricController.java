package com.datamaster.module.ontology.controller.admin.metric;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.service.IMetricService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "本体指标管理（仅元数据定义）")
@RestController
@RequestMapping("/ont/metric")
@Validated
public class MetricController {
    @Resource private IMetricService metricService;

    @PreAuthorize("@ss.hasPermi('ont:metric:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<MetricRespVO>> page(@Valid MetricPageReqVO req) {
        return CommonResult.success(metricService.getPage(req));
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:query')")
    @GetMapping("/{id}")
    public CommonResult<MetricRespVO> get(@PathVariable Long id) {
        return CommonResult.success(metricService.get(id));
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody MetricSaveReqVO req) {
        return CommonResult.success(metricService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody MetricSaveReqVO req) {
        return CommonResult.success(metricService.update(req));
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(metricService.delete(id));
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:edit')")
    @PostMapping("/{id}/validate")
    public CommonResult<Boolean> validate(@PathVariable Long id,
                                          @RequestParam(required = false) Integer revision) {
        metricService.validate(id);
        return CommonResult.success(true);
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:publish')")
    @PostMapping("/{id}/publish")
    public CommonResult<Boolean> publish(@PathVariable Long id,
                                         @RequestParam(required = false) Integer revision) {
        metricService.publish(id, revision);
        return CommonResult.success(true);
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:publish')")
    @PostMapping("/{id}/disable")
    public CommonResult<Boolean> disable(@PathVariable Long id,
                                         @RequestParam(required = false) Integer revision) {
        metricService.disable(id, revision);
        return CommonResult.success(true);
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:query')")
    @GetMapping("/{id}/versions")
    public CommonResult<List<MetricVersionRespVO>> versions(@PathVariable Long id) {
        return CommonResult.success(metricService.versions(id));
    }

    @PreAuthorize("@ss.hasPermi('ont:metric:query')")
    @GetMapping("/{id}/versions/{version}")
    public CommonResult<MetricRespVO> version(@PathVariable Long id, @PathVariable Integer version) {
        return CommonResult.success(metricService.version(id, version));
    }
}
