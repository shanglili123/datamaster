package com.datamaster.module.ontology.controller.admin.metricsource;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.metricsource.vo.MetricSourceApprovalSaveReqVO;
import com.datamaster.module.ontology.service.analysis.MetricSourceApprovalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.Map;

@RestController
@RequestMapping("/ont/metric-source-approval")
@Validated
public class MetricSourceApprovalController {
    private static final String READ = "@ss.hasAnyPermi('ont:metric-source:query,ont:metric-source:approve,ont:metric-source:revoke')";
    @Resource private MetricSourceApprovalService service;

    @GetMapping("/page") @PreAuthorize(READ)
    public CommonResult<PageResult<Map<String,Object>>> page(@RequestParam @Positive Long ontologyId,
            @RequestParam(defaultValue="1") @Min(1) int pageNum,
            @RequestParam(defaultValue="10") @Min(1) @Max(100) int pageSize) {
        return CommonResult.success(service.page(ontologyId,pageNum,pageSize));
    }
    @GetMapping("/settings") @PreAuthorize(READ)
    public CommonResult<Map<String,Object>> settings(@RequestParam @Positive Long ontologyId) {
        return CommonResult.success(service.settings(ontologyId));
    }
    @GetMapping("/history") @PreAuthorize(READ)
    public CommonResult<PageResult<Map<String,Object>>> history(@RequestParam @Positive Long ontologyId,
            @RequestParam @Positive Long bindingId,
            @RequestParam(defaultValue="1") @Min(1) int pageNum,
            @RequestParam(defaultValue="10") @Min(1) @Max(100) int pageSize) {
        return CommonResult.success(service.history(ontologyId,bindingId,pageNum,pageSize));
    }
    @PostMapping("/approve") @PreAuthorize("@ss.hasPermi('ont:metric-source:approve')")
    public CommonResult<Boolean> approve(@Valid @RequestBody MetricSourceApprovalSaveReqVO request) {
        service.approve(request); return CommonResult.success(true);
    }
    @PostMapping("/revoke") @PreAuthorize("@ss.hasPermi('ont:metric-source:revoke')")
    public CommonResult<Boolean> revoke(@Valid @RequestBody MetricSourceApprovalSaveReqVO request) {
        service.revoke(request); return CommonResult.success(true);
    }
    @GetMapping("/status") @PreAuthorize("@ss.hasPermi('ont:metric:execute')")
    public CommonResult<Map<String,Object>> status(@RequestParam @Positive Long ontologyId,@RequestParam @Positive Long bindingId) {
        return CommonResult.success(service.status(ontologyId,bindingId));
    }
}
