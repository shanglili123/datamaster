package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.dal.dataobject.skill.AiAskSessionDO;
import com.datamaster.module.ai.dal.mapper.skill.AiAskSessionMapper;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.controller.admin.workflow.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.*;
import com.datamaster.security.web.service.PermissionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/** Durable decision boundary. A model suggestion is never a confirmation or an approval. */
@Service
public class DecisionProposalService {
    private static final long TTL = 30 * 60 * 1000L;
    @Resource private JdbcTemplate jdbc;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private PermissionService permissions;
    @Resource private ISpaceApi spaceApi;
    @Resource private AiAskSessionMapper sessionMapper;
    @Resource private OntologyMapper ontologyMapper;
    @Resource private DecisionBindingResolver bindingResolver;
    @Resource private IAiActionDecisionService actionDecision;
    @Resource private IActionExecutionService executions;
    @Resource private IActionWorkflowRuntimeService workflows;
    @Resource private ActionExecutionMapper executionMapper;
    @Resource private ActionWorkflowRunMapper runMapper;
    @Resource private DecisionCalculationService calculations;

    public Proposal create(AskUnifiedReqVO request, AgentToolContext context) {
        requirePermission(false);
        if (request == null || context == null || !Objects.equals(SecurityUtils.getUserId(), context.getUserId())) {
            throw new ServiceException("缺少真实用户上下文");
        }
        if (!Objects.equals(request.getSpaceId(), context.getSpaceId())
                || !Objects.equals(request.getSpaceCode(), context.getSpaceCode()) || request.getOntologyId() == null) {
            throw new ServiceException("决策必须绑定当前空间和本体");
        }
        Row row = new Row();
        row.ownerId = context.getUserId(); row.spaceId = context.getSpaceId(); row.spaceCode = context.getSpaceCode();
        row.sessionId = request.getSessionId(); row.ontologyId = request.getOntologyId();
        row.conversationId = StringUtils.defaultIfBlank(request.getConversationId(),
                row.sessionId == null ? "decision:" + UUID.randomUUID() : "session:" + row.sessionId);
        if (row.conversationId.length() > 256) throw new ServiceException("会话标识过长");
        Proposal proposal = new Proposal();
        proposal.setId(UUID.randomUUID().toString()); proposal.setRevision(1); proposal.setStatus("ANALYZING");
        proposal.setOperationId(context.getOperationId()); proposal.setOntologyId(String.valueOf(row.ontologyId));
        proposal.setGoal(request.getQuestion()); proposal.setExpiresAt(new Date(System.currentTimeMillis() + TTL));
        proposal.setObjectKey(request.getObjectKey());
        proposal.setMessage("正在形成有证据的决策提案；尚未准备或提交动作"); row.proposal = proposal;
        authorize(row); insert(row, proposal);
        return copy(proposal);
    }

    public Proposal get(String id) {
        requirePermission(false);
        Row row = latest(id, false); authorize(row);
        return overlay(row.proposal, dispatch(id));
    }

    public Proposal saveAnalysis(Proposal generated, AgentToolContext context) {
        requirePermission(false);
        if (generated == null || context == null) throw new ServiceException("缺少决策分析上下文");
        return shortTransaction().execute(tx -> {
            Row row = latest(generated.getId(), true); authorize(row); assertContext(row, context);
            if (!Objects.equals(generated.getRevision(), row.proposal.getRevision())
                    || !"ANALYZING".equals(row.proposal.getStatus()) || dispatch(generated.getId()) != null) {
                throw new ServiceException("分析提案已冻结，请刷新");
            }
            if (!"PROPOSED".equals(generated.getStatus()) && !"NEED_INPUT".equals(generated.getStatus())) {
                throw new ServiceException("分析结果只能为 PROPOSED 或 NEED_INPUT");
            }
            Proposal next = copy(row.proposal);
            next.setRevision(next.getRevision() + 1); next.setStatus(generated.getStatus());
            next.setEvidence(generated.getEvidence()); next.setCandidates(generated.getCandidates());
            next.setMessage(generated.getMessage());
            // Identity, goal, ontology, operation, expiry and owner remain server-owned.
            insert(row, next); return copy(next);
        });
    }

    public Proposal calculate(CalculationRequest request) {
        requirePermission(false);
        if (request == null) throw new ServiceException("缺少测算请求");
        if (calculations.usesMetric(request) && (!permissions.hasPermi("ont:metric:query") || !permissions.hasPermi("ont:metric:execute")))
            throw new ServiceException("当前用户缺少使用正式指标证据进行测算的权限");
        if (request.getCandidates() != null && request.getCandidates().stream()
                .anyMatch(spec -> spec != null && Boolean.TRUE.equals(spec.getAlignExecutionQuantities()))
                && !permissions.hasPermi("ont:action:edit")) throw new ServiceException("同步执行参数需要动作编辑权限");
        Request identity = new Request(); identity.setDecisionId(request.getDecisionId()); identity.setRevision(request.getRevision());
        Row row = checkedRequest(identity, false); assertLive(row.proposal);
        if (!Arrays.asList("PROPOSED", "PREVIEW_READY").contains(row.proposal.getStatus()) || dispatch(row.proposal.getId()) != null)
            throw new ServiceException("只有未提交的候选提案可以测算");
        Proposal next = copy(row.proposal);
        calculations.calculate(next, request, String.valueOf(SecurityUtils.getUserId()));
        next.setRevision(next.getRevision() + 1); next.setStatus("PROPOSED");
        next.setBinding(null); next.setSelectedCandidateId(null); next.setPreviewDigest(null); next.setDefinitionDigest(null);
        next.setMessage("确定性测算已完成并生成新版本，旧预览已失效；未知项不代表通过，仍需准备和确认执行");
        return shortTransaction().execute(tx -> {
            Row current = checkedRequest(identity, true); assertLive(current.proposal);
            if (dispatch(next.getId()) != null || !Objects.equals(current.proposal.getStatus(), row.proposal.getStatus()))
                throw new ServiceException("提案已提交或状态变化，请刷新");
            insert(current, next); return copy(next);
        });
    }

    public Proposal prepare(Request request) {
        requirePermission(true);
        Row row = checkedRequest(request, false); assertLive(row.proposal);
        if (dispatch(row.proposal.getId()) != null) throw new ServiceException("提案已提交，不能重新准备");
        if (!Arrays.asList("PROPOSED", "PREVIEW_READY").contains(row.proposal.getStatus())) {
            throw new ServiceException("当前状态不能准备动作");
        }
        Candidate candidate = candidate(row.proposal, request.getCandidateId());
        if (candidate.isBaseline()) throw new ServiceException("基线方案不执行动作");
        if (candidate.getConstraints() != null) for (Constraint constraint : candidate.getConstraints()) {
            if (constraint != null && "FAIL".equalsIgnoreCase(constraint.getStatus())) {
                throw new ServiceException("候选方案违反硬约束：" + constraint.getName());
            }
        }
        if (candidate.getEvaluation() != null && "FAIL".equalsIgnoreCase(candidate.getEvaluation().getStatus()))
            throw new ServiceException("候选方案未通过评估，不能准备执行");
        Binding suggested = candidate.getSuggestedBinding();
        if (suggested == null || StringUtils.isBlank(suggested.getTargetId())) {
            AiActionDecisionReqVO req = new AiActionDecisionReqVO();
            req.setOntologyId(row.ontologyId); req.setPrompt(row.proposal.getGoal() + "\n选定方案：" + candidate.getName()
                    + "\n方案理由：" + StringUtils.defaultString(candidate.getRationale()));
            req.setObjectKey(row.proposal.getObjectKey()); req.setSpaceId(row.spaceId); req.setSpaceCode(row.spaceCode);
            AiActionDecisionRespVO decision = actionDecision.decide(req);
            if (!"ALLOW".equals(decision.getDecision()) || decision.getActionId() == null
                    || Boolean.TRUE.equals(decision.getRequiresHuman())) {
                throw new ServiceException("动作绑定仍需人工补充：" + decision.getReason());
            }
            suggested = new Binding(); suggested.setType("ACTION"); suggested.setTargetId(String.valueOf(decision.getActionId()));
            suggested.setInputParams(decision.getInputParams());
        }
        if (StringUtils.isBlank(suggested.getObjectKey())) suggested.setObjectKey(row.proposal.getObjectKey());
        Binding resolved = bindingResolver.resolve(suggested, row.ontologyId, row.spaceId, row.spaceCode);
        calculations.assertExecution(candidate, resolved);
        Proposal prepared = copy(row.proposal);
        prepared.setRevision(prepared.getRevision() + 1); prepared.setStatus("PREVIEW_READY");
        prepared.setSelectedCandidateId(candidate.getId()); prepared.setBinding(resolved);
        prepared.setObjectKey(resolved.getObjectKey()); prepared.setDefinitionDigest(resolved.getDefinitionDigest());
        prepared.setExpiresAt(new Date(System.currentTimeMillis() + TTL));
        prepared.setMessage("只读准备完成：目标、参数、定义版本已冻结；尚未提交、未执行。提交时仍需前置条件、字段权限、dry-run及原审批流程。");
        prepared.setPreviewDigest(previewDigest(row, prepared));
        return shortTransaction().execute(tx -> {
            Row current = checkedRequest(request, true); assertLive(current.proposal);
            if (!Objects.equals(current.proposal.getStatus(), row.proposal.getStatus()) || dispatch(prepared.getId()) != null) {
                throw new ServiceException("提案状态已变化，请刷新");
            }
            insert(current, prepared); return copy(prepared);
        });
    }

    /** Only this explicit HTTP/user entry can claim a dispatch; there are no model calls here. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Proposal confirm(Request request) {
        requirePermission(true);
        Row row = checkedRequest(request, false);
        if (!"PREVIEW_READY".equals(row.proposal.getStatus())) throw new ServiceException("请先准备动作并确认当前预览");
        if (StringUtils.isBlank(request.getPreviewDigest())
                || !request.getPreviewDigest().equals(row.proposal.getPreviewDigest())
                || !Objects.equals(request.getCandidateId(), row.proposal.getSelectedCandidateId())) {
            throw new ServiceException("确认内容与服务端预览不一致");
        }
        Dispatch existing = dispatch(row.proposal.getId());
        if (existing != null) return reconcile(row, existing);
        assertLive(row.proposal);
        calculations.assertExecution(candidate(row.proposal, row.proposal.getSelectedCandidateId()), row.proposal.getBinding());
        Binding current = bindingResolver.resolve(row.proposal.getBinding(), row.ontologyId, row.spaceId, row.spaceCode);
        if (!Objects.equals(current.getDefinitionDigest(), row.proposal.getDefinitionDigest())
                || !Objects.equals(current.getVersion(), row.proposal.getBinding().getVersion())) {
            throw new ServiceException("动作或 Workflow 定义已变化，必须重新创建提案并预览");
        }
        boolean claimed = Boolean.TRUE.equals(shortTransaction().execute(tx -> {
            Row locked = checkedRequest(request, true); assertLive(locked.proposal);
            if (dispatch(locked.proposal.getId()) != null) return false;
            Binding b = locked.proposal.getBinding();
            jdbc.update("INSERT INTO ai_decision_dispatch(decision_id,revision,idempotency_key,binding_type,target_id,status) VALUES(?,?,?,?,?,?)",
                    locked.proposal.getId(), locked.proposal.getRevision(), idempotency(locked.proposal), b.getType(),
                    DecisionBindingResolver.positiveId(b.getTargetId()), "SUBMITTING");
            return true;
        }));
        if (!claimed) return reconcile(row, dispatch(row.proposal.getId()));
        // The durable claim has committed. External submission must not share that transaction.
        try {
            Binding b = row.proposal.getBinding();
            calculations.assertExecution(candidate(row.proposal, row.proposal.getSelectedCandidateId()), b);
            Binding rechecked = bindingResolver.resolve(b, row.ontologyId, row.spaceId, row.spaceCode);
            if (!Objects.equals(rechecked.getDefinitionDigest(), row.proposal.getDefinitionDigest()))
                throw new ServiceException("定义或对象状态已变化，停止提交，请重新预览");
            if ("WORKFLOW".equals(b.getType())) {
                ActionWorkflowRunReqVO req = new ActionWorkflowRunReqVO();
                req.setExpectedVersion(b.getVersion());
                req.setInputParams(b.getInputParams()); req.setObjectKey(b.getObjectKey());
                req.setSpaceId(row.spaceId); req.setSpaceCode(row.spaceCode); req.setAsync(true);
                req.setIdempotencyKey(idempotency(row.proposal));
                ActionWorkflowRunRespVO result = workflows.start(DecisionBindingResolver.positiveId(b.getTargetId()), req);
                record(row.proposal, null, result.getId(), result.getStatus(), result.getErrorMessage());
            } else {
                ExecutionSubmitReqVO req = new ExecutionSubmitReqVO();
                req.setExpectedVersion(b.getVersion());
                req.setActionId(DecisionBindingResolver.positiveId(b.getTargetId())); req.setInputParams(b.getInputParams());
                req.setObjectKey(b.getObjectKey()); req.setSpaceId(row.spaceId); req.setSpaceCode(row.spaceCode);
                req.setIdempotencyKey(idempotency(row.proposal)); req.setTriggerType("MANUAL");
                req.setTriggerRef("AI_DECISION"); req.setCreatorId(row.ownerId); req.setCreateBy(SecurityUtils.getUsername());
                req.setMaxAttempts(1);
                ExecutionRespVO result = executions.submitExecution(req);
                record(row.proposal, result.getId(), null, result.getStatus(), result.getErrorMessage());
            }
        } catch (RuntimeException submissionError) {
            // Timeout/crash/commit ambiguity: do not retry submission, even with an idempotency key.
            jdbc.update("UPDATE ai_decision_dispatch SET status='RECONCILIATION_REQUIRED',message=?,updated_at=CURRENT_TIMESTAMP,lock_version=lock_version+1 WHERE decision_id=? AND revision=? AND status='SUBMITTING'",
                    "提交结果未知，需按幂等键对账；禁止自动重放", row.proposal.getId(), row.proposal.getRevision());
        }
        return reconcile(row, dispatch(row.proposal.getId()));
    }

    public Proposal refresh(String id) {
        requirePermission(false);
        Row row = latest(id, false); authorize(row);
        Dispatch dispatch = dispatch(id);
        return dispatch == null ? copy(row.proposal) : reconcile(row, dispatch);
    }

    /** Effect assessment is independent of execution and cannot change a prepared binding. */
    public Proposal updateEffect(String id, Integer revision, String status, String message) {
        requirePermission(false);
        Row row = latest(id, false); authorize(row);
        if (!Objects.equals(revision, row.proposal.getRevision()) || dispatch(id) == null) {
            throw new ServiceException("没有可复查的当前执行");
        }
        if (!Arrays.asList("UNKNOWN", "IMPROVED", "NOT_IMPROVED", "INCONCLUSIVE").contains(status)) {
            throw new ServiceException("不支持的效果复查状态");
        }
        jdbc.update("UPDATE ai_decision_dispatch SET effect_status=?,effect_message=?,updated_at=CURRENT_TIMESTAMP,lock_version=lock_version+1 WHERE decision_id=? AND revision=?",
                status, truncate(message), id, revision);
        return get(id);
    }

    private Proposal reconcile(Row row, Dispatch d) {
        if (d == null) return copy(row.proposal);
        if (!Objects.equals(d.revision, row.proposal.getRevision())) throw new ServiceException("执行绑定版本不一致，需人工对账");
        Long executionId = d.executionId; Long runId = d.runId;
        // Lookup by stable server-owned key also recovers submit-committed/store-update-failed cases.
        if ("WORKFLOW".equals(d.bindingType)) {
            ActionWorkflowRunDO run = runMapper.selectByWorkflowIdAndIdempotencyKey(d.targetId, d.idempotencyKey);
            if (run != null) {
                if (!Objects.equals(run.getCreatorId(), row.ownerId)) throw new ServiceException("运行所有者不一致");
                ActionWorkflowRunRespVO result = workflows.get(run.getId());
                record(row.proposal, null, result.getId(), result.getStatus(), result.getErrorMessage());
            } else markUnknown(row.proposal, d);
        } else {
            ActionExecutionDO execution = executionMapper.selectByActionIdAndIdempotencyKey(d.targetId, d.idempotencyKey);
            if (execution != null) {
                if (!Objects.equals(execution.getOntologyId(), row.ontologyId)
                        || !Objects.equals(execution.getCreatorId(), row.ownerId)
                        || !Objects.equals(execution.getSpaceId(), row.spaceId)
                        || !Objects.equals(execution.getSpaceCode(), row.spaceCode)) {
                    throw new ServiceException("执行上下文不一致，需人工对账");
                }
                executions.getExecutionById(execution.getId()); // Retain existing service permission/response semantics.
                record(row.proposal, execution.getId(), null, execution.getStatus(), execution.getErrorMessage());
            } else markUnknown(row.proposal, d);
        }
        return overlay(row.proposal, dispatch(row.proposal.getId()));
    }

    private void markUnknown(Proposal proposal, Dispatch d) {
        // Another request can observe a healthy in-flight claim; allow a short observation window, not a retry.
        if ("SUBMITTING".equals(d.status) && d.updatedAt != null
                && System.currentTimeMillis() - d.updatedAt.getTime() < 120000L) return;
        jdbc.update("UPDATE ai_decision_dispatch SET status='RECONCILIATION_REQUIRED',message=?,updated_at=CURRENT_TIMESTAMP,lock_version=lock_version+1 WHERE decision_id=? AND revision=? AND execution_id IS NULL AND run_id IS NULL",
                "未找到可靠执行记录；禁止自动重放，请人工对账", proposal.getId(), proposal.getRevision());
    }

    private void record(Proposal proposal, Long executionId, Long runId, String executionStatus, String message) {
        jdbc.update("UPDATE ai_decision_dispatch SET status=?,execution_id=?,run_id=?,execution_status=?,message=?,updated_at=CURRENT_TIMESTAMP,lock_version=lock_version+1 WHERE decision_id=? AND revision=?",
                proposalStatus(executionStatus), executionId, runId, executionStatus, truncate(message), proposal.getId(), proposal.getRevision());
    }

    private String proposalStatus(String status) {
        if ("EXECUTED".equals(status) || "SUCCEEDED".equals(status)) return "SUCCEEDED";
        if ("RUNNING".equals(status)) return "RUNNING";
        if ("PENDING_APPROVAL".equals(status) || "WAITING_APPROVAL".equals(status)) return "WAITING_APPROVAL";
        if ("FAILED".equals(status) || "REJECTED".equals(status) || "CANCELED".equals(status) || "ROLLED_BACK".equals(status)) return status;
        if ("RECONCILIATION_REQUIRED".equals(status)) return status;
        return "SUBMITTED"; // APPROVED/QUEUED/RUNNING is not success.
    }

    private Row checkedRequest(Request request, boolean lock) {
        if (request == null || request.getRevision() == null || StringUtils.isBlank(request.getDecisionId())) {
            throw new ServiceException("决策 ID 和版本不能为空");
        }
        Row row = latest(request.getDecisionId(), lock); authorize(row);
        if (!Objects.equals(request.getRevision(), row.proposal.getRevision())) throw new ServiceException("提案版本已变化，请刷新后重新确认");
        return row;
    }

    private Candidate candidate(Proposal proposal, String id) {
        if (proposal.getCandidates() != null) for (Candidate candidate : proposal.getCandidates()) {
            if (candidate != null && Objects.equals(id, candidate.getId()) && StringUtils.isNotBlank(id)) return candidate;
        }
        throw new ServiceException("候选方案不属于当前提案");
    }

    private void authorize(Row row) {
        Long userId = SecurityUtils.getUserId();
        if (!Objects.equals(userId, row.ownerId) || (row.spaceId == null && StringUtils.isBlank(row.spaceCode))
                || !spaceApi.hasUserSpace(userId, row.spaceId, row.spaceCode)) throw new ServiceException("无权访问此决策提案");
        OntologyDO ontology = ontologyMapper.selectById(row.ontologyId);
        if (ontology == null || (ontology.getDelFlag() != null && ontology.getDelFlag() != 0)
                || (row.spaceId != null && !Objects.equals(row.spaceId, ontology.getSpaceId()))
                || (StringUtils.isNotBlank(row.spaceCode) && !Objects.equals(row.spaceCode, ontology.getSpaceCode()))) {
            throw new ServiceException("本体不属于提案绑定空间");
        }
        if (row.sessionId != null) {
            AiAskSessionDO session = sessionMapper.selectById(row.sessionId);
            if (session == null || Boolean.TRUE.equals(session.getDelFlag()) || !Objects.equals(row.ownerId, session.getUserId())
                    || !Objects.equals(row.spaceId, session.getSpaceId())
                    || !Objects.equals(row.spaceCode, session.getSpaceCode())
                    || !"ASK_DATA".equals(session.getCategory())) throw new ServiceException("会话不存在或不属于当前普通问数上下文");
        }
    }

    private void assertContext(Row row, AgentToolContext context) {
        if (!Objects.equals(row.ownerId, context.getUserId()) || !Objects.equals(SecurityUtils.getUserId(), context.getUserId())
                || !Objects.equals(row.spaceId, context.getSpaceId())
                || !Objects.equals(row.spaceCode, context.getSpaceCode())
                || !Objects.equals(row.proposal.getOperationId(), context.getOperationId())) throw new ServiceException("分析上下文与提案不一致");
        Object conversationId = context.getAttributes().get("conversationId");
        if (conversationId != null && !row.conversationId.equals(String.valueOf(conversationId)))
            throw new ServiceException("分析会话标识不一致");
        Object ontologyId = context.getAttributes().get("ontologyId");
        if (ontologyId != null && !String.valueOf(row.ontologyId).equals(String.valueOf(ontologyId))) throw new ServiceException("分析本体不一致");
        Object sessionId = context.getAttributes().get("sessionId");
        if (sessionId != null && !String.valueOf(row.sessionId).equals(String.valueOf(sessionId))) throw new ServiceException("分析会话不一致");
    }

    private void requirePermission(boolean write) {
        SecurityUtils.getUserId();
        if (!permissions.hasPermi("ai:ask-data:query") || (write && !permissions.hasPermi("ont:action:edit"))) {
            throw new ServiceException("缺少决策查询或动作编辑权限");
        }
    }
    private void assertLive(Proposal p) {
        if (p.getExpiresAt() == null || p.getExpiresAt().getTime() < System.currentTimeMillis()) throw new ServiceException("提案已过期，请重新分析和准备");
    }
    private String previewDigest(Row row, Proposal p) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("owner", row.ownerId); value.put("spaceId", row.spaceId); value.put("spaceCode", row.spaceCode);
        value.put("session", row.sessionId); value.put("conversation", row.conversationId); value.put("ontology", row.ontologyId);
        value.put("id", p.getId()); value.put("revision", p.getRevision()); value.put("candidate", p.getSelectedCandidateId());
        value.put("binding", p.getBinding()); value.put("expiresAt", p.getExpiresAt().getTime());
        Candidate selected = candidate(p, p.getSelectedCandidateId());
        value.put("calculation", selected.getCalculation()); value.put("calculationSpec", selected.getCalculationSpec());
        value.put("calculationContext", p.getCalculationContext());
        return DecisionBindingResolver.sha256(DecisionBindingResolver.canonical(value));
    }
    private String idempotency(Proposal p) { return "decision:" + p.getId() + ":" + p.getRevision(); }
    private Proposal copy(Proposal p) { return JSON.parseObject(JSON.toJSONString(p), Proposal.class); }
    private String truncate(String value) { return value == null ? null : value.substring(0, Math.min(value.length(), 1000)); }
    private TransactionTemplate shortTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setTimeout(15); return template;
    }
    private Row latest(String id, boolean lock) {
        if (StringUtils.isBlank(id) || id.length() > 64) throw new ServiceException("无效的提案 ID");
        // All revisions share the immutable first-row mutex. Locking only the latest row allows
        // a waiter to confirm an old revision after a concurrent prepare inserts a newer one.
        if (lock) jdbc.query("SELECT decision_id FROM ai_decision_proposal WHERE decision_id=? AND revision=1 FOR UPDATE",
                (rs, n) -> rs.getString(1), id);
        List<Row> rows = jdbc.query("SELECT * FROM ai_decision_proposal WHERE decision_id=? ORDER BY revision DESC LIMIT 1",
                (rs, n) -> row(rs), id);
        if (rows.isEmpty()) throw new ServiceException("提案不存在");
        return rows.get(0);
    }
    private Row row(ResultSet rs) throws SQLException {
        Row row = new Row(); row.ownerId = number(rs, "owner_id"); row.spaceId = number(rs, "space_id");
        row.spaceCode = rs.getString("space_code"); row.sessionId = number(rs, "session_id");
        row.conversationId = rs.getString("conversation_id"); row.ontologyId = number(rs, "ontology_id");
        row.proposal = JSON.parseObject(rs.getString("payload"), Proposal.class); return row;
    }
    private void insert(Row row, Proposal p) {
        jdbc.update("INSERT INTO ai_decision_proposal(decision_id,revision,owner_id,space_id,space_code,session_id,conversation_id,ontology_id,operation_id,status,payload,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                p.getId(), p.getRevision(), row.ownerId, row.spaceId, row.spaceCode, row.sessionId, row.conversationId, row.ontologyId,
                p.getOperationId(), p.getStatus(), JSON.toJSONString(p), new Timestamp(p.getExpiresAt().getTime()));
    }
    private Dispatch dispatch(String id) {
        List<Dispatch> result = jdbc.query("SELECT * FROM ai_decision_dispatch WHERE decision_id=?", (rs, n) -> {
            Dispatch d = new Dispatch(); d.revision = rs.getInt("revision"); d.idempotencyKey = rs.getString("idempotency_key");
            d.bindingType = rs.getString("binding_type"); d.targetId = number(rs, "target_id"); d.status = rs.getString("status");
            d.executionId = number(rs, "execution_id"); d.runId = number(rs, "run_id"); d.executionStatus = rs.getString("execution_status");
            d.message = rs.getString("message"); d.effectStatus = rs.getString("effect_status"); d.effectMessage = rs.getString("effect_message");
            d.updatedAt = rs.getTimestamp("updated_at"); return d;
        }, id);
        return result.isEmpty() ? null : result.get(0);
    }
    private Long number(ResultSet rs, String name) throws SQLException { long value = rs.getLong(name); return rs.wasNull() ? null : value; }
    private Proposal overlay(Proposal persisted, Dispatch d) {
        Proposal result = copy(persisted);
        if (d == null) return result;
        result.setStatus(d.status); result.setExecutionId(d.executionId == null ? null : String.valueOf(d.executionId));
        result.setRunId(d.runId == null ? null : String.valueOf(d.runId)); result.setExecutionStatus(d.executionStatus);
        result.setMessage(StringUtils.defaultIfBlank(d.message, "已提交到现有执行流程；审批、执行及业务效果须分别确认"));
        result.setEffectStatus(StringUtils.defaultIfBlank(d.effectStatus, "UNKNOWN"));
        if (d.effectMessage != null) result.setEffectMessage(d.effectMessage);
        return result;
    }
    private static class Row {
        Long ownerId, spaceId, sessionId, ontologyId;
        String spaceCode, conversationId;
        Proposal proposal;
    }
    private static class Dispatch {
        Integer revision;
        Long targetId, executionId, runId;
        String bindingType, idempotencyKey, status, executionStatus, message, effectStatus, effectMessage;
        Date updatedAt;
    }
}
