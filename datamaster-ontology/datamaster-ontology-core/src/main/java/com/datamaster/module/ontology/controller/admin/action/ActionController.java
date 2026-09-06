package com.datamaster.module.ontology.controller.admin.action;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.api.dto.ActionDataArrivalTriggerDTO;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.service.IActionApprovalService;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
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
    @Resource
    private IActionApprovalService approvalService;

    @Value("${datamaster.ontology.action-trigger.secret:}")
    private String actionTriggerSecret;

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
        // 人工入口固定保留“预览后手动确认执行”，自动执行只允许由受控触发入口设置。
        reqVO.setAutoExecute(false);
        reqVO.setTriggerType("MANUAL");
        reqVO.setTriggerRef(null);
        reqVO.setEventId(null);
        reqVO.setMaxAttempts(1);
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

    @Operation(summary = "回退已执行的记录（按执行前后快照还原）")
    @PreAuthorize("@ss.hasPermi('ont:action:edit')")
    @PostMapping("/execution/rollback/{id}")
    public CommonResult<ExecutionRespVO> rollback(@PathVariable Long id) {
        return CommonResult.success(executionService.rollbackExecution(id));
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

    @Operation(summary = "查询对象执行记录的人工确认审计（请求 + 确认任务 + 确认人结论）")
    @PreAuthorize("@ss.hasPermi('ont:action:query')")
    @GetMapping("/execution/approval-chain/{executionId}")
    public CommonResult<ApprovalChainRespVO> approvalChain(@PathVariable Long executionId) {
        return CommonResult.success(approvalService.getChain(executionId));
    }

    // ==================== 数据到达触发 ====================

    @Operation(summary = "数据到达触发：匹配 triggerRef 的启用动作自动提交并进入可靠执行队列")
    @PostMapping("/trigger/data-arrival")
    public CommonResult<List<ExecutionRespVO>> dataArrivalTrigger(
            @RequestHeader(value = "X-DataMaster-Trigger-Secret", required = false) String secret,
            @RequestBody(required = false) ActionDataArrivalTriggerDTO body,
            @RequestParam(required = false) String triggerRef,
            @RequestParam(required = false) String inputParams,
            @RequestParam(required = false) String objectKey,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) String spaceCode) {
        assertTriggerSecret(secret);
        ActionDataArrivalTriggerDTO request = body == null ? new ActionDataArrivalTriggerDTO() : body;
        if (request.getTriggerRef() == null) request.setTriggerRef(triggerRef);
        if (request.getInputParams() == null) request.setInputParams(inputParams);
        if (request.getObjectKey() == null) request.setObjectKey(objectKey);
        if (request.getEventId() == null) request.setEventId(eventId);
        if (request.getSpaceId() == null) request.setSpaceId(spaceId);
        if (request.getSpaceCode() == null) request.setSpaceCode(spaceCode);
        if (request.getTriggerRef() == null || request.getTriggerRef().trim().isEmpty()) {
            throw new IllegalArgumentException("triggerRef 不能为空");
        }
        return CommonResult.success(executionService.submitByTrigger(
                request.getTriggerRef(), request.getInputParams(), request.getObjectKey(),
                request.getEventId(), request.getSpaceId(), request.getSpaceCode()));
    }

    private void assertTriggerSecret(String secret) {
        if (actionTriggerSecret == null || actionTriggerSecret.trim().isEmpty()) {
            throw new IllegalStateException("HTTP 动作触发入口未配置 datamaster.ontology.action-trigger.secret");
        }
        if (!actionTriggerSecret.equals(secret)) {
            throw new AccessDeniedException("动作触发密钥错误");
        }
    }
}
