package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionSubmitReqVO;
import com.datamaster.module.ontology.controller.admin.workflow.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IActionWorkflowRuntimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Workflow V2 运行时：支持已发布 DAG 中的串行动作节点、结构化条件分支和并行拆分/汇聚。
 * 节点超时会安全转入人工对账（不强行中断可能已产生副作用的底层动作）；
 * 失败运行可按已执行步骤逆序提交补偿动作，但只进入人工审批/执行链，不自动重放。
 */
@Service
@Slf4j
public class ActionWorkflowRuntimeServiceImpl implements IActionWorkflowRuntimeService {

    private static final String RUNNING = "RUNNING";
    private static final String QUEUED = "QUEUED";
    private static final String WAITING_APPROVAL = "WAITING_APPROVAL";
    private static final String EXECUTED = "EXECUTED";
    private static final String FAILED = "FAILED";
    private static final String CANCELED = "CANCELED";
    private static final String RECONCILIATION_REQUIRED = "RECONCILIATION_REQUIRED";
    private static final String COMPENSATION_NONE = "NONE";
    private static final String COMPENSATION_REQUESTED = "REQUESTED";
    private static final String COMPENSATION_PARTIAL = "PARTIAL";
    private static final String COMPENSATION_COMPENSATED = "COMPENSATED";
    private static final String COMPENSATION_FAILED = "FAILED";
    private static final Set<String> RUN_STATUSES = new HashSet<>(Arrays.asList(
            QUEUED, RUNNING, WAITING_APPROVAL, EXECUTED, FAILED, CANCELED, RECONCILIATION_REQUIRED));

    @Resource private ActionWorkflowMapper workflowMapper;
    @Resource private OntologyMapper ontologyMapper;
    @Resource private ActionWorkflowNodeMapper nodeMapper;
    @Resource private ActionWorkflowEdgeMapper edgeMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private ActionWorkflowRunMapper runMapper;
    @Resource private ActionWorkflowStepMapper stepMapper;
    @Resource private ActionExecutionMapper executionMapper;
    @Resource private ActionWorkflowAccessGuard accessGuard;
    @Resource private ActionWorkflowAlertService workflowAlertService;
    @Resource private IActionExecutionService executionService;
    @Resource private ObjectStateConditionResolver objectStateConditionResolver;

    @Override
    public ActionWorkflowRunRespVO start(Long workflowId, ActionWorkflowRunReqVO reqVO) {
        ActionWorkflowDO workflow = requirePublishedWorkflow(workflowId);
        accessGuard.assertWorkflowAccess(workflow);
        String idempotencyKey = reqVO == null ? null : StringUtils.trim(reqVO.getIdempotencyKey());
        if (StringUtils.isBlank(idempotencyKey) && reqVO != null && StringUtils.isNotBlank(reqVO.getEventId())) {
            idempotencyKey = "event:" + StringUtils.trim(reqVO.getEventId());
        }
        if (idempotencyKey != null && idempotencyKey.length() > 256) {
            throw new ServiceException("Workflow 幂等键不能超过 256 个字符");
        }
        if (StringUtils.isNotBlank(idempotencyKey)) {
            ActionWorkflowRunDO existing = runMapper.selectByWorkflowIdAndIdempotencyKey(workflowId, idempotencyKey);
            if (existing != null) return toResp(existing);
        }
        validateWorkflowInput(workflow, reqVO == null ? null : reqVO.getInputParams());
        ActionWorkflowRunDO run = new ActionWorkflowRunDO();
        run.setWorkflowId(workflowId);
        run.setIdempotencyKey(idempotencyKey);
        boolean async = reqVO != null && Boolean.TRUE.equals(reqVO.getAsync());
        run.setStatus(async ? QUEUED : RUNNING);
        run.setInputParams(normalizeJson(reqVO == null ? null : reqVO.getInputParams()));
        run.setOutputContext(JSON.toJSONString(buildInitialContext(reqVO)));
        run.setStartedTime(new Date());
        run.setCancelRequested(0);
        run.setCompensationStatus(COMPENSATION_NONE);
        run.setCreatorId(SecurityUtils.getUserId());
        run.setCreateBy(SecurityUtils.getUsername());
        try {
            runMapper.insert(run);
        } catch (RuntimeException insertError) {
            if (StringUtils.isNotBlank(idempotencyKey)) {
                ActionWorkflowRunDO winner = runMapper.selectByWorkflowIdAndIdempotencyKey(workflowId, idempotencyKey);
                if (winner != null) return toResp(winner);
            }
            throw insertError;
        }
        if (async) return toResp(run);
        try {
            executeLinear(run, workflow, reqVO);
        } catch (Exception e) {
            markRunFailed(run, e.getMessage());
        }
        return toResp(requireRun(run.getId()));
    }

    @Override
    public List<ActionWorkflowRunRespVO> triggerByEvent(String triggerRef, String inputParams,
                                                         String objectKey, String eventId,
                                                         Long spaceId, String spaceCode) {
        if (StringUtils.isBlank(triggerRef)) throw new IllegalArgumentException("triggerRef 不能为空");
        if (StringUtils.isBlank(eventId)) throw new IllegalArgumentException("数据到达触发必须携带稳定 eventId");
        ActionWorkflowRunReqVO req = new ActionWorkflowRunReqVO();
        req.setInputParams(inputParams);
        req.setObjectKey(objectKey);
        req.setEventId(eventId);
        req.setSpaceId(spaceId);
        req.setSpaceCode(spaceCode);
        req.setAsync(true);
        Map<String, Object> context = buildInitialContext(req);
        context.put("triggerType", "DATA_ARRIVAL");
        context.put("triggerRef", triggerRef.trim());
        context.put("currentUser", "workflow-trigger");

        List<ActionWorkflowRunRespVO> result = new ArrayList<>();
        for (ActionWorkflowDO workflow : workflowMapper.selectByTriggerRef(triggerRef)) {
            if (!matchesTriggerSpace(workflow, spaceId, spaceCode)) continue;
            String idempotencyKey = "event:" + eventId.trim();
            ActionWorkflowRunDO existing = runMapper.selectByWorkflowIdAndIdempotencyKey(workflow.getId(), idempotencyKey);
            if (existing != null) {
                result.add(toResp(existing));
                continue;
            }
            validateWorkflowInput(workflow, inputParams);
            ActionWorkflowRunDO run = new ActionWorkflowRunDO();
            run.setWorkflowId(workflow.getId());
            run.setIdempotencyKey(idempotencyKey);
            run.setStatus(QUEUED);
            run.setInputParams(normalizeJson(inputParams));
            run.setOutputContext(JSON.toJSONString(context));
            run.setStartedTime(new Date());
            run.setCancelRequested(0);
            run.setCompensationStatus(COMPENSATION_NONE);
            run.setCreatorId(workflow.getCreatorId());
            run.setCreateBy(StringUtils.defaultIfBlank(workflow.getCreateBy(), "workflow-trigger"));
            try {
                runMapper.insert(run);
                result.add(toResp(run));
            } catch (RuntimeException insertError) {
                ActionWorkflowRunDO winner = runMapper.selectByWorkflowIdAndIdempotencyKey(workflow.getId(), idempotencyKey);
                if (winner != null) result.add(toResp(winner));
                else log.error("数据到达触发 Workflow 入队失败 workflowId={} eventId={}", workflow.getId(), eventId, insertError);
            }
        }
        return result;
    }

    @Override
    public void executeQueued(Long runId) {
        ActionWorkflowRunDO run = requireRun(runId);
        if (!RUNNING.equals(run.getStatus())) return;
        try {
            ActionWorkflowDO workflow = requirePublishedWorkflow(run.getWorkflowId());
            validateWorkflowInput(workflow, run.getInputParams());
            executeLinear(run, workflow, null);
        } catch (Exception e) {
            markRunFailed(run, e.getMessage());
        }
    }

    private boolean matchesTriggerSpace(ActionWorkflowDO workflow, Long spaceId, String spaceCode) {
        if (workflow == null || workflow.getOntologyId() == null) return false;
        OntologyDO ontology = ontologyMapper.selectById(workflow.getOntologyId());
        if (ontology == null) return false;
        if (spaceId == null && StringUtils.isBlank(spaceCode)) return false;
        if (spaceId != null && !spaceId.equals(ontology.getSpaceId())) return false;
        if (StringUtils.isNotBlank(spaceCode) && !spaceCode.equals(ontology.getSpaceCode())) return false;
        return true;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public ActionWorkflowRunRespVO get(Long runId) {
        ActionWorkflowRunDO run = requireRun(runId);
        accessGuard.assertWorkflowAccess(requireWorkflowForRun(run));
        return toResp(run);
    }

    @Override
    public ActionWorkflowRunRespVO resume(Long runId, ActionWorkflowRunReqVO reqVO) {
        ActionWorkflowRunDO run = requireRun(runId);
        accessGuard.assertWorkflowAccess(requireWorkflowForRun(run));
        if (!WAITING_APPROVAL.equals(run.getStatus()) && !FAILED.equals(run.getStatus())) {
            throw new ServiceException("当前运行状态不能续跑：" + run.getStatus());
        }
        ActionWorkflowDO workflow = requirePublishedWorkflow(run.getWorkflowId());
        validateWorkflowInput(workflow, run.getInputParams());
        boolean async = reqVO != null && Boolean.TRUE.equals(reqVO.getAsync());
        int resumed = runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, runId)
                .in(ActionWorkflowRunDO::getStatus, WAITING_APPROVAL, FAILED)
                .set(ActionWorkflowRunDO::getStatus, async ? QUEUED : RUNNING)
                .set(ActionWorkflowRunDO::getErrorMessage, null)
                .set(ActionWorkflowRunDO::getFinishedTime, null)
                .set(ActionWorkflowRunDO::getCancelRequested, 0)
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
        if (resumed == 0) {
            throw new ServiceException("Workflow 运行状态已变化，请刷新后重试：" + requireRun(runId).getStatus());
        }
        run = requireRun(runId);
        if (async) return toResp(run);
        try {
            executeLinear(run, workflow, reqVO);
        } catch (Exception e) {
            markRunFailed(run, e.getMessage());
        }
        return toResp(requireRun(run.getId()));
    }

    @Override
    public ActionWorkflowCompensationRespVO compensate(Long runId) {
        ActionWorkflowRunDO run = requireRun(runId);
        ActionWorkflowDO workflow = requireWorkflowForRun(run);
        accessGuard.assertWorkflowAccess(workflow);
        if (!FAILED.equals(run.getStatus()) && !RECONCILIATION_REQUIRED.equals(run.getStatus())) {
            throw new ServiceException("只有失败或需人工对账的 Workflow 才能发起补偿：" + run.getStatus());
        }
        Map<String, ActionWorkflowNodeDO> nodes = nodeMapper.selectByWorkflowId(workflow.getId()).stream()
                .collect(Collectors.toMap(ActionWorkflowNodeDO::getNodeKey, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        List<ActionWorkflowStepDO> eligible = stepMapper.selectByRunId(runId).stream()
                .filter(step -> EXECUTED.equals(step.getStatus()))
                .filter(step -> nodes.get(step.getNodeKey()) != null
                        && nodes.get(step.getNodeKey()).getCompensationActionId() != null)
                .sorted((left, right) -> {
                    Long leftId = left.getId();
                    Long rightId = right.getId();
                    if (leftId == null && rightId == null) return 0;
                    if (leftId == null) return 1;
                    if (rightId == null) return -1;
                    return rightId.compareTo(leftId);
                })
                .collect(Collectors.toList());
        ActionWorkflowCompensationRespVO result = new ActionWorkflowCompensationRespVO();
        result.setRunId(runId);
        refreshCompensationStatus(run);
        fillCompensationStatus(result, run);
        result.setEligibleCount(eligible.size());
        if (eligible.isEmpty()) {
            result.setMessage("没有已执行且配置补偿动作的节点");
            return result;
        }

        Map<String, Object> context = parseObject(run.getOutputContext());
        for (ActionWorkflowStepDO step : eligible) {
            ActionWorkflowNodeDO node = nodes.get(step.getNodeKey());
            ActionExecutionDO originalExecution = step.getExecutionId() == null
                    ? null : executionMapper.selectById(step.getExecutionId());
            ActionDO compensation = actionMapper.selectById(node.getCompensationActionId());
            if (compensation == null) {
                result.getErrors().add("节点「" + step.getNodeKey() + "」的补偿动作不存在");
                continue;
            }
            if (!Objects.equals(workflow.getOntologyId(), compensation.getOntologyId())) {
                result.getErrors().add("节点「" + step.getNodeKey() + "」的补偿动作不属于当前本体");
                continue;
            }
            if (!Boolean.TRUE.equals(compensation.getNeedsApproval())
                    && (compensation.getApprovalLevels() == null || compensation.getApprovalLevels() <= 0)) {
                result.getErrors().add("补偿动作「" + compensation.getName() + "」必须配置人工审批");
                continue;
            }
            ExecutionSubmitReqVO request = new ExecutionSubmitReqVO();
            request.setActionId(compensation.getId());
            request.setInputParams(normalizeJson(step.getInputParams()));
            request.setObjectKey(StringUtils.defaultIfBlank(stringValue(context.get("objectKey")),
                    originalExecution == null ? null : originalExecution.getObjectKey()));
            Long contextSpaceId = asLong(context.get("spaceId"));
            request.setSpaceId(contextSpaceId != null ? contextSpaceId
                    : originalExecution == null ? null : originalExecution.getSpaceId());
            request.setSpaceCode(StringUtils.defaultIfBlank(stringValue(context.get("spaceCode")),
                    originalExecution == null ? null : originalExecution.getSpaceCode()));
            request.setIdempotencyKey("workflow-compensation:" + runId + ":" + step.getId());
            request.setTriggerType("WORKFLOW_COMPENSATION");
            request.setTriggerRef("workflow:" + runId);
            request.setEventId("workflow:" + runId + ":compensation:" + step.getId());
            request.setWorkflowRunId(runId);
            request.setWorkflowStepId(step.getId());
            request.setParentExecutionId(step.getExecutionId());
            request.setCreatorId(SecurityUtils.getUserId());
            request.setCreateBy(SecurityUtils.getUsername());
            request.setMaxAttempts(1);
            request.setAutoExecute(false);
            try {
                result.getExecutions().add(executionService.submitExecution(request));
            } catch (Exception e) {
                result.getErrors().add("节点「" + step.getNodeKey() + "」补偿提交失败："
                        + StringUtils.defaultIfBlank(e.getMessage(), "未知错误"));
            }
        }
        result.setMessage(result.getExecutions().isEmpty()
                ? "没有成功提交补偿动作，请检查补偿配置"
                : "补偿动作已提交到人工审批/执行链，不会自动执行");
        refreshCompensationStatus(run);
        fillCompensationStatus(result, run);
        return result;
    }

    @Override
    public ActionWorkflowCompensationRespVO retryCompensation(Long runId) {
        ActionWorkflowRunDO run = requireRun(runId);
        ActionWorkflowDO workflow = requireWorkflowForRun(run);
        accessGuard.assertWorkflowAccess(workflow);
        if (!FAILED.equals(run.getStatus()) && !RECONCILIATION_REQUIRED.equals(run.getStatus())) {
            throw new ServiceException("只有失败或需人工对账的 Workflow 才能重试补偿：" + run.getStatus());
        }
        List<ActionExecutionDO> allCompensations = executionMapper.selectWorkflowCompensations(runId);
        Map<Long, ActionExecutionDO> latestByStep = new LinkedHashMap<>();
        for (ActionExecutionDO execution : allCompensations) {
            Long stepId = execution.getWorkflowStepId();
            if (stepId == null) stepId = execution.getId();
            latestByStep.put(stepId, execution);
        }
        List<ActionExecutionDO> retryable = latestByStep.values().stream()
                .filter(execution -> FAILED.equals(execution.getStatus()) || "REJECTED".equals(execution.getStatus()))
                .collect(Collectors.toList());
        ActionWorkflowCompensationRespVO result = new ActionWorkflowCompensationRespVO();
        result.setRunId(runId);
        result.setEligibleCount(retryable.size());
        result.setRetryableCount(retryable.size());
        refreshCompensationStatus(run);
        fillCompensationStatus(result, run);
        if (retryable.isEmpty()) {
            result.setMessage("没有可安全重试的失败补偿动作；需人工对账的补偿不会自动重放");
            return result;
        }
        for (ActionExecutionDO failedExecution : retryable) {
            ActionDO action = actionMapper.selectById(failedExecution.getActionId());
            if (action == null) {
                result.getErrors().add("补偿执行记录「" + failedExecution.getId() + "」对应动作不存在");
                continue;
            }
            if (!Objects.equals(workflow.getOntologyId(), action.getOntologyId())) {
                result.getErrors().add("补偿执行记录「" + failedExecution.getId() + "」的动作不属于当前本体");
                continue;
            }
            if (!Boolean.TRUE.equals(action.getNeedsApproval())
                    && (action.getApprovalLevels() == null || action.getApprovalLevels() <= 0)) {
                result.getErrors().add("补偿动作「" + action.getName() + "」必须配置人工审批");
                continue;
            }
            long attempt = allCompensations.stream()
                    .filter(item -> Objects.equals(item.getWorkflowStepId(), failedExecution.getWorkflowStepId()))
                    .count() + 1;
            ExecutionSubmitReqVO request = new ExecutionSubmitReqVO();
            request.setActionId(action.getId());
            request.setInputParams(normalizeJson(failedExecution.getInputParams()));
            request.setObjectKey(failedExecution.getObjectKey());
            request.setSpaceId(failedExecution.getSpaceId());
            request.setSpaceCode(failedExecution.getSpaceCode());
            request.setIdempotencyKey("workflow-compensation:" + runId + ":"
                    + failedExecution.getWorkflowStepId() + ":retry:" + attempt);
            request.setTriggerType("WORKFLOW_COMPENSATION");
            request.setTriggerRef("workflow:" + runId);
            request.setEventId("workflow:" + runId + ":compensation:retry:" + failedExecution.getId());
            request.setWorkflowRunId(runId);
            request.setWorkflowStepId(failedExecution.getWorkflowStepId());
            request.setParentExecutionId(failedExecution.getId());
            request.setCreatorId(SecurityUtils.getUserId());
            request.setCreateBy(SecurityUtils.getUsername());
            request.setMaxAttempts(1);
            request.setAutoExecute(false);
            try {
                result.getExecutions().add(executionService.submitExecution(request));
            } catch (Exception e) {
                result.getErrors().add("补偿执行记录「" + failedExecution.getId() + "」重试提交失败："
                        + StringUtils.defaultIfBlank(e.getMessage(), "未知错误"));
            }
        }
        result.setMessage(result.getExecutions().isEmpty()
                ? "没有成功提交补偿重试，请检查动作配置"
                : "失败补偿已重新提交到人工审批/执行链，不会自动执行");
        refreshCompensationStatus(run);
        fillCompensationStatus(result, run);
        return result;
    }

    @Override
    public List<ActionWorkflowRunRespVO> list(Long workflowId) {
        accessGuard.assertWorkflowAccess(requireWorkflow(workflowId));
        return runMapper.selectByWorkflowId(workflowId).stream().map(this::toResp).collect(Collectors.toList());
    }

    @Override
    public PageResult<ActionWorkflowRunRespVO> page(Long workflowId, ActionWorkflowRunPageReqVO reqVO) {
        accessGuard.assertWorkflowAccess(requireWorkflow(workflowId));
        ActionWorkflowRunPageReqVO query = reqVO == null ? new ActionWorkflowRunPageReqVO() : reqVO;
        if (StringUtils.isNotBlank(query.getStatus())) {
            String status = query.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!RUN_STATUSES.contains(status)) throw new ServiceException("不支持的 Workflow 运行状态：" + status);
            query.setStatus(status);
        }
        if (StringUtils.isNotBlank(query.getIdempotencyKey())) {
            String idempotencyKey = query.getIdempotencyKey().trim();
            if (idempotencyKey.length() > 256) throw new ServiceException("Workflow 幂等键不能超过 256 个字符");
            query.setIdempotencyKey(idempotencyKey);
        }
        PageResult<ActionWorkflowRunDO> page = runMapper.selectPageByWorkflowId(workflowId, query);
        List<ActionWorkflowRunRespVO> rows = page.getRows().stream()
                .map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    public ActionWorkflowMonitorRespVO monitor(Integer limit) {
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        // 取比展示条数更大的样本，避免无权空间的运行记录把当前用户的监控窗口挤掉。
        List<ActionWorkflowRunDO> candidates = runMapper.selectRecent(Math.min(1000, safeLimit * 10));
        ActionWorkflowMonitorRespVO result = new ActionWorkflowMonitorRespVO();
        result.setObservedAt(new Date());
        Map<String, Long> counts = new LinkedHashMap<>();
        List<ActionWorkflowRunRespVO> recent = new ArrayList<>();
        Set<Long> accessibleWorkflowIds = new HashSet<>();
        Set<Long> deniedWorkflowIds = new HashSet<>();
        int visible = 0;
        for (ActionWorkflowRunDO run : candidates) {
            if (run == null || run.getWorkflowId() == null) continue;
            Long workflowId = run.getWorkflowId();
            if (!accessibleWorkflowIds.contains(workflowId)) {
                if (deniedWorkflowIds.contains(workflowId)) continue;
                try {
                    accessGuard.assertWorkflowAccess(requireWorkflow(workflowId));
                    accessibleWorkflowIds.add(workflowId);
                } catch (RuntimeException deniedOrMissing) {
                    deniedWorkflowIds.add(workflowId);
                    continue;
                }
            }
            String status = StringUtils.defaultIfBlank(run.getStatus(), "UNKNOWN");
            counts.put(status, counts.getOrDefault(status, 0L) + 1L);
            visible++;
            if (recent.size() < safeLimit) recent.add(toResp(run));
        }
        result.setSampleSize(visible);
        result.setCounts(counts);
        result.setRecentRuns(recent);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionWorkflowRunRespVO cancel(Long runId) {
        ActionWorkflowRunDO run = requireRun(runId);
        accessGuard.assertWorkflowAccess(requireWorkflowForRun(run));
        if (!QUEUED.equals(run.getStatus()) && !RUNNING.equals(run.getStatus()) && !WAITING_APPROVAL.equals(run.getStatus())) {
            throw new ServiceException("当前运行状态不能取消：" + run.getStatus());
        }
        Date now = new Date();
        runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, runId)
                .in(ActionWorkflowRunDO::getStatus, QUEUED, RUNNING, WAITING_APPROVAL)
                .set(ActionWorkflowRunDO::getCancelRequested, 1)
                .set(ActionWorkflowRunDO::getStatus, CANCELED)
                .set(ActionWorkflowRunDO::getFinishedTime, now)
                .set(ActionWorkflowRunDO::getErrorMessage, "用户请求取消")
                .set(ActionWorkflowRunDO::getUpdaterId, SecurityUtils.getUserId())
                .set(ActionWorkflowRunDO::getUpdateBy, SecurityUtils.getUsername())
                .set(ActionWorkflowRunDO::getUpdateTime, now));
        return toResp(requireRun(runId));
    }

    private void executeLinear(ActionWorkflowRunDO run, ActionWorkflowDO workflow,
                               ActionWorkflowRunReqVO reqVO) {
        List<ActionWorkflowNodeDO> nodes = nodeMapper.selectByWorkflowId(workflow.getId());
        List<ActionWorkflowEdgeDO> edges = edgeMapper.selectByWorkflowId(workflow.getId());
        Map<String, ActionWorkflowNodeDO> byKey = nodes.stream()
                .collect(Collectors.toMap(ActionWorkflowNodeDO::getNodeKey, n -> n, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<ActionWorkflowEdgeDO>> outgoing = new HashMap<>();
        for (ActionWorkflowEdgeDO edge : edges) {
            outgoing.computeIfAbsent(edge.getFromNodeKey(), k -> new ArrayList<>()).add(edge);
        }
        ActionWorkflowNodeDO start = nodes.stream()
                .filter(n -> "START".equalsIgnoreCase(n.getNodeType()))
                .findFirst().orElseThrow(() -> new ServiceException("Workflow 缺少开始节点"));
        Map<String, Object> context = parseObject(run.getInputParams());
        Map<String, Object> persistedContext = parseObject(run.getOutputContext());
        if (!persistedContext.isEmpty()) context.putAll(persistedContext);
        if (reqVO != null && StringUtils.isNotBlank(reqVO.getObjectKey())) context.put("objectKey", reqVO.getObjectKey());
        if (reqVO != null && reqVO.getSpaceId() != null) context.put("spaceId", reqVO.getSpaceId());
        if (reqVO != null && StringUtils.isNotBlank(reqVO.getSpaceCode())) context.put("spaceCode", reqVO.getSpaceCode());
        if (reqVO != null && StringUtils.isNotBlank(reqVO.getEventId())) context.put("eventId", reqVO.getEventId());
        if (context.get("currentUser") == null) {
            try {
                context.put("currentUser", SecurityUtils.getUsername());
            } catch (Exception ignored) {
                context.put("currentUser", "workflow-worker");
            }
        }
        Set<String> visited = new HashSet<>();
        String currentKey = start.getNodeKey();
        while (true) {
            if (!visited.add(currentKey)) {
                throw new ServiceException("Workflow 运行路径存在环：" + currentKey);
            }
            ActionWorkflowNodeDO node = byKey.get(currentKey);
            if (node == null) throw new ServiceException("Workflow 引用了不存在的节点：" + currentKey);
            String type = node.getNodeType() == null ? "" : node.getNodeType().toUpperCase(Locale.ROOT);
            if ("END".equals(type)) {
                completeRun(run, workflow, context);
                return;
            }
            refreshRun(run, currentKey);
            if (isCancelRequested(run.getId())) {
                markRunCanceled(run, "运行已取消");
                return;
            }
            if ("ACTION".equals(type)) {
                executeActionNode(run, workflow, reqVO, node, context);
                syncRunStatus(run);
                if (WAITING_APPROVAL.equals(run.getStatus()) || FAILED.equals(run.getStatus())
                        || CANCELED.equals(run.getStatus()) || RECONCILIATION_REQUIRED.equals(run.getStatus())) return;
            } else if ("START".equals(type)) {
                // 开始节点只负责绑定触发动作，不重复执行该触发动作。
            } else if ("CONDITION".equals(type)) {
                ActionWorkflowEdgeDO selected = selectConditionEdge(node, outgoing.getOrDefault(currentKey, Collections.emptyList()),
                        context, reqVO, start);
                context.put("_condition:" + currentKey, selected.getEdgeKey());
                persistContext(run, context);
                currentKey = selected.getToNodeKey();
                continue;
            } else if ("PARALLEL_SPLIT".equals(type)) {
                String joinNodeKey = executeParallelBranches(run, workflow, reqVO, node, byKey, outgoing, context, start);
                if (WAITING_APPROVAL.equals(run.getStatus()) || FAILED.equals(run.getStatus())
                        || CANCELED.equals(run.getStatus()) || RECONCILIATION_REQUIRED.equals(run.getStatus())) return;
                if (StringUtils.isBlank(joinNodeKey)) {
                    throw new ServiceException("并行拆分未找到可汇聚的并行节点：" + currentKey);
                }
                currentKey = joinNodeKey;
                continue;
            } else if ("PARALLEL_JOIN".equals(type)) {
                if (!Boolean.TRUE.equals(context.get("_parallelJoin:" + currentKey))) {
                    throw new ServiceException("并行汇聚节点尚未完成全部分支：" + currentKey);
                }
            } else {
                throw new ServiceException("Workflow Runtime V2 暂不支持节点类型：" + type);
            }
            List<ActionWorkflowEdgeDO> next = outgoing.getOrDefault(currentKey, Collections.emptyList());
            if (next.size() != 1) throw new ServiceException("串行运行时要求节点只有一条出边：" + currentKey);
            currentKey = next.get(0).getToNodeKey();
        }
    }

    private void executeActionNode(ActionWorkflowRunDO run, ActionWorkflowDO workflow,
                                   ActionWorkflowRunReqVO reqVO, ActionWorkflowNodeDO node,
                                   Map<String, Object> context) {
        ActionWorkflowStepDO existingStep = stepMapper.selectByRunId(run.getId()).stream()
                .filter(item -> node.getNodeKey().equals(item.getNodeKey()))
                .reduce((first, second) -> second).orElse(null);
        if (existingStep != null && RECONCILIATION_REQUIRED.equals(existingStep.getStatus())) {
            run.setStatus(RECONCILIATION_REQUIRED);
            return;
        }
        if (existingStep != null && EXECUTED.equals(existingStep.getStatus())) {
            context.put(node.getNodeKey(), parseOutputValue(existingStep.getOutputJson()));
            if (existingStep.getExecutionId() != null) context.put("_lastExecutionId", existingStep.getExecutionId());
            return;
        }
        if (existingStep != null && existingStep.getExecutionId() != null) {
            ExecutionRespVO existingExecution = executionService.getExecutionById(existingStep.getExecutionId());
            if (existingExecution != null && "APPROVED".equals(existingExecution.getStatus())) {
                existingExecution = executionService.executeExecution(existingExecution.getId(), true);
            }
            if (existingExecution != null && EXECUTED.equals(existingExecution.getStatus())) {
                existingStep.setStatus(EXECUTED);
                existingStep.setOutputJson(existingExecution.getResultContext());
                existingStep.setErrorMessage(null);
                existingStep.setFinishedTime(new Date());
                stepMapper.updateById(existingStep);
                context.put(node.getNodeKey(), parseOutputValue(existingExecution.getResultContext()));
                context.put("_lastExecutionId", existingExecution.getId());
                return;
            }
            if (existingExecution != null && ("PENDING_APPROVAL".equals(existingExecution.getStatus()) || "PENDING".equals(existingExecution.getStatus()))) {
                markWaitingApproval(run, context);
                return;
            }
            if (existingExecution != null && ("REJECTED".equals(existingExecution.getStatus())
                    || "RECONCILIATION_REQUIRED".equals(existingExecution.getStatus()))) {
                boolean reconciliation = "RECONCILIATION_REQUIRED".equals(existingExecution.getStatus());
                existingStep.setStatus(reconciliation ? RECONCILIATION_REQUIRED : FAILED);
                existingStep.setErrorMessage(reconciliation
                        ? "动作执行结果需要人工对账，Workflow 不会自动重放"
                        : "动作审批被驳回，Workflow 不会自动重新提交");
                existingStep.setFinishedTime(new Date());
                stepMapper.updateById(existingStep);
                if (reconciliation) markRunReconciliationRequired(run, existingStep.getErrorMessage());
                else markRunFailed(run, existingStep.getErrorMessage());
                return;
            }
        }
        ActionWorkflowStepDO step = new ActionWorkflowStepDO();
        step.setRunId(run.getId());
        step.setNodeKey(node.getNodeKey());
        step.setNodeType(node.getNodeType());
        step.setActionId(node.getActionId());
        step.setStatus(RUNNING);
        String actionInputParams = buildActionInputParams(run.getInputParams(), context);
        step.setInputParams(actionInputParams);
        step.setStartedTime(new Date());
        step.setAttemptNo(existingStep != null && existingStep.getAttemptNo() != null
                ? existingStep.getAttemptNo() + 1 : 1);
        stepMapper.insert(step);
        try {
            ExecutionSubmitReqVO request = new ExecutionSubmitReqVO();
            request.setActionId(node.getActionId());
            request.setInputParams(actionInputParams);
            request.setObjectKey(context.get("objectKey") == null ? null : String.valueOf(context.get("objectKey")));
            request.setSpaceId(asLong(context.get("spaceId")));
            request.setSpaceCode(context.get("spaceCode") == null ? null : String.valueOf(context.get("spaceCode")));
            request.setEventId(context.get("eventId") == null ? null : String.valueOf(context.get("eventId")));
            request.setWorkflowRunId(run.getId());
            request.setWorkflowStepId(step.getId());
            request.setParentExecutionId(asLong(context.get("_lastExecutionId")));
            request.setCreatorId(run.getCreatorId());
            request.setCreateBy(run.getCreateBy());
            request.setTriggerType("WORKFLOW");
            request.setTriggerRef(workflow.getTriggerRef());
            request.setMaxAttempts(1);
            request.setAutoExecute(true);
            String idempotencyKey = "workflow:" + run.getId() + ":" + node.getNodeKey();
            if (existingStep != null && FAILED.equals(existingStep.getStatus())) {
                idempotencyKey += ":retry:" + System.currentTimeMillis();
            }
            request.setIdempotencyKey(idempotencyKey);
            ExecutionRespVO submitted = executionService.submitExecution(request);
            step.setExecutionId(submitted.getId());
            syncRunStatus(run);
            if (RECONCILIATION_REQUIRED.equals(run.getStatus())) {
                executionMapper.markReconciliationRequired(submitted.getId(),
                        "Workflow 节点在动作提交期间超时，已转人工对账");
                step.setStatus(RECONCILIATION_REQUIRED);
                step.setErrorMessage("Workflow 节点在动作提交期间超时，已转人工对账");
                step.setFinishedTime(new Date());
                stepMapper.updateById(step);
                return;
            }
            if ("PENDING_APPROVAL".equals(submitted.getStatus()) || "PENDING".equals(submitted.getStatus())) {
                step.setStatus(WAITING_APPROVAL);
                step.setOutputJson(submitted.getPreviewResult());
                step.setFinishedTime(new Date());
                stepMapper.updateById(step);
                markWaitingApproval(run, context);
                return;
            }
            ExecutionRespVO executed = "APPROVED".equals(submitted.getStatus())
                    ? executionService.executeExecution(submitted.getId(), true) : submitted;
            if (RECONCILIATION_REQUIRED.equals(executed.getStatus())) {
                step.setStatus(RECONCILIATION_REQUIRED);
                step.setOutputJson(executed.getResultContext() != null
                        ? executed.getResultContext() : executed.getPreviewResult());
                step.setErrorMessage(StringUtils.defaultIfBlank(executed.getErrorMessage(),
                        "动作执行结果需要人工对账，Workflow 不会自动重放"));
                step.setFinishedTime(new Date());
                stepMapper.updateById(step);
                markRunReconciliationRequired(run, step.getErrorMessage());
                return;
            }
            step.setOutputJson(executed.getResultContext() != null
                    ? executed.getResultContext() : executed.getPreviewResult());
            step.setStatus(EXECUTED.equals(executed.getStatus()) ? EXECUTED : FAILED);
            step.setErrorMessage(executed.getErrorMessage());
            step.setFinishedTime(new Date());
            stepMapper.updateById(step);
            if (!EXECUTED.equals(executed.getStatus())) {
                retryOrFail(run, workflow, reqVO, node, context,
                        StringUtils.defaultIfBlank(executed.getErrorMessage(), "动作节点执行失败"),
                        step.getAttemptNo());
                return;
            }
            context.put(node.getNodeKey(), parseOutputValue(executed.getResultContext() != null
                    ? executed.getResultContext() : executed.getPreviewResult()));
            context.put("_lastExecutionId", executed.getId());
            persistContext(run, context);
        } catch (Exception e) {
            ActionWorkflowStepDO persistedStep = stepMapper.selectById(step.getId());
            if (persistedStep != null && RECONCILIATION_REQUIRED.equals(persistedStep.getStatus())) {
                run.setStatus(RECONCILIATION_REQUIRED);
                return;
            }
            step.setStatus(FAILED);
            step.setErrorMessage(e.getMessage());
            step.setFinishedTime(new Date());
            stepMapper.updateById(step);
            retryOrFail(run, workflow, reqVO, node, context, e.getMessage(), step.getAttemptNo());
        }
    }

    /**
     * 节点失败后的重试入口。retryPolicy 采用 JSON：
     * {"maxAttempts":3,"backoffMs":1000}，maxAttempts 表示包含首次执行在内的总尝试次数。
     */
    private void retryOrFail(ActionWorkflowRunDO run, ActionWorkflowDO workflow,
                             ActionWorkflowRunReqVO reqVO, ActionWorkflowNodeDO node,
                             Map<String, Object> context, String message, Integer attemptNo) {
        ActionWorkflowRunDO currentRun = runMapper.selectById(run.getId());
        if (currentRun != null && RECONCILIATION_REQUIRED.equals(currentRun.getStatus())) {
            return;
        }
        RetryPolicy policy = parseRetryPolicy(node.getRetryPolicy());
        if (policy.maxAttempts > 1 && isNonIdempotentWebhook(node.getActionId())) {
            markRunFailed(run, "Webhook 动作未声明幂等性，禁止自动重试；请将 retryPolicy.maxAttempts 设为 1 或配置 idempotent=true");
            return;
        }
        int currentAttempt = attemptNo == null || attemptNo < 1 ? 1 : attemptNo;
        if (currentAttempt < policy.maxAttempts && !isCancelRequested(run.getId())) {
            long delay = retryDelay(policy.backoffMs, currentAttempt);
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    markRunFailed(run, "动作节点重试被中断");
                    return;
                }
            }
            executeActionNode(run, workflow, reqVO, node, context);
            return;
        }
        if (isCancelRequested(run.getId())) {
            markRunCanceled(run, "运行已取消");
        } else {
            String suffix = policy.maxAttempts > 1
                    ? "（已达到最大尝试次数 " + policy.maxAttempts + "）" : "";
            markRunFailed(run, StringUtils.defaultIfBlank(message, "动作节点执行失败") + suffix);
        }
    }

    private RetryPolicy parseRetryPolicy(String value) {
        if (StringUtils.isBlank(value)) return new RetryPolicy(1, 0L);
        try {
            JSONObject json = JSON.parseObject(value);
            Integer maxAttempts = json.getInteger("maxAttempts");
            if (maxAttempts == null) {
                Integer maxRetries = json.getInteger("maxRetries");
                maxAttempts = maxRetries == null ? 1 : maxRetries + 1;
            }
            Long backoffMs = json.getLong("backoffMs");
            int attempts = Math.max(1, Math.min(maxAttempts, 10));
            long backoff = backoffMs == null ? 0L : Math.max(0L, Math.min(backoffMs, 300000L));
            return new RetryPolicy(attempts, backoff);
        } catch (Exception ignored) {
            return new RetryPolicy(1, 0L);
        }
    }

    private long retryDelay(long backoffMs, int attemptNo) {
        if (backoffMs <= 0) return 0L;
        long multiplier = 1L << Math.min(Math.max(attemptNo - 1, 0), 8);
        return Math.min(backoffMs * multiplier, 300000L);
    }

    /** 外部副作用动作默认不可重放，只有动作配置显式声明幂等才允许 Workflow 自动重试。 */
    private boolean isNonIdempotentWebhook(Long actionId) {
        if (actionId == null) return false;
        ActionDO action = actionMapper.selectById(actionId);
        if (action == null || !"WEBHOOK".equalsIgnoreCase(action.getActionType())) return false;
        try {
            JSONObject config = JSONObject.parseObject(action.getExecutionSteps());
            return config == null || !Boolean.TRUE.equals(config.getBoolean("idempotent"));
        } catch (Exception e) {
            // 配置错误由动作提交阶段拦截；这里按不幂等处理，避免错误配置进入自动重试。
            return true;
        }
    }

    private static final class RetryPolicy {
        private final int maxAttempts;
        private final long backoffMs;

        private RetryPolicy(int maxAttempts, long backoffMs) {
            this.maxAttempts = maxAttempts;
            this.backoffMs = backoffMs;
        }
    }

    /**
     * 执行一个并行拆分节点的所有分支，并要求所有分支最终汇聚到同一个 PARALLEL_JOIN。
     * 当前实现按分支逐个推进，但每个分支均独立持久化步骤和输出，具备可恢复的并行语义；
     * 这样可以避免在动作执行已经产生副作用后，用线程中断伪造“硬并行”而导致重复执行风险。
     */
    private String executeParallelBranches(ActionWorkflowRunDO run, ActionWorkflowDO workflow,
                                           ActionWorkflowRunReqVO reqVO, ActionWorkflowNodeDO split,
                                           Map<String, ActionWorkflowNodeDO> byKey,
                                           Map<String, List<ActionWorkflowEdgeDO>> outgoing,
                                           Map<String, Object> context,
                                           ActionWorkflowNodeDO startNode) {
        List<ActionWorkflowEdgeDO> branches = outgoing.getOrDefault(split.getNodeKey(), Collections.emptyList());
        if (branches.size() < 2) {
            throw new ServiceException("并行拆分节点至少需要两条分支：" + split.getNodeKey());
        }
        Map<String, Object> splitContext = new LinkedHashMap<>(context);
        Object parentExecutionId = splitContext.get("_lastExecutionId");
        String joinNodeKey = null;
        for (ActionWorkflowEdgeDO branch : branches) {
            // 每个分支都从拆分节点快照启动，不能看到其它分支尚未汇聚的输出。
            Map<String, Object> branchContext = new LinkedHashMap<>(splitContext);
            // 各分支继承拆分前的父执行，但不能把前一个分支的末尾动作串成后一个分支的父级。
            if (parentExecutionId == null) branchContext.remove("_lastExecutionId");
            else branchContext.put("_lastExecutionId", parentExecutionId);
            String branchJoin = executeBranchUntilJoin(run, workflow, reqVO, branch.getToNodeKey(),
                    byKey, outgoing, branchContext, startNode, new HashSet<>());
            if (WAITING_APPROVAL.equals(run.getStatus()) || FAILED.equals(run.getStatus())
                    || CANCELED.equals(run.getStatus()) || RECONCILIATION_REQUIRED.equals(run.getStatus())) {
                branchContext.remove("_lastExecutionId");
                if (parentExecutionId == null) branchContext.remove("_lastExecutionId");
                else branchContext.put("_lastExecutionId", parentExecutionId);
                persistContext(run, branchContext);
                return null;
            }
            if (StringUtils.isBlank(branchJoin)) {
                throw new ServiceException("并行分支未到达汇聚节点：" + branch.getToNodeKey());
            }
            if (joinNodeKey == null) {
                joinNodeKey = branchJoin;
            } else if (!joinNodeKey.equals(branchJoin)) {
                throw new ServiceException("并行拆分的分支必须汇聚到同一个 PARALLEL_JOIN："
                        + split.getNodeKey());
            }
            branchContext.remove("_lastExecutionId");
            context.putAll(branchContext);
            if (parentExecutionId == null) context.remove("_lastExecutionId");
            else context.put("_lastExecutionId", parentExecutionId);
            persistContext(run, context);
        }
        context.put("_parallelJoin:" + joinNodeKey, Boolean.TRUE);
        persistContext(run, context);
        return joinNodeKey;
    }

    /** 执行单个分支，遇到 PARALLEL_JOIN 时返回汇聚节点编码。 */
    private String executeBranchUntilJoin(ActionWorkflowRunDO run, ActionWorkflowDO workflow,
                                          ActionWorkflowRunReqVO reqVO, String currentKey,
                                          Map<String, ActionWorkflowNodeDO> byKey,
                                          Map<String, List<ActionWorkflowEdgeDO>> outgoing,
                                          Map<String, Object> context,
                                          ActionWorkflowNodeDO startNode,
                                          Set<String> visited) {
        while (true) {
            if (!visited.add(currentKey)) {
                throw new ServiceException("并行分支路径存在环：" + currentKey);
            }
            ActionWorkflowNodeDO node = byKey.get(currentKey);
            if (node == null) throw new ServiceException("Workflow 引用了不存在的节点：" + currentKey);
            String type = node.getNodeType() == null ? "" : node.getNodeType().toUpperCase(Locale.ROOT);
            if ("PARALLEL_JOIN".equals(type)) return currentKey;
            if ("END".equals(type)) {
                throw new ServiceException("并行分支在汇聚前到达结束节点：" + currentKey);
            }
            refreshRun(run, currentKey);
            if (isCancelRequested(run.getId())) {
                markRunCanceled(run, "运行已取消");
                return null;
            }
            if ("ACTION".equals(type)) {
                executeActionNode(run, workflow, reqVO, node, context);
                syncRunStatus(run);
                if (WAITING_APPROVAL.equals(run.getStatus()) || FAILED.equals(run.getStatus())
                        || CANCELED.equals(run.getStatus()) || RECONCILIATION_REQUIRED.equals(run.getStatus())) return null;
            } else if ("CONDITION".equals(type)) {
                ActionWorkflowEdgeDO selected = selectConditionEdge(node,
                        outgoing.getOrDefault(currentKey, Collections.emptyList()), context, reqVO, startNode);
                context.put("_condition:" + currentKey, selected.getEdgeKey());
                persistContext(run, context);
                currentKey = selected.getToNodeKey();
                continue;
            } else if ("PARALLEL_SPLIT".equals(type)) {
                String nestedJoin = executeParallelBranches(run, workflow, reqVO, node, byKey, outgoing,
                        context, startNode);
                if (WAITING_APPROVAL.equals(run.getStatus()) || FAILED.equals(run.getStatus())
                        || CANCELED.equals(run.getStatus()) || RECONCILIATION_REQUIRED.equals(run.getStatus())) return null;
                if (StringUtils.isBlank(nestedJoin)) {
                    throw new ServiceException("嵌套并行拆分未找到汇聚节点：" + currentKey);
                }
                currentKey = nestedJoin;
                continue;
            } else if (!"START".equals(type)) {
                throw new ServiceException("并行分支暂不支持节点类型：" + type);
            }
            List<ActionWorkflowEdgeDO> next = outgoing.getOrDefault(currentKey, Collections.emptyList());
            if (next.size() != 1) {
                throw new ServiceException("并行分支中的节点必须只有一条出边：" + currentKey);
            }
            currentKey = next.get(0).getToNodeKey();
        }
    }

    private void persistContext(ActionWorkflowRunDO run, Map<String, Object> context) {
        Map<String, Object> persisted = parseObject(run.getOutputContext());
        persisted.putAll(context);
        runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .set(ActionWorkflowRunDO::getOutputContext, JSON.toJSONString(persisted))
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }

    private void markWaitingApproval(ActionWorkflowRunDO run, Map<String, Object> context) {
        Map<String, Object> persisted = parseObject(run.getOutputContext());
        persisted.putAll(context);
        run.setStatus(WAITING_APPROVAL);
        run.setOutputContext(JSON.toJSONString(persisted));
        runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .in(ActionWorkflowRunDO::getStatus, RUNNING, QUEUED)
                .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                .set(ActionWorkflowRunDO::getStatus, WAITING_APPROVAL)
                .set(ActionWorkflowRunDO::getOutputContext, JSON.toJSONString(persisted))
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }

    private void completeRun(ActionWorkflowRunDO run, ActionWorkflowDO workflow,
                             Map<String, Object> context) {
        Map<String, Object> persisted = parseObject(run.getOutputContext());
        persisted.putAll(context);
        validateWorkflowOutput(workflow, persisted);
        Date now = new Date();
        run.setStatus(EXECUTED);
        run.setCurrentNodeKey(null);
        run.setOutputContext(JSON.toJSONString(persisted));
        run.setFinishedTime(now);
        runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .in(ActionWorkflowRunDO::getStatus, RUNNING, QUEUED)
                .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                .set(ActionWorkflowRunDO::getStatus, EXECUTED)
                .set(ActionWorkflowRunDO::getCurrentNodeKey, null)
                .set(ActionWorkflowRunDO::getOutputContext, JSON.toJSONString(persisted))
                .set(ActionWorkflowRunDO::getFinishedTime, now)
                .set(ActionWorkflowRunDO::getUpdateTime, now));
    }

    /**
     * 动作节点输入由用户初始输入和当前运行上下文合并而成。
     * 以下划线开头的内部游标只用于运行时恢复，不暴露给动作参数解析器。
     */
    private String buildActionInputParams(String rawInput, Map<String, Object> context) {
        Map<String, Object> input = parseObject(rawInput);
        if (context != null) {
            context.forEach((key, value) -> {
                if (key != null && !key.startsWith("_")) input.put(key, value);
            });
        }
        return JSON.toJSONString(input);
    }

    private List<ActionWorkflowNodeDO> linearActionPath(List<ActionWorkflowNodeDO> nodes,
                                                         List<ActionWorkflowEdgeDO> edges) {
        Map<String, ActionWorkflowNodeDO> byKey = nodes.stream()
                .collect(Collectors.toMap(ActionWorkflowNodeDO::getNodeKey, n -> n, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<ActionWorkflowEdgeDO>> outgoing = new HashMap<>();
        for (ActionWorkflowEdgeDO edge : edges) {
            outgoing.computeIfAbsent(edge.getFromNodeKey(), k -> new ArrayList<>()).add(edge);
        }
        ActionWorkflowNodeDO start = nodes.stream().filter(n -> "START".equalsIgnoreCase(n.getNodeType())).findFirst()
                .orElseThrow(() -> new ServiceException("Workflow 缺少开始节点"));
        List<ActionWorkflowNodeDO> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String key = start.getNodeKey();
        while (true) {
            if (!visited.add(key)) throw new ServiceException("Workflow 运行路径存在环：" + key);
            ActionWorkflowNodeDO node = byKey.get(key);
            if (node == null) throw new ServiceException("Workflow 引用了不存在的节点：" + key);
            String type = node.getNodeType() == null ? "" : node.getNodeType().toUpperCase(Locale.ROOT);
            if ("END".equals(type)) break;
            if (!"START".equals(type) && !"ACTION".equals(type)) {
                throw new ServiceException("Workflow Runtime V1 暂不支持节点类型：" + type);
            }
            if ("ACTION".equals(type)) {
                if (node.getActionId() == null) throw new ServiceException("动作节点未绑定动作：" + key);
                result.add(node);
            }
            List<ActionWorkflowEdgeDO> next = outgoing.getOrDefault(key, Collections.emptyList());
            if (next.size() != 1) throw new ServiceException("串行运行时要求节点只有一条出边：" + key);
            key = next.get(0).getToNodeKey();
        }
        if (result.isEmpty()) throw new ServiceException("Workflow 至少需要一个动作节点");
        return result;
    }

    private ActionWorkflowEdgeDO selectConditionEdge(ActionWorkflowNodeDO conditionNode,
                                                     List<ActionWorkflowEdgeDO> branches,
                                                     Map<String, Object> context,
                                                     ActionWorkflowRunReqVO reqVO,
                                                     ActionWorkflowNodeDO startNode) {
        if (branches == null || branches.size() < 2) {
            throw new ServiceException("条件节点至少需要两条分支：" + conditionNode.getNodeKey());
        }
        Object recordedEdge = context.get("_condition:" + conditionNode.getNodeKey());
        if (recordedEdge != null) {
            for (ActionWorkflowEdgeDO edge : branches) {
                if (String.valueOf(recordedEdge).equals(edge.getEdgeKey())) return edge;
            }
            throw new ServiceException("已记录的条件分支不存在：" + recordedEdge);
        }
        ActionDO triggerAction = actionMapper.selectById(startNode.getActionId());
        if (triggerAction != null && triggerAction.getConceptId() == null
                && triggerAction.getSourceConceptId() != null) {
            // FUNCTION/复合触发动作可能只保存 sourceConceptId；对象条件仍应按触发对象概念回查。
            triggerAction.setConceptId(triggerAction.getSourceConceptId());
        }
        ActionWorkflowEdgeDO defaultEdge = null;
        for (ActionWorkflowEdgeDO edge : branches) {
            if (StringUtils.isBlank(edge.getConditionExpr())) {
                if (defaultEdge == null) defaultEdge = edge;
                continue;
            }
            if (evaluateCondition(edge.getConditionExpr(), context, reqVO, triggerAction)) {
                return edge;
            }
        }
        if (defaultEdge != null) return defaultEdge;
        throw new ServiceException("条件节点没有匹配的分支：" + conditionNode.getNodeKey());
    }

    private boolean evaluateCondition(String expressionText, Map<String, Object> context,
                                      ActionWorkflowRunReqVO reqVO, ActionDO triggerAction) {
        final JSONObject expression;
        try {
            expression = JSON.parseObject(expressionText);
        } catch (Exception e) {
            throw new ServiceException("条件表达式不是合法 JSON");
        }
        if (expression == null || !"COMPARE".equalsIgnoreCase(expression.getString("type"))) {
            throw new ServiceException("暂不支持的条件类型");
        }
        String operator = StringUtils.defaultIfBlank(expression.getString("operator"), "EQ").toUpperCase(Locale.ROOT);
        Object left = resolveConditionReference(expression.getJSONObject("left"), context, reqVO, triggerAction);
        if ("IS_EMPTY".equals(operator)) return isEmptyValue(left);
        if ("NOT_EMPTY".equals(operator)) return !isEmptyValue(left);
        JSONObject rightRef = expression.getJSONObject("right");
        if (rightRef == null) throw new ServiceException("条件右值不能为空");
        Object right = "REFERENCE".equalsIgnoreCase(rightRef.getString("mode"))
                ? resolveConditionReference(rightRef, context, reqVO, triggerAction)
                : rightRef.get("value");
        return compareConditionValues(left, right, operator);
    }

    private Object resolveConditionReference(JSONObject reference, Map<String, Object> context,
                                             ActionWorkflowRunReqVO reqVO, ActionDO triggerAction) {
        if (reference == null) throw new ServiceException("条件引用不能为空");
        String source = reference.getString("source");
        String path = reference.getString("path");
        if (StringUtils.isBlank(source) || StringUtils.isBlank(path)) {
            throw new ServiceException("条件引用必须包含来源和字段路径");
        }
        if ("OBJECT".equalsIgnoreCase(source)) {
            String objectKey = reqVO != null && StringUtils.isNotBlank(reqVO.getObjectKey())
                    ? reqVO.getObjectKey()
                    : (context.get("objectKey") == null ? null : String.valueOf(context.get("objectKey")));
            return objectStateConditionResolver.resolve("object." + path, triggerAction, objectKey);
        }
        if ("INPUT".equalsIgnoreCase(source)) {
            return readPath(context, path);
        }
        if ("STEP_OUTPUT".equalsIgnoreCase(source)) {
            String nodeKey = reference.getString("nodeKey");
            if (StringUtils.isBlank(nodeKey)) throw new ServiceException("步骤输出引用缺少 nodeKey");
            Object stepOutput = context.get(nodeKey);
            return readPath(stepOutput, path);
        }
        if ("CONTEXT".equalsIgnoreCase(source)) {
            if ("now".equals(path)) return new Date();
            return context.get(path);
        }
        throw new ServiceException("不支持的条件引用来源：" + source);
    }

    private Object readPath(Object value, String path) {
        if (value == null || StringUtils.isBlank(path)) return value;
        Object current = value;
        for (String segment : path.split("\\.")) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(segment);
            } else if (current instanceof List && segment.matches("\\d+")) {
                int index = Integer.parseInt(segment);
                List<?> list = (List<?>) current;
                current = index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
        }
        return current;
    }

    private boolean compareConditionValues(Object left, Object right, String operator) {
        if ("IN".equals(operator)) {
            if (right instanceof Collection) {
                for (Object candidate : (Collection<?>) right) {
                    if (compareConditionValues(left, candidate, "EQ")) return true;
                }
                return false;
            }
            if (right != null && right.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(right);
                for (int i = 0; i < length; i++) {
                    if (compareConditionValues(left, java.lang.reflect.Array.get(right, i), "EQ")) return true;
                }
                return false;
            }
            if (right instanceof String) {
                for (String candidate : ((String) right).split(",")) {
                    if (compareConditionValues(left, candidate.trim(), "EQ")) return true;
                }
            }
            return false;
        }
        if ("CONTAINS".equals(operator)) {
            if (left instanceof Collection) return ((Collection<?>) left).contains(right);
            return left != null && right != null && String.valueOf(left).contains(String.valueOf(right));
        }
        if ("EQ".equals(operator) || "NE".equals(operator)) {
            boolean equals;
            BigDecimal leftNumber = decimal(left);
            BigDecimal rightNumber = decimal(right);
            if (leftNumber != null && rightNumber != null) {
                equals = leftNumber.compareTo(rightNumber) == 0;
            } else if (left == null || right == null) {
                equals = left == right;
            } else {
                equals = String.valueOf(left).equals(String.valueOf(right));
            }
            return "NE".equals(operator) ? !equals : equals;
        }
        BigDecimal leftNumber = decimal(left);
        BigDecimal rightNumber = decimal(right);
        int comparison;
        if (leftNumber != null && rightNumber != null) {
            comparison = leftNumber.compareTo(rightNumber);
        } else if (left == null || right == null) {
            return false;
        } else {
            comparison = String.valueOf(left).compareTo(String.valueOf(right));
        }
        switch (operator) {
            case "GT": return comparison > 0;
            case "GE": return comparison >= 0;
            case "LT": return comparison < 0;
            case "LE": return comparison <= 0;
            default: throw new ServiceException("不支持的条件操作符：" + operator);
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null || value instanceof Boolean || value instanceof Date) return null;
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) return true;
        if (value instanceof CharSequence) return value.toString().trim().isEmpty();
        if (value instanceof Collection) return ((Collection<?>) value).isEmpty();
        if (value instanceof Map) return ((Map<?, ?>) value).isEmpty();
        return false;
    }

    private Object parseOutputValue(String output) {
        if (StringUtils.isBlank(output)) return null;
        try {
            return JSON.parse(output);
        } catch (Exception ignored) {
            return output;
        }
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void refreshRun(ActionWorkflowRunDO run, String nodeKey) {
        runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .in(ActionWorkflowRunDO::getStatus, RUNNING, QUEUED)
                .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                .set(ActionWorkflowRunDO::getCurrentNodeKey, nodeKey)
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }

    private void markRunCanceled(ActionWorkflowRunDO run, String message) {
        Date now = new Date();
        run.setStatus(CANCELED);
        run.setCancelRequested(1);
        run.setErrorMessage(message);
        run.setFinishedTime(now);
        runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .in(ActionWorkflowRunDO::getStatus, QUEUED, RUNNING, WAITING_APPROVAL)
                .set(ActionWorkflowRunDO::getCancelRequested, 1)
                .set(ActionWorkflowRunDO::getStatus, CANCELED)
                .set(ActionWorkflowRunDO::getErrorMessage, message)
                .set(ActionWorkflowRunDO::getFinishedTime, now)
                .set(ActionWorkflowRunDO::getUpdateTime, now));
    }

    private Map<String, Object> buildInitialContext(ActionWorkflowRunReqVO reqVO) {
        Map<String, Object> context = parseObject(reqVO == null ? null : reqVO.getInputParams());
        if (reqVO != null && StringUtils.isNotBlank(reqVO.getObjectKey())) context.put("objectKey", reqVO.getObjectKey());
        if (reqVO != null && reqVO.getSpaceId() != null) context.put("spaceId", reqVO.getSpaceId());
        if (reqVO != null && StringUtils.isNotBlank(reqVO.getSpaceCode())) context.put("spaceCode", reqVO.getSpaceCode());
        if (reqVO != null && StringUtils.isNotBlank(reqVO.getEventId())) context.put("eventId", reqVO.getEventId());
        try {
            context.put("currentUser", SecurityUtils.getUsername());
        } catch (Exception ignored) {
            // 异步调用必须允许由外部调度器提交，运行时会使用 worker 标识兜底。
        }
        return context;
    }

    /** 对 Workflow 的 inputSchema 执行安全的 JSON Schema 子集校验。 */
    private void validateWorkflowInput(ActionWorkflowDO workflow, String inputParams) {
        if (workflow == null || StringUtils.isBlank(workflow.getInputSchema())) return;
        final JSONObject schema;
        try {
            schema = JSON.parseObject(workflow.getInputSchema());
        } catch (Exception e) {
            throw new ServiceException("Workflow 输入 Schema 不是合法 JSON");
        }
        if (schema == null) throw new ServiceException("Workflow 输入 Schema 不能为空对象");
        Map<String, Object> input = parseObject(inputParams);
        validateSchemaValue(input, schema, "$", "输入");
    }

    private void validateWorkflowOutput(ActionWorkflowDO workflow, Map<String, Object> output) {
        if (workflow == null || StringUtils.isBlank(workflow.getOutputSchema())) return;
        final JSONObject schema;
        try {
            schema = JSON.parseObject(workflow.getOutputSchema());
        } catch (Exception e) {
            throw new ServiceException("Workflow 输出 Schema 不是合法 JSON");
        }
        if (schema == null) throw new ServiceException("Workflow 输出 Schema 不能为空对象");
        Map<String, Object> businessOutput = output == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(output);
        // 运行时游标和触发元数据不属于业务输出，不能因为 additionalProperties=false 使合法结果失败。
        businessOutput.keySet().removeIf(key -> key == null || key.startsWith("_")
                || "objectKey".equals(key) || "spaceId".equals(key) || "spaceCode".equals(key)
                || "eventId".equals(key) || "currentUser".equals(key)
                || "triggerType".equals(key) || "triggerRef".equals(key));
        validateSchemaValue(businessOutput, schema, "$", "输出");
    }

    @SuppressWarnings("unchecked")
    private void validateSchemaValue(Object value, JSONObject schema, String path, String label) {
        Object type = schema.get("type");
        if (type != null && !matchesSchemaType(value, type)) {
            throw new ServiceException("Workflow " + label + "字段 " + path + " 类型不符合 Schema");
        }
        Object enumValue = schema.get("enum");
        if (enumValue instanceof Collection && !((Collection<Object>) enumValue).contains(value)) {
            throw new ServiceException("Workflow " + label + "字段 " + path + " 不在允许值范围内");
        }
        Object items = schema.get("items");
        if (value instanceof Collection && items instanceof JSONObject) {
            int index = 0;
            for (Object item : (Collection<?>) value) {
                validateSchemaValue(item, (JSONObject) items, path + "[" + index + "]", label);
                index++;
            }
        }
        if (!(value instanceof Map)) return;
        Map<String, Object> object = (Map<String, Object>) value;
        Object required = schema.get("required");
        if (required instanceof Collection) {
            for (Object field : (Collection<Object>) required) {
                String name = String.valueOf(field);
                if (!object.containsKey(name) || object.get(name) == null) {
                    throw new ServiceException("Workflow " + label + "缺少必填字段：" + name);
                }
            }
        }
        Object properties = schema.get("properties");
        if (!(properties instanceof Map)) return;
        Map<String, Object> propertySchemas = (Map<String, Object>) properties;
        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            for (String key : object.keySet()) {
                if (!propertySchemas.containsKey(key)) {
                    throw new ServiceException("Workflow " + label + "包含未定义字段：" + key);
                }
            }
        }
        for (Map.Entry<String, Object> entry : propertySchemas.entrySet()) {
            if (!object.containsKey(entry.getKey()) || !(entry.getValue() instanceof JSONObject)) continue;
            validateSchemaValue(object.get(entry.getKey()), (JSONObject) entry.getValue(),
                    path + "." + entry.getKey(), label);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean matchesSchemaType(Object value, Object type) {
        if (type instanceof Collection) {
            for (Object item : (Collection<Object>) type) if (matchesSchemaType(value, item)) return true;
            return false;
        }
        String name = String.valueOf(type);
        if ("null".equals(name)) return value == null;
        if (value == null) return false;
        if ("object".equals(name)) return value instanceof Map;
        if ("array".equals(name)) return value instanceof Collection;
        if ("string".equals(name)) return value instanceof CharSequence;
        if ("boolean".equals(name)) return value instanceof Boolean;
        if ("number".equals(name)) return value instanceof Number;
        if ("integer".equals(name)) {
            if (!(value instanceof Number)) return false;
            return new BigDecimal(String.valueOf(value)).stripTrailingZeros().scale() <= 0;
        }
        return true;
    }

    private boolean isCancelRequested(Long runId) {
        ActionWorkflowRunDO current = runMapper.selectById(runId);
        return current != null && Integer.valueOf(1).equals(current.getCancelRequested());
    }

    private void syncRunStatus(ActionWorkflowRunDO run) {
        if (run == null || run.getId() == null) return;
        ActionWorkflowRunDO latest = runMapper.selectById(run.getId());
        if (latest != null) {
            run.setStatus(latest.getStatus());
            run.setCancelRequested(latest.getCancelRequested());
            run.setErrorMessage(latest.getErrorMessage());
        }
    }

    private void markRunFailed(ActionWorkflowRunDO run, String message) {
        Date now = new Date();
        run.setStatus(FAILED);
        run.setErrorMessage(StringUtils.defaultIfBlank(message, "Workflow 执行失败"));
        run.setFinishedTime(now);
        int updated = runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .in(ActionWorkflowRunDO::getStatus, QUEUED, RUNNING, WAITING_APPROVAL)
                .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                .set(ActionWorkflowRunDO::getStatus, FAILED)
                .set(ActionWorkflowRunDO::getErrorMessage,
                        StringUtils.defaultIfBlank(message, "Workflow 执行失败"))
                .set(ActionWorkflowRunDO::getFinishedTime, now)
                .set(ActionWorkflowRunDO::getUpdateTime, now));
        if (updated > 0) {
            workflowAlertService.notifyTerminal(run, FAILED, run.getErrorMessage());
        }
    }

    private void markRunReconciliationRequired(ActionWorkflowRunDO run, String message) {
        Date now = new Date();
        run.setStatus(RECONCILIATION_REQUIRED);
        run.setErrorMessage(StringUtils.defaultIfBlank(message, "Workflow 需要人工对账"));
        run.setFinishedTime(now);
        int updated = runMapper.update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .in(ActionWorkflowRunDO::getStatus, QUEUED, RUNNING, WAITING_APPROVAL)
                .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                .set(ActionWorkflowRunDO::getStatus, RECONCILIATION_REQUIRED)
                .set(ActionWorkflowRunDO::getErrorMessage, run.getErrorMessage())
                .set(ActionWorkflowRunDO::getFinishedTime, now)
                .set(ActionWorkflowRunDO::getUpdateTime, now));
        if (updated > 0) {
            workflowAlertService.notifyTerminal(run, RECONCILIATION_REQUIRED, run.getErrorMessage());
        }
    }

    private ActionWorkflowDO requirePublishedWorkflow(Long id) {
        if (id == null) throw new ServiceException("Workflow ID 不能为空");
        ActionWorkflowDO workflow = workflowMapper.selectById(id);
        if (workflow == null) throw new ServiceException("Workflow 不存在：" + id);
        if (!"PUBLISHED".equals(workflow.getStatus()) || !Boolean.TRUE.equals(workflow.getEnabled())) {
            throw new ServiceException("只有已发布且启用的 Workflow 才能运行");
        }
        return workflow;
    }

    private ActionWorkflowDO requireWorkflowForRun(ActionWorkflowRunDO run) {
        if (run == null || run.getWorkflowId() == null) {
            throw new ServiceException("Workflow 运行未关联有效编排");
        }
        return requireWorkflow(run.getWorkflowId());
    }

    private ActionWorkflowDO requireWorkflow(Long id) {
        if (id == null) throw new ServiceException("Workflow ID 不能为空");
        ActionWorkflowDO workflow = workflowMapper.selectById(id);
        if (workflow == null) throw new ServiceException("Workflow 不存在：" + id);
        return workflow;
    }


    private ActionWorkflowRunDO requireRun(Long id) {
        if (id == null) throw new ServiceException("运行 ID 不能为空");
        ActionWorkflowRunDO run = runMapper.selectById(id);
        if (run == null) throw new ServiceException("Workflow 运行不存在：" + id);
        return run;
    }

    private ActionWorkflowRunRespVO toResp(ActionWorkflowRunDO run) {
        refreshCompensationStatus(run);
        ActionWorkflowRunRespVO vo = new ActionWorkflowRunRespVO();
        vo.setId(run.getId()); vo.setWorkflowId(run.getWorkflowId()); vo.setStatus(run.getStatus());
        vo.setIdempotencyKey(run.getIdempotencyKey());
        vo.setInputParams(run.getInputParams()); vo.setOutputContext(run.getOutputContext());
        vo.setCurrentNodeKey(run.getCurrentNodeKey()); vo.setErrorMessage(run.getErrorMessage());
        vo.setCancelRequested(run.getCancelRequested()); vo.setStartedTime(run.getStartedTime());
        vo.setFinishedTime(run.getFinishedTime());
        vo.setCompensationStatus(run.getCompensationStatus());
        vo.setCompensationRequestedTime(run.getCompensationRequestedTime());
        vo.setCompensationFinishedTime(run.getCompensationFinishedTime());
        vo.setCompensationError(run.getCompensationError());
        vo.setCompensationRetryableCount(countRetryableCompensations(run.getId()));
        vo.setSteps(stepMapper.selectByRunId(run.getId()).stream().map(this::toStepResp).collect(Collectors.toList()));
        return vo;
    }

    private int countRetryableCompensations(Long runId) {
        Map<Long, ActionExecutionDO> latestByStep = new LinkedHashMap<>();
        for (ActionExecutionDO execution : executionMapper.selectWorkflowCompensations(runId)) {
            Long key = execution.getWorkflowStepId() == null ? execution.getId() : execution.getWorkflowStepId();
            latestByStep.put(key, execution);
        }
        int count = 0;
        for (ActionExecutionDO execution : latestByStep.values()) {
            if (FAILED.equals(execution.getStatus()) || "REJECTED".equals(execution.getStatus())) count++;
        }
        return count;
    }

    private void fillCompensationStatus(ActionWorkflowCompensationRespVO result, ActionWorkflowRunDO run) {
        result.setCompensationStatus(StringUtils.defaultIfBlank(run.getCompensationStatus(), COMPENSATION_NONE));
        result.setCompensationRequestedTime(run.getCompensationRequestedTime());
        result.setCompensationFinishedTime(run.getCompensationFinishedTime());
        result.setCompensationError(run.getCompensationError());
    }

    /** 根据关联动作执行记录汇总补偿计划状态，保证刷新页面后仍能看到真实进度。 */
    private void refreshCompensationStatus(ActionWorkflowRunDO run) {
        if (run == null || run.getId() == null) return;
        List<ActionExecutionDO> compensations = executionMapper.selectWorkflowCompensations(run.getId());
        String nextStatus = COMPENSATION_NONE;
        String nextError = null;
        Date requestedTime = run.getCompensationRequestedTime();
        Date finishedTime = null;
        if (!compensations.isEmpty()) {
            if (requestedTime == null) requestedTime = compensations.stream()
                    .map(ActionExecutionDO::getCreateTime).filter(Objects::nonNull).min(Date::compareTo).orElse(new Date());
            boolean pending = false;
            boolean executed = false;
            boolean failed = false;
            List<String> errors = new ArrayList<>();
            for (ActionExecutionDO execution : compensations) {
                String status = StringUtils.defaultIfBlank(execution.getStatus(), "UNKNOWN");
                if (EXECUTED.equals(status)) executed = true;
                else if (FAILED.equals(status) || "REJECTED".equals(status)
                        || RECONCILIATION_REQUIRED.equals(status) || "ROLLED_BACK".equals(status)) {
                    failed = true;
                    if (StringUtils.isNotBlank(execution.getErrorMessage())) errors.add(execution.getErrorMessage());
                } else {
                    pending = true;
                }
            }
            if (pending) nextStatus = executed || failed ? COMPENSATION_PARTIAL : COMPENSATION_REQUESTED;
            else if (failed) nextStatus = executed ? COMPENSATION_PARTIAL : COMPENSATION_FAILED;
            else nextStatus = COMPENSATION_COMPENSATED;
            if (!errors.isEmpty()) nextError = errors.stream().distinct().limit(5).collect(Collectors.joining("；"));
            if (!pending) finishedTime = run.getCompensationFinishedTime() == null
                    ? new Date() : run.getCompensationFinishedTime();
        }
        String currentStatus = StringUtils.defaultIfBlank(run.getCompensationStatus(), COMPENSATION_NONE);
        boolean changed = !Objects.equals(currentStatus, nextStatus)
                || !Objects.equals(run.getCompensationError(), nextError)
                || !Objects.equals(run.getCompensationRequestedTime(), requestedTime)
                || (!Objects.equals(run.getCompensationFinishedTime(), finishedTime)
                && (finishedTime != null || run.getCompensationFinishedTime() != null));
        run.setCompensationStatus(nextStatus);
        run.setCompensationRequestedTime(requestedTime);
        run.setCompensationFinishedTime(finishedTime);
        run.setCompensationError(nextError);
        if (!changed) return;
        LambdaUpdateWrapper<ActionWorkflowRunDO> update = new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, run.getId())
                .set(ActionWorkflowRunDO::getCompensationStatus, nextStatus)
                .set(ActionWorkflowRunDO::getCompensationRequestedTime, requestedTime)
                .set(ActionWorkflowRunDO::getCompensationFinishedTime, finishedTime)
                .set(ActionWorkflowRunDO::getCompensationError, nextError)
                .set(ActionWorkflowRunDO::getUpdateTime, new Date());
        runMapper.update(null, update);
    }

    private ActionWorkflowStepRespVO toStepResp(ActionWorkflowStepDO step) {
        ActionWorkflowStepRespVO vo = new ActionWorkflowStepRespVO();
        vo.setId(step.getId()); vo.setRunId(step.getRunId()); vo.setNodeKey(step.getNodeKey());
        vo.setNodeType(step.getNodeType()); vo.setActionId(step.getActionId()); vo.setExecutionId(step.getExecutionId());
        vo.setStatus(step.getStatus()); vo.setInputParams(step.getInputParams()); vo.setOutputJson(step.getOutputJson());
        vo.setErrorMessage(step.getErrorMessage()); vo.setStartedTime(step.getStartedTime());
        vo.setFinishedTime(step.getFinishedTime()); vo.setAttemptNo(step.getAttemptNo());
        return vo;
    }

    private String normalizeJson(String value) {
        if (StringUtils.isBlank(value)) return "{}";
        parseObject(value);
        return value;
    }

    private Map<String, Object> parseObject(String value) {
        try {
            JSONObject json = JSON.parseObject(StringUtils.defaultIfBlank(value, "{}"));
            return json == null ? new LinkedHashMap<>() : new LinkedHashMap<>(json);
        } catch (Exception e) {
            throw new ServiceException("Workflow 输入参数必须是合法 JSON");
        }
    }
}
