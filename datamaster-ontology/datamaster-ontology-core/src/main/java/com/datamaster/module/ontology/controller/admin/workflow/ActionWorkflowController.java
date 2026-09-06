package com.datamaster.module.ontology.controller.admin.workflow;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.*;
import com.datamaster.module.ontology.service.IActionWorkflowService;
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
}
