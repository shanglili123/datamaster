package com.datamaster.module.ontology.controller.admin.workflow;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.*;
import com.datamaster.module.ontology.service.IActionWorkflowService;
import com.datamaster.module.ontology.service.IActionWorkflowRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "本体动作编排")
@RestController
@RequestMapping("/ont/workflow")
@Validated
public class ActionWorkflowController {
    @Resource private IActionWorkflowService workflowService;
    @Resource private IActionWorkflowRuntimeService runtimeService;

    @Operation(summary = "分页查询动作编排")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    public CommonResult<PageResult<ActionWorkflowRespVO>> page(ActionWorkflowPageReqVO reqVO) {
        return CommonResult.success(workflowService.getWorkflowPage(reqVO));
    }
    @Operation(summary = "获取动作编排详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    public CommonResult<ActionWorkflowRespVO> get(@PathVariable Long id) { return CommonResult.success(workflowService.getWorkflow(id)); }

    @Operation(summary = "查询动作编排发布版本")
    @GetMapping("/{id}/versions")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    public CommonResult<java.util.List<ActionWorkflowVersionRespVO>> versions(@PathVariable Long id) {
        return CommonResult.success(workflowService.listWorkflowVersions(id));
    }

    @Operation(summary = "获取动作编排指定版本")
    @GetMapping("/{id}/versions/{version}")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    public CommonResult<ActionWorkflowRespVO> version(@PathVariable Long id, @PathVariable Integer version) {
        return CommonResult.success(workflowService.getWorkflowVersion(id, version));
    }

    @Operation(summary = "按历史版本生成新的 Workflow 草稿")
    @PostMapping("/{id}/versions/{version}/rollback")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<Boolean> rollback(@PathVariable Long id, @PathVariable Integer version) {
        workflowService.rollbackWorkflow(id, version);
        return CommonResult.success(true);
    }
    @Operation(summary = "新增动作编排")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('ont:action:add')")
    public CommonResult<Long> create(@Valid @RequestBody ActionWorkflowSaveReqVO reqVO) { return CommonResult.success(workflowService.createWorkflow(reqVO)); }
    @Operation(summary = "修改动作编排")
    @PutMapping
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<Integer> update(@Valid @RequestBody ActionWorkflowSaveReqVO reqVO) { return CommonResult.success(workflowService.updateWorkflow(reqVO)); }
    @Operation(summary = "删除动作编排")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ont:action:remove')")
    public CommonResult<Integer> delete(@PathVariable Long id) { return CommonResult.success(workflowService.deleteWorkflow(id)); }
    @Operation(summary = "校验动作编排 DAG")
    @PostMapping("/{id}/validate")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<Boolean> validate(@PathVariable Long id) { workflowService.validateWorkflow(id); return CommonResult.success(true); }
    @Operation(summary = "发布动作编排")
    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<Boolean> publish(@PathVariable Long id) { workflowService.publishWorkflow(id); return CommonResult.success(true); }
    @Operation(summary = "停用动作编排")
    @PostMapping("/{id}/disable")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<Boolean> disable(@PathVariable Long id) { workflowService.disableWorkflow(id); return CommonResult.success(true); }

    @Operation(summary = "启动动作编排")
    @PostMapping("/{id}/run")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<ActionWorkflowRunRespVO> run(@PathVariable Long id,
                                                      @RequestBody(required = false) ActionWorkflowRunReqVO reqVO) {
        return CommonResult.success(runtimeService.start(id, reqVO));
    }

    @Operation(summary = "获取动作编排运行详情")
    @GetMapping("/runs/{runId}")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    public CommonResult<ActionWorkflowRunRespVO> runDetail(@PathVariable Long runId) {
        return CommonResult.success(runtimeService.get(runId));
    }

    @Operation(summary = "续跑动作编排")
    @PostMapping("/runs/{runId}/resume")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<ActionWorkflowRunRespVO> resume(@PathVariable Long runId,
                                                         @RequestBody(required = false) ActionWorkflowRunReqVO reqVO) {
        return CommonResult.success(runtimeService.resume(runId, reqVO));
    }

    @Operation(summary = "发起 Workflow 补偿动作")
    @PostMapping("/runs/{runId}/compensate")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<ActionWorkflowCompensationRespVO> compensate(@PathVariable Long runId) {
        return CommonResult.success(runtimeService.compensate(runId));
    }

    @Operation(summary = "重试失败的 Workflow 补偿动作")
    @PostMapping("/runs/{runId}/compensate/retry")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<ActionWorkflowCompensationRespVO> retryCompensation(@PathVariable Long runId) {
        return CommonResult.success(runtimeService.retryCompensation(runId));
    }

    @Operation(summary = "查询动作编排运行记录")
    @GetMapping("/{id}/runs/page")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    public CommonResult<PageResult<ActionWorkflowRunRespVO>> runPage(
            @PathVariable Long id, @Valid ActionWorkflowRunPageReqVO reqVO) {
        return CommonResult.success(runtimeService.page(id, reqVO));
    }

    @Operation(summary = "查询动作编排运行记录（兼容接口）")
    @GetMapping("/{id}/runs")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    public CommonResult<java.util.List<ActionWorkflowRunRespVO>> runs(@PathVariable Long id) {
        return CommonResult.success(runtimeService.list(id));
    }

    @Operation(summary = "查询动作编排统一运行监控")
    @GetMapping("/monitor")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    public CommonResult<ActionWorkflowMonitorRespVO> monitor(
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return CommonResult.success(runtimeService.monitor(limit));
    }

    @Operation(summary = "取消动作编排运行")
    @PostMapping("/runs/{runId}/cancel")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    public CommonResult<ActionWorkflowRunRespVO> cancel(@PathVariable Long runId) {
        return CommonResult.success(runtimeService.cancel(runId));
    }
}
