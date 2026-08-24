package com.datamaster.module.ontology.controller.admin.action;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 本体动作管理 Controller
 */
@Tag(name = "本体动作管理")
@RestController
@RequestMapping("/ont/action")
@Validated
public class ActionController {

    @Resource
    private IActionService actionService;
    @Resource
    private IActionExecutionService executionService;

    // ==================== Action CRUD ====================

    @Operation(summary = "分页查询动作")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<ActionRespVO>> page(ActionPageReqVO pageReqVO) {
        return CommonResult.success(actionService.getActionPage(pageReqVO));
    }

    @Operation(summary = "按本体ID查询动作列表")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    @GetMapping("/list")
    public CommonResult<List<ActionRespVO>> list(@RequestParam Long ontologyId) {
        return CommonResult.success(actionService.getActionsByOntologyId(ontologyId));
    }

    @Operation(summary = "获取动作详情")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    @GetMapping("/{id}")
    public CommonResult<ActionRespVO> get(@PathVariable Long id) {
        return CommonResult.success(actionService.getActionById(id));
    }

    @Operation(summary = "新增动作")
    @PreAuthorize("@ss.hasPermi('ont:action:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ActionSaveReqVO createReqVO) {
        return CommonResult.success(actionService.createAction(createReqVO));
    }

    @Operation(summary = "修改动作")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ActionSaveReqVO updateReqVO) {
        return CommonResult.success(actionService.updateAction(updateReqVO));
    }

    @Operation(summary = "删除动作")
    @PreAuthorize("@ss.hasPermi('ont:action:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(actionService.deleteAction(id));
    }

    // ==================== Execution ====================

    @Operation(summary = "提交执行（生成SQL + dry-run + 进入审批）")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    @PostMapping("/execution/submit")
    public CommonResult<ExecutionRespVO> submitExecution(@Valid @RequestBody ExecutionSubmitReqVO reqVO) {
        return CommonResult.success(executionService.submitExecution(reqVO));
    }

    @Operation(summary = "审批通过")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    @PostMapping("/execution/approve")
    public CommonResult<Boolean> approveExecution(@Valid @RequestBody ApprovalReqVO reqVO) {
        executionService.approveExecution(reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "审批拒绝")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    @PostMapping("/execution/reject")
    public CommonResult<Boolean> rejectExecution(@Valid @RequestBody ApprovalReqVO reqVO) {
        executionService.rejectExecution(reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "执行已批准的动作")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    @PostMapping("/execution/run/{id}")
    public CommonResult<ExecutionRespVO> execute(@PathVariable Long id) {
        return CommonResult.success(executionService.executeExecution(id));
    }

    @Operation(summary = "获取执行详情")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    @GetMapping("/execution/{id}")
    public CommonResult<ExecutionRespVO> getExecution(@PathVariable Long id) {
        return CommonResult.success(executionService.getExecutionById(id));
    }

    @Operation(summary = "分页查询执行记录")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    @GetMapping("/execution/page")
    public CommonResult<PageResult<ExecutionRespVO>> executionPage(ExecutionPageReqVO pageReqVO) {
        return CommonResult.success(executionService.getExecutionPage(pageReqVO));
    }

    @Operation(summary = "查询待审批列表")
    @PreAuthorize("@ss.hasPermi('ont:action:list')")
    @GetMapping("/execution/pending")
    public CommonResult<List<ExecutionRespVO>> pendingApprovals(@RequestParam Long ontologyId) {
        return CommonResult.success(executionService.getPendingApprovals(ontologyId));
    }
}
