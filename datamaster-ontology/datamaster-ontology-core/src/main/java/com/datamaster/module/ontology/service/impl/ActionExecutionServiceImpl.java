package com.datamaster.module.ontology.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.convert.ActionConvert;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IActionApprovalService;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IFunctionService;
import com.datamaster.module.ontology.service.IWebhookService;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.neo4j.node.ActionExecutionNode;
import com.datamaster.neo4j.node.ObjectNode;
import com.datamaster.neo4j.rel.ObjectDecisionRel;
import com.datamaster.neo4j.service.LineageDataService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

@Service
@Validated
public class ActionExecutionServiceImpl implements IActionExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutionServiceImpl.class);

    @Resource private ActionExecutionMapper executionMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private FunctionMapper functionMapper;
    @Resource private ObjectMapper objectMapper;
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private DataSourceFactory dataSourceFactory;
    @Resource private IAssetsTableGovernanceApiService tableGovernanceApiService;
    @Resource private IFunctionService functionService;
    @Resource private IWebhookService webhookService;
    @Resource private IActionApprovalService approvalService;
    @Resource private ObjectStateConditionResolver objectStateConditionResolver;
    @Autowired
    @Lazy
    private IActionExecutionService self;

    /**
     * 动作血缘写入（可插拔）：
     * LINEAGE_ENABLED=true 时由 LineageDataService 提供 Neo4j 写入；
     * 未开启/未部署 Neo4j 时该 Bean 不存在，注入为 null，不影响动作执行主流程。
     */
    @Autowired(required = false)
    private LineageDataService lineageDataService;

    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    /** 执行失败错误码：落 ERROR_CODE 供统一结构/审计/前端展示（成功后置 null） */
    private static final String ERROR_CODE_EXECUTE = "EXECUTE_ERROR";
    private static final String ERROR_CODE_FUNCTION = "FUNCTION_ERROR";
    private static final String ERROR_CODE_TRIGGER_SUBMIT = "TRIGGER_SUBMIT_ERROR";
    private static final String ERROR_CODE_ACTION_VERSION = "ACTION_VERSION_CHANGED";
    private static final String ERROR_CODE_FUNCTION_VERSION = "FUNCTION_VERSION_CHANGED";
    private static final String EXECUTION_OWNER = UUID.randomUUID().toString();

    /** 治理 API 入口标识：查询维持原值，增删改按动作类型区分（写操作严格模式） */
    private static final String ENTRANCE_ONTOLOGY_ACTION = "ONTOLOGY_ACTION";
    private static final String ENTRANCE_ONTOLOGY_CREATE = "ONTOLOGY_CREATE";
    private static final String ENTRANCE_ONTOLOGY_UPDATE = "ONTOLOGY_UPDATE";
    private static final String ENTRANCE_ONTOLOGY_DELETE = "ONTOLOGY_DELETE";

    @Override
    @Transactional
    public ExecutionRespVO submitExecution(ExecutionSubmitReqVO reqVO) {
        ActionDO action = actionMapper.selectById(reqVO.getActionId());
        if (action == null) {
            throw new RuntimeException("动作不存在: " + reqVO.getActionId());
        }
        // autoExecute 表示“无需人工确认，提交后由 Worker 直接执行”，与触发来源无关。
        boolean autoExecute = resolveAutoExecute(action, reqVO);
        // 幂等去重：同动作 + 同幂等键已存在执行记录 → 直接返回首次执行记录，防重复提交/重复执行
        if (reqVO.getIdempotencyKey() != null && !reqVO.getIdempotencyKey().trim().isEmpty()) {
            ActionExecutionDO existing = executionMapper.selectByActionIdAndIdempotencyKey(
                    action.getId(), reqVO.getIdempotencyKey());
            if (existing != null) {
                log.info("命中幂等键，返回既有执行记录: actionId={}, idempotencyKey={}, executionId={}",
                        action.getId(), reqVO.getIdempotencyKey(), existing.getId());
                return ActionConvert.INSTANCE.convert(existing);
            }
        }
        // FUNCTION 类型动作：不生成SQL、不干跑、不做表权限校验。
        // 直接校验函数与参数后，将替换后的解析代码体写入 generatedSql 供审批展示。
        if ("FUNCTION".equals(action.getActionType())) {
            if (action.getFunctionId() == null) {
                throw new RuntimeException("函数类型动作未绑定共享函数");
            }
            String resolvedBody = functionService.resolveFunctionBody(action.getFunctionId(), reqVO.getInputParams());
            if (resolvedBody == null || resolvedBody.trim().isEmpty()) {
                resolvedBody = "[函数类型动作]";
            }
            // 版本冻结：函数定义版本 + 解析函数体 SHA-256（执行前复核一致性）
            FunctionDO function = functionMapper.selectById(action.getFunctionId());
            Integer functionVersion = function == null || function.getVersion() == null ? 1 : function.getVersion();
            String functionHash = sha256(resolvedBody);
            String effectiveObjectKey = deriveObjectKey(action, reqVO);
            ActionExecutionDO functionExec = ActionExecutionDO.builder()
                    .actionId(action.getId()).ontologyId(action.getOntologyId())
                    .inputParams(reqVO.getInputParams()).generatedSql(resolvedBody)
                    .previewResult(null)
                    .spaceId(reqVO.getSpaceId()).spaceCode(reqVO.getSpaceCode())
                    .actionVersion(defaultVersion(action.getVersion()))
                    .functionVersion(functionVersion).functionHash(functionHash)
                    .idempotencyKey(reqVO.getIdempotencyKey())
                    .objectKey(effectiveObjectKey)
                    .autoExecute(autoExecute)
                    .triggerType(defaultTriggerType(reqVO.getTriggerType()))
                    .triggerRef(reqVO.getTriggerRef()).eventId(reqVO.getEventId())
                    .maxAttempts(defaultMaxAttempts(reqVO.getMaxAttempts()))
                    .nextRunTime(reqVO.getNextRunTime()).attemptNo(0)
                    .status(STATUS_PENDING).build();
            // 前置条件通过后，按动作执行模式直接进入 Worker 队列或等待一次人工确认。
            applySubmissionDecision(functionExec, action, reqVO);
            executionMapper.insert(functionExec);
            initApprovalChainIfPending(functionExec, action);
            return ActionConvert.INSTANCE.convert(functionExec);
        }
        if ("COMPOSITE".equals(action.getActionType())) {
            return submitCompositeExecution(action, reqVO, autoExecute);
        }
        // 提交前先校验当前空间对目标物理表的访问权限；
        // 增删改（CREATE/UPDATE/DELETE）额外按涉及列做字段级严格校验（主张5接入）
        String effectiveObjectKey = deriveObjectKey(action, reqVO);
        Map<String, Object> rawInputParams = parseParams(reqVO.getInputParams());
        Map<String, Object> params = applyParamConfig(action, rawInputParams, effectiveObjectKey);
        assertTableAccess(action, reqVO.getSpaceId(), reqVO.getSpaceCode(),
                extractActionColumns(action, params), action.getActionType());
        String sql = generateSql(action, params);
        Map<String, Object> preview = executeDryRun(action.getConceptId(), sql, action.getActionType());
        // beforeData 不在提交时抓取：提交→执行之间存在时间差（审批、排队），
        // 提交时抓的快照可能与执行前一刻的数据不一致（如提交后行被改/被删/新插入）。
        // 「可溯源回退」要求修改前数据=执行前一刻的旧值，故在 executeExecution 执行 DML 前重查。
        // 版本冻结：动作定义版本（执行前复核，防定义变更后误执行与预览不一致的逻辑）
        ActionExecutionDO exec = ActionExecutionDO.builder()
                .actionId(action.getId()).ontologyId(action.getOntologyId())
                .inputParams(reqVO.getInputParams()).generatedSql(sql)
                .previewResult(toJson(preview))
                .spaceId(reqVO.getSpaceId()).spaceCode(reqVO.getSpaceCode())
                .actionVersion(defaultVersion(action.getVersion()))
                .idempotencyKey(reqVO.getIdempotencyKey())
                .objectKey(effectiveObjectKey)
                .autoExecute(autoExecute)
                .triggerType(defaultTriggerType(reqVO.getTriggerType()))
                .triggerRef(reqVO.getTriggerRef()).eventId(reqVO.getEventId())
                .maxAttempts(defaultMaxAttempts(reqVO.getMaxAttempts()))
                .nextRunTime(reqVO.getNextRunTime()).attemptNo(0)
                .status(STATUS_PENDING).build();
        // 前置条件通过后，按动作执行模式直接进入 Worker 队列或等待一次人工确认。
        applySubmissionDecision(exec, action, reqVO);
        executionMapper.insert(exec);
        initApprovalChainIfPending(exec, action);
        return ActionConvert.INSTANCE.convert(exec);
    }

    private ExecutionRespVO submitCompositeExecution(ActionDO action, ExecutionSubmitReqVO reqVO,
                                                       boolean autoExecute) {
        String effectiveObjectKey = deriveObjectKey(action, reqVO);
        Map<String, Object> rawInputParams = parseParams(reqVO.getInputParams());
        List<ExecutionStep> steps = resolveExecutionSteps(action);
        assertSameDatasource(steps);
        List<Map<String, Object>> plan = new ArrayList<>();
        List<Map<String, Object>> previews = new ArrayList<>();
        for (ExecutionStep step : steps) {
            ActionDO targetAction = targetAction(action, step);
            Map<String, Object> params = applyParamConfig(targetAction, rawInputParams,
                    effectiveObjectKey, action);
            assertTableAccess(targetAction, reqVO.getSpaceId(), reqVO.getSpaceCode(),
                    extractActionColumns(targetAction, params), step.actionType);
            String sql = generateSql(targetAction, params);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stepNo", step.stepNo);
            item.put("name", step.name);
            item.put("conceptId", step.conceptId);
            item.put("actionType", step.actionType);
            item.put("content", sql);
            plan.add(item);
            Map<String, Object> preview = new LinkedHashMap<>(item);
            preview.put("result", executeDryRun(step.conceptId, sql, step.actionType));
            previews.add(preview);
        }
        ActionExecutionDO exec = ActionExecutionDO.builder()
                .actionId(action.getId()).ontologyId(action.getOntologyId())
                .inputParams(reqVO.getInputParams()).generatedSql(toJson(plan))
                .previewResult(toJson(previews))
                .spaceId(reqVO.getSpaceId()).spaceCode(reqVO.getSpaceCode())
                .actionVersion(defaultVersion(action.getVersion()))
                .idempotencyKey(reqVO.getIdempotencyKey())
                .objectKey(effectiveObjectKey)
                .autoExecute(autoExecute)
                .triggerType(defaultTriggerType(reqVO.getTriggerType()))
                .triggerRef(reqVO.getTriggerRef()).eventId(reqVO.getEventId())
                .maxAttempts(defaultMaxAttempts(reqVO.getMaxAttempts()))
                .nextRunTime(reqVO.getNextRunTime()).attemptNo(0)
                .status(STATUS_PENDING).build();
        applySubmissionDecision(exec, action, reqVO);
        executionMapper.insert(exec);
        initApprovalChainIfPending(exec, action);
        return ActionConvert.INSTANCE.convert(exec);
    }

    @Override
    public List<ExecutionRespVO> submitByTrigger(String triggerRef, String inputParams, String objectKey,
                                                 String eventId, Long spaceId, String spaceCode) {
        List<ExecutionRespVO> result = new ArrayList<>();
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("数据到达触发必须携带稳定 eventId");
        }
        List<ActionDO> actions = actionMapper.selectByTriggerRef(triggerRef);
        if (actions == null || actions.isEmpty()) {
            log.info("数据到达触发未匹配到启用动作 triggerRef={}", triggerRef);
            return result;
        }
        for (ActionDO action : actions) {
            ExecutionSubmitReqVO req = new ExecutionSubmitReqVO();
            req.setActionId(action.getId());
            req.setInputParams(inputParams == null || inputParams.trim().isEmpty() ? "{}" : inputParams);
            req.setObjectKey(objectKey);
            req.setEventId(eventId);
            req.setIdempotencyKey(eventId.trim() + ":" + action.getId());
            req.setTriggerType("DATA_ARRIVAL");
            req.setTriggerRef(triggerRef);
            req.setMaxAttempts(1);
            req.setSpaceId(spaceId);
            req.setSpaceCode(spaceCode);
            try {
                // 通过代理调用，使每个匹配动作拥有独立事务；单个动作失败不回滚同事件的其他动作。
                result.add(self.submitExecution(req));
            } catch (Exception e) {
                // 并发重复事件可能在“先查后插”窗口命中唯一索引；事务结束后再查一次即可返回赢家记录。
                ActionExecutionDO existing = executionMapper.selectByActionIdAndIdempotencyKey(
                        action.getId(), req.getIdempotencyKey());
                if (existing != null) {
                    result.add(ActionConvert.INSTANCE.convert(existing));
                    log.info("数据到达触发并发幂等命中 actionId={} eventId={} executionId={}",
                            action.getId(), eventId, existing.getId());
                } else {
                    log.error("数据到达触发自动提交动作失败 actionId={} triggerRef={} eventId={}",
                            action.getId(), triggerRef, eventId, e);
                    ActionExecutionDO failed = ActionExecutionDO.builder()
                            .actionId(action.getId()).ontologyId(action.getOntologyId())
                            .inputParams(req.getInputParams()).objectKey(req.getObjectKey())
                            .idempotencyKey(req.getIdempotencyKey())
                            .autoExecute(resolveAutoExecute(action, req)).triggerType("DATA_ARRIVAL")
                            .triggerRef(triggerRef).eventId(eventId)
                            .actionVersion(defaultVersion(action.getVersion()))
                            .attemptNo(0).maxAttempts(1)
                            .status(STATUS_FAILED).executeTime(new Date())
                            .errorCode(ERROR_CODE_TRIGGER_SUBMIT)
                            .errorMessage(e.getMessage())
                            .build();
                    try {
                        executionMapper.insert(failed);
                        result.add(ActionConvert.INSTANCE.convert(failed));
                    } catch (Exception recordError) {
                        log.error("数据到达触发失败记录落库失败 actionId={} eventId={}",
                                action.getId(), eventId, recordError);
                    }
                }
            }
        }
        return result;
    }

    /**
     * ① 提交时判定：评估动作的 submissionCriteria，落 objectKey/criteriaResult，并按结果设定执行记录状态。
     * 免判定（无 criteria）→ 按动作执行模式决定直接执行或等待一次人工确认；
     * 前置检查不通过 → FAILED/PRECONDITION_FAILED；检查通过但需人工确认 → PENDING_APPROVAL；否则直接 APPROVED。
     * objectKey 已在提交分支推导完成（deriveObjectKey，优先调用方显式值），此处不再覆盖。
     */
    private void applySubmissionDecision(ActionExecutionDO exec, ActionDO action, ExecutionSubmitReqVO reqVO) {
        String criteriaResult = approvalService.evaluateCriteria(action, reqVO.getInputParams(), exec.getObjectKey());
        exec.setCriteriaResult(criteriaResult);
        boolean passed = isCriteriaPassed(criteriaResult);
        // autoExecute 表示该次执行无需人工确认；触发来源与执行模式互相独立。
        boolean requiresApproval = !Boolean.TRUE.equals(exec.getAutoExecute())
                && ((action.getApprovalLevels() != null && action.getApprovalLevels() > 0)
                || Boolean.TRUE.equals(action.getNeedsApproval()));
        if (!passed) {
            exec.setStatus(STATUS_FAILED);
            exec.setCurrentStage(0);
            exec.setErrorCode("PRECONDITION_FAILED");
            exec.setErrorMessage("提交前置检查未通过，动作未进入执行队列");
            exec.setResultContext(buildResultContext(exec, action, null));
        } else if (requiresApproval) {
            exec.setStatus(STATUS_PENDING);
            exec.setCurrentStage(1);
        } else {
            exec.setStatus(STATUS_APPROVED);
            exec.setCurrentStage(0);
            exec.setApproveTime(new Date());
        }
    }

    /**
     * 提交判定通过与否（解析 criteriaResult 的 passed）。
     * fail-closed：决策为 EVALUATION_ERROR（条件解析/求值失败）或缺省决策/解析异常 → 一律按不通过拒绝，
     * 修复原先解析异常「按通过处理」的 fail-open 缺陷——配置错误绝不能静默放行执行。
     */
    private boolean isCriteriaPassed(String criteriaResult) {
        if (criteriaResult == null || criteriaResult.trim().isEmpty()) {
            return true;
        }
        try {
            JsonNode node = objectMapper.readTree(criteriaResult);
            String decision = node.path("decision").asText("");
            if (ActionApprovalServiceImpl.DECISION_EVALUATION_ERROR.equals(decision)) {
                return false;
            }
            if (node.has("passed")) {
                return node.path("passed").asBoolean(false);
            }
            // 缺省 passed 字段但决策未显式评估错误：老数据兼容按决策判定（PASS 才通过）
            return ActionApprovalServiceImpl.DECISION_PASS.equals(decision);
        } catch (Exception e) {
            log.warn("解析提交判定结果失败，按不通过处理: {}", e.getMessage());
            return false;
        }
    }

    /** 提交后如需人工确认（PENDING_APPROVAL），建立唯一确认任务。 */
    private void initApprovalChainIfPending(ActionExecutionDO exec, ActionDO action) {
        if (STATUS_PENDING.equals(exec.getStatus())) {
            approvalService.initChain(exec, action);
        }
    }

    /* ---------------- 阶段一运行时加固：对象标识推导 + 版本冻结 + 幂等 ---------------- */

    /** 版本缺省值：未显式设置（老数据）按 1 冻结，保证与 DB 默认列一致。 */
    private int defaultVersion(Integer version) {
        return version == null ? 1 : version;
    }

    private boolean resolveAutoExecute(ActionDO action, ExecutionSubmitReqVO reqVO) {
        // 对象行操作的“预览”接口必须停在 APPROVED，等待用户确认当前预览内容；这不是审批。
        if ("PREVIEW_ONLY".equalsIgnoreCase(reqVO.getTriggerType())) {
            return false;
        }
        boolean needsManualApproval = (action.getApprovalLevels() != null && action.getApprovalLevels() > 0)
                || Boolean.TRUE.equals(action.getNeedsApproval());
        return !needsManualApproval;
    }

    private int defaultMaxAttempts(Integer maxAttempts) {
        return maxAttempts == null || maxAttempts < 1 ? 1 : maxAttempts;
    }

    private String defaultTriggerType(String triggerType) {
        return triggerType == null || triggerType.trim().isEmpty() ? "MANUAL" : triggerType.trim();
    }

    /** 执行前复核提交时冻结的动作/函数定义，任何变化均 fail-closed。 */
    private void validateFrozenDefinition(ActionExecutionDO exec, ActionDO action) {
        if (exec.getActionVersion() != null
                && !exec.getActionVersion().equals(defaultVersion(action.getVersion()))) {
            throw new IllegalStateException("动作定义已变更（提交时版本 v" + exec.getActionVersion()
                    + "，当前 v" + defaultVersion(action.getVersion()) + "），请基于新定义重新提交");
        }
        if (!"FUNCTION".equals(action.getActionType())) {
            return;
        }
        FunctionDO function = functionMapper.selectById(action.getFunctionId());
        int currentFunctionVersion = function == null ? 1 : defaultVersion(function.getVersion());
        if (exec.getFunctionVersion() != null
                && !exec.getFunctionVersion().equals(currentFunctionVersion)) {
            throw new IllegalStateException("函数定义版本已变更（提交时版本 v" + exec.getFunctionVersion()
                    + "，当前 v" + currentFunctionVersion + "），请基于新定义重新提交");
        }
        if (exec.getFunctionHash() != null && !exec.getFunctionHash().isEmpty()) {
            String currentBody = functionService.resolveFunctionBody(action.getFunctionId(), exec.getInputParams());
            if (currentBody == null || currentBody.trim().isEmpty()) {
                currentBody = "[函数类型动作]";
            }
            if (!exec.getFunctionHash().equals(sha256(currentBody))) {
                throw new IllegalStateException("函数定义内容已变更，请基于新定义重新提交");
            }
        }
    }

    private String classifyExecutionError(ActionDO action, Exception error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.startsWith("动作定义已变更")) {
            return ERROR_CODE_ACTION_VERSION;
        }
        if (message.startsWith("函数定义版本已变更") || message.startsWith("函数定义内容已变更")) {
            return ERROR_CODE_FUNCTION_VERSION;
        }
        return action != null && "FUNCTION".equals(action.getActionType())
                ? ERROR_CODE_FUNCTION : ERROR_CODE_EXECUTE;
    }

    /**
     * 推导稳定对象标识（objectKey）：调用方显式传入则优先采用；否则从概念主键属性 + 提交参数提取。
     * 单一主键属性 → 值字符串；联合主键属性 → 规范化 JSON（{code: value}，按 code 字典序保证可比较/可幂等）。
     * 概念未绑定主键属性或参数缺失主键值 → 返回 null（object.* 条件会因此无法求值，fail-closed 拒绝）。
     */
    private String deriveObjectKey(ActionDO action, ExecutionSubmitReqVO reqVO) {
        if (reqVO.getObjectKey() != null && !reqVO.getObjectKey().trim().isEmpty()) {
            return reqVO.getObjectKey().trim();
        }
        if (action == null || action.getConceptId() == null) {
            return null;
        }
        List<ColumnMapping> mapping;
        try {
            List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
            if (tables == null || tables.isEmpty()) {
                return null;
            }
            mapping = resolveColumnMappings(action.getConceptId(), tables.get(0).getId());
        } catch (Exception e) {
            log.debug("推导 objectKey 失败（概念无绑定表？），按 null 处理: {}", e.getMessage());
            return null;
        }
        List<ColumnMapping> pkMappings = new ArrayList<>();
        for (ColumnMapping m : mapping) {
            if (m.primaryKey) {
                pkMappings.add(m);
            }
        }
        if (pkMappings.isEmpty()) {
            return null;
        }
        Map<String, Object> params = parseParams(reqVO.getInputParams());
        if (pkMappings.size() == 1) {
            ColumnMapping pk = pkMappings.get(0);
            Object v = params.get(pk.semanticName);
            return v == null ? null : String.valueOf(v);
        }
        // 联合主键：规范化 JSON（按属性 code 字典序）
        try {
            Map<String, Object> keyMap = new TreeMap<>();
            for (ColumnMapping pk : pkMappings) {
                Object v = params.get(pk.semanticName);
                if (v == null) {
                    return null; // 联合主键任一缺失 → 无法定位对象
                }
                keyMap.put(pk.semanticName, String.valueOf(v));
            }
            return objectMapper.writeValueAsString(keyMap);
        } catch (Exception e) {
            log.debug("序列化联合主键 objectKey 失败: {}", e.getMessage());
            return null;
        }
    }

    /** SHA-256 摘要（hex 小写），用于函数体/动作定义的版本指纹。 */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 理论上 SHA-256 必然存在；异常时退化为内容哈希，避免阻断提交流程
            log.warn("计算 SHA-256 失败，退化为字符串哈希: {}", e.getMessage());
            return String.valueOf((input == null ? "" : input).hashCode());
        }
    }

    /**
     * ④ 释放 CAS 执行锁（LOCK_TIME 置空）。成功/失败路径都必须释放；
     * 状态已落 EXECUTED/FAILED，后续 executeExecution 的状态校验即天然防重入，LOCK_TIME 仅标志「执行中」。
     */
    private void releaseExecutionLock(Long executionId) {
        executionMapper.update(null, new LambdaUpdateWrapper<ActionExecutionDO>()
                .eq(ActionExecutionDO::getId, executionId)
                .set(ActionExecutionDO::getLockTime, null)
                .set(ActionExecutionDO::getLockOwner, null));
    }

    /**
     * 构建统一执行结果上下文 JSON（RESULT_CONTEXT）：
     * 固定结构 { actionId, actionType, actionVersion, functionVersion?, endTime, status, errorCode?, errorMessage?, ...业务汇总 }
     * 供前端详情 / 审计 / 后续工作流编排（阶段二）统一消费；失败时业务汇总为 null。
     */
    private String buildResultContext(ActionExecutionDO exec, ActionDO action, Map<String, Object> businessSummary) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode ctx = objectMapper.createObjectNode();
            ctx.put("actionId", action.getId());
            ctx.put("actionType", action.getActionType());
            ctx.put("actionVersion", exec.getActionVersion() == null
                    ? defaultVersion(action.getVersion()) : exec.getActionVersion());
            if (exec.getObjectKey() != null) {
                ctx.put("objectKey", exec.getObjectKey());
            }
            if (exec.getTriggerType() != null) {
                ctx.put("triggerType", exec.getTriggerType());
            }
            ctx.put("autoExecute", Boolean.TRUE.equals(exec.getAutoExecute()));
            if (exec.getTriggerRef() != null) {
                ctx.put("triggerRef", exec.getTriggerRef());
            }
            if (exec.getEventId() != null) {
                ctx.put("eventId", exec.getEventId());
            }
            if (exec.getCriteriaResult() != null) {
                try {
                    ctx.set("preconditionResult", objectMapper.readTree(exec.getCriteriaResult()));
                } catch (Exception ignore) {
                    ctx.put("preconditionResult", exec.getCriteriaResult());
                }
            }
            ctx.put("endTime", exec.getExecuteTime() == null ? "" : String.valueOf(exec.getExecuteTime().getTime()));
            ctx.put("status", exec.getStatus());
            if (exec.getFunctionVersion() != null) {
                ctx.put("functionVersion", exec.getFunctionVersion());
            }
            if (exec.getErrorCode() != null) {
                ctx.put("errorCode", exec.getErrorCode());
            }
            if (exec.getErrorMessage() != null) {
                ctx.put("errorMessage", exec.getErrorMessage());
            }
            if (businessSummary != null) {
                for (Map.Entry<String, Object> entry : businessSummary.entrySet()) {
                    Object v = entry.getValue();
                    if (v instanceof Number) {
                        ctx.put(entry.getKey(), ((Number) v).doubleValue());
                    } else {
                        ctx.put(entry.getKey(), String.valueOf(v));
                    }
                }
            }
            return ctx.toString();
        } catch (Exception e) {
            log.warn("构建执行结果上下文失败，忽略: {}", e.getMessage());
            return null;
        }
    }

@Override
    @Transactional
    public void approveExecution(ApprovalReqVO reqVO) {
        ActionExecutionDO exec = getExecutionOrThrow(reqVO.getExecutionId());
        if (!STATUS_PENDING.equals(exec.getStatus())) {
            throw new RuntimeException("当前状态不允许审批: " + exec.getStatus());
        }
        // 单次人工确认：结论永久审计，意见可选；通过后由 Worker 自动执行。
        String result = approvalService.submitReviewerDecision(exec.getId(), true, reqVO.getApprovalReason());
        exec.setApprovalReason(reqVO.getApprovalReason());
        if (result == null) {
            // 旧单步路径（无审批链）：一次人工确认后交给 Worker 自动执行。
            exec.setStatus(STATUS_APPROVED);
            exec.setAutoExecute(true);
            exec.setApproveTime(new Date());
        } else if ("APPROVED".equals(result)) {
            exec.setStatus(STATUS_APPROVED);
            exec.setAutoExecute(true);
            exec.setApproveTime(new Date());
        } else if ("REJECTED".equals(result)) {
            exec.setStatus(STATUS_REJECTED);
        }
        // result == PENDING：审批流仍在中途/本关未完，保持 PENDING_APPROVAL
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public void rejectExecution(ApprovalReqVO reqVO) {
        ActionExecutionDO exec = getExecutionOrThrow(reqVO.getExecutionId());
        if (!STATUS_PENDING.equals(exec.getStatus())) {
            throw new RuntimeException("当前状态不允许拒绝: " + exec.getStatus());
        }
        String result = approvalService.submitReviewerDecision(exec.getId(), false, reqVO.getApprovalReason());
        exec.setApprovalReason(reqVO.getApprovalReason());
        if (result == null) {
            // 旧单步路径（无审批链）
            exec.setStatus(STATUS_REJECTED);
            exec.setApproveTime(new Date());
        } else if ("REJECTED".equals(result)) {
            // 任一审阅人拒绝 → 审批流终止 → 执行记录 REJECTED
            exec.setStatus(STATUS_REJECTED);
        }
        // result == PENDING：本关仍有其他审阅人需决策，保持 PENDING_APPROVAL
        executionMapper.updateById(exec);
    }

    @Override
    public ExecutionRespVO executeExecution(Long executionId, boolean triggerWebhook) {
        ActionExecutionDO exec = getExecutionOrThrow(executionId);
        if (!STATUS_APPROVED.equals(exec.getStatus())) {
            throw new RuntimeException("只有已批准的才能执行，当前状态: " + exec.getStatus());
        }
        // 先以独立数据库语句把 RUNNING 状态持久化，再访问业务库。该方法不包裹管理库长事务：
        // 若业务库写成功后进程崩溃，RUNNING 会被保留并由 Worker 转为 RECONCILIATION_REQUIRED，
        // 不会因为事务回滚重新变成 APPROVED 而盲目重放写动作。
        Date lockTime = new Date();
        int locked = executionMapper.update(null, new LambdaUpdateWrapper<ActionExecutionDO>()
                .eq(ActionExecutionDO::getId, executionId)
                .eq(ActionExecutionDO::getStatus, STATUS_APPROVED)
                .isNull(ActionExecutionDO::getLockTime)
                .apply("COALESCE(ATTEMPT_NO, 0) < COALESCE(MAX_ATTEMPTS, 1)")
                .set(ActionExecutionDO::getStatus, STATUS_RUNNING)
                .set(ActionExecutionDO::getLockTime, lockTime)
                .set(ActionExecutionDO::getLockOwner, EXECUTION_OWNER)
                .setSql("ATTEMPT_NO = COALESCE(ATTEMPT_NO, 0) + 1"));
        if (locked == 0) {
            throw new RuntimeException("执行记录正被其他请求执行、已完成或已达到最大尝试次数");
        }
        exec = getExecutionOrThrow(executionId);
        ActionDO action = null;
        DbQuery dbQuery = null;
        boolean finalStatePersisted = false;
        try {
            action = actionMapper.selectById(exec.getActionId());
            if (action == null) {
                throw new RuntimeException("动作不存在: " + exec.getActionId());
            }
            validateFrozenDefinition(exec, action);

            // 前置条件不能只在提交时检查。审批等待期间对象状态可能变化（例如库存被其他订单扣减），
            // 因此在任何业务写入/函数副作用发生前，基于最新对象状态再次 fail-closed 求值。
            String executionCriteriaResult = approvalService.evaluateCriteria(
                    action, exec.getInputParams(), exec.getObjectKey());
            exec.setCriteriaResult(executionCriteriaResult);
            if (!isCriteriaPassed(executionCriteriaResult)) {
                exec.setStatus(STATUS_FAILED);
                exec.setExecuteTime(new Date());
                exec.setErrorCode("PRECONDITION_FAILED");
                exec.setErrorMessage("执行前置检查未通过，动作未执行");
                exec.setResultContext(buildResultContext(exec, action, null));
            } else if ("FUNCTION".equals(action.getActionType())) {
                if (action.getFunctionId() == null) {
                    throw new RuntimeException("函数类型动作未绑定共享函数");
                }
                String effectiveInputParams = applyFunctionParamMapping(action, parseParams(exec.getInputParams()));
                String output = functionService.runFunctionWithBinding(
                        action.getFunctionId(),
                        action.getSourceConceptId(),
                        action.getSourceRelationIds(),
                        action.getOutputConceptId(),
                        action.getReadLimit(),
                        effectiveInputParams);
                exec.setPreviewResult(toJson(Collections.singletonMap("output", output)));
                exec.setTargetTable(resolveTargetTable(action.getOutputConceptId()));
                exec.setStatus(STATUS_EXECUTED);
                exec.setExecuteTime(new Date());
                exec.setErrorCode(null);
                exec.setErrorMessage(null);
                exec.setResultContext(buildResultContext(exec, action,
                        Collections.singletonMap("output", output)));
            } else if ("COMPOSITE".equals(action.getActionType())) {
                Map<String, Object> summary = executeCompositeSteps(exec, action);
                exec.setStatus(STATUS_EXECUTED);
                exec.setExecuteTime(new Date());
                exec.setErrorCode(null);
                exec.setErrorMessage(null);
                exec.setResultContext(buildResultContext(exec, action, summary));
            } else {
                dbQuery = getDbQuery(action.getConceptId());
                exec.setTargetTable(resolveTargetTable(action.getConceptId()));
                Map<String, Object> execParams = applyParamConfig(action, parseParams(exec.getInputParams()), exec.getObjectKey());
                assertTableAccess(action, exec.getSpaceId(), exec.getSpaceCode(),
                        extractActionColumns(action, execParams), action.getActionType());
                // 审批/排队期间对象值可能变化；真正执行前必须按 objectKey 重新读取条件值并重建 SQL。
                String sql = generateSql(action, execParams);
                exec.setGeneratedSql(sql);
                Map<String, Object> summary;
                if (sql.toUpperCase().trim().startsWith("SELECT")) {
                    List<Map<String, Object>> rows = dbQuery.queryList(sql);
                    exec.setPreviewResult(toJson(rows));
                    summary = Collections.singletonMap("rowCount", rows.size());
                } else {
                    if ("UPDATE".equals(action.getActionType()) || "DELETE".equals(action.getActionType())) {
                        exec.setBeforeData(captureSnapshot(action.getConceptId(), sql));
                    }
                    int affected = dbQuery.update(sql);
                    exec.setPreviewResult(toJson(Collections.singletonMap("affectedRows", affected)));
                    exec.setAfterData(captureAfterData(action, sql, execParams));
                    summary = Collections.singletonMap("affectedRows", affected);
                }
                exec.setStatus(STATUS_EXECUTED);
                exec.setExecuteTime(new Date());
                exec.setErrorCode(null);
                exec.setErrorMessage(null);
                exec.setResultContext(buildResultContext(exec, action, summary));
            }
        } catch (Exception e) {
            log.error("动作执行失败 executionId={}", executionId, e);
            exec.setStatus(STATUS_FAILED);
            exec.setExecuteTime(new Date());
            exec.setErrorCode(classifyExecutionError(action, e));
            exec.setErrorMessage(e.getMessage());
            if (action != null) {
                exec.setResultContext(buildResultContext(exec, action, null));
            }
        } finally {
            closeDbQuery(dbQuery);
            try {
                int updated = executionMapper.update(exec, new LambdaUpdateWrapper<ActionExecutionDO>()
                        .eq(ActionExecutionDO::getId, executionId)
                        .eq(ActionExecutionDO::getStatus, STATUS_RUNNING)
                        .eq(ActionExecutionDO::getLockOwner, EXECUTION_OWNER));
                finalStatePersisted = updated > 0;
            } finally {
                if (finalStatePersisted) {
                    releaseExecutionLock(executionId);
                } else {
                    log.error("动作最终状态未保存：执行锁可能已被超时对账接管 executionId={}", executionId);
                }
            }
        }
        if (!finalStatePersisted) {
            return ActionConvert.INSTANCE.convert(getExecutionOrThrow(executionId));
        }
        if (action != null && !"FUNCTION".equals(action.getActionType())
                && !"COMPOSITE".equals(action.getActionType())) {
            writeActionLineageSilently(exec, action);
        }
        if (triggerWebhook && action != null) {
            try {
                webhookService.notifyActionExecution(exec);
            } catch (Exception e) {
                log.warn("动作执行结果 Webhook 通知失败 executionId={}", executionId, e);
            }
        }
        return ActionConvert.INSTANCE.convert(exec);
    }

    /**
     * 多目标动作在同一个业务库事务中按步骤顺序执行。所有步骤在执行前已校验为同一数据源，
     * 任一步失败即回滚整个事务，避免出现“订单已改、库存未扣”的半完成状态。
     */
    private Map<String, Object> executeCompositeSteps(ActionExecutionDO exec, ActionDO action) throws Exception {
        List<ExecutionStep> steps = resolveExecutionSteps(action);
        assertSameDatasource(steps);
        Map<String, Object> rawInputParams = parseParams(exec.getInputParams());
        List<Map<String, Object>> plans = new ArrayList<>();
        List<Map<String, Object>> summaries = new ArrayList<>();
        List<Map<String, Object>> beforeSnapshots = new ArrayList<>();
        List<Map<String, Object>> afterSnapshots = new ArrayList<>();
        List<String> targetTables = new ArrayList<>();

        DbQuery dbQuery = getDbQuery(steps.get(0).conceptId);
        Connection connection = null;
        try {
            connection = dbQuery.getConnection();
            connection.setAutoCommit(false);
            for (ExecutionStep step : steps) {
                ActionDO targetAction = targetAction(action, step);
                Map<String, Object> params = applyParamConfig(targetAction, rawInputParams,
                        exec.getObjectKey(), action);
                assertTableAccess(targetAction, exec.getSpaceId(), exec.getSpaceCode(),
                        extractActionColumns(targetAction, params), step.actionType);
                String sql = generateSql(targetAction, params);
                String table = resolveTargetTable(step.conceptId);
                targetTables.add(table);

                Map<String, Object> plan = new LinkedHashMap<>();
                plan.put("stepNo", step.stepNo);
                plan.put("name", step.name);
                plan.put("conceptId", step.conceptId);
                plan.put("actionType", step.actionType);
                plan.put("content", sql);
                plans.add(plan);

                String beforeSelect = ("UPDATE".equals(step.actionType) || "DELETE".equals(step.actionType))
                        ? convertToSelect(sql) : null;
                List<Map<String, Object>> beforeRows = beforeSelect == null
                        ? Collections.emptyList() : queryRows(connection, beforeSelect);

                int affected;
                try (Statement statement = connection.createStatement()) {
                    affected = statement.executeUpdate(sql);
                }
                if (affected <= 0) {
                    throw new RuntimeException("多目标动作步骤 " + step.stepNo + "（" + step.name
                            + "）未匹配到可执行对象，已回滚全部步骤；请检查库存、对象状态或定位条件");
                }

                String afterSelect = convertToSelect(sql);
                if (afterSelect == null && "CREATE".equals(step.actionType)) {
                    afterSelect = buildSelectByPk(targetAction, params);
                }
                List<Map<String, Object>> afterRows = afterSelect == null
                        ? Collections.emptyList() : queryRows(connection, afterSelect);

                Map<String, Object> before = new LinkedHashMap<>();
                before.put("stepNo", step.stepNo);
                before.put("name", step.name);
                before.put("conceptId", step.conceptId);
                before.put("target", table);
                before.put("rows", beforeRows);
                beforeSnapshots.add(before);

                Map<String, Object> after = new LinkedHashMap<>();
                after.put("stepNo", step.stepNo);
                after.put("name", step.name);
                after.put("conceptId", step.conceptId);
                after.put("target", table);
                after.put("rows", afterRows);
                afterSnapshots.add(after);

                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("stepNo", step.stepNo);
                summary.put("name", step.name);
                summary.put("conceptId", step.conceptId);
                summary.put("actionType", step.actionType);
                summary.put("affectedRows", affected);
                summaries.add(summary);
            }
            connection.commit();
        } catch (Exception e) {
            if (connection != null) {
                try { connection.rollback(); } catch (Exception rollbackError) {
                    log.warn("多目标动作事务回滚失败 executionId={}", exec.getId(), rollbackError);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (Exception closeError) {
                    log.debug("多目标动作连接关闭失败", closeError);
                }
            }
            closeDbQuery(dbQuery);
        }

        exec.setGeneratedSql(toJson(plans));
        exec.setPreviewResult(toJson(summaries));
        exec.setBeforeData(toJson(beforeSnapshots));
        exec.setAfterData(toJson(afterSnapshots));
        exec.setTargetTable(String.join(",", new LinkedHashSet<>(targetTables)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stepCount", summaries.size());
        result.put("steps", summaries);
        return result;
    }

    private List<Map<String, Object>> queryRows(Connection connection, String sql) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            ResultSetMetaData meta = resultSet.getMetaData();
            int count = meta.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= count; i++) {
                    row.put(meta.getColumnLabel(i), resultSet.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    @Override
    @Transactional
    public ExecutionRespVO rollbackExecution(Long executionId) {
        ActionExecutionDO exec = getExecutionOrThrow(executionId);
        if (!STATUS_EXECUTED.equals(exec.getStatus())) {
            throw new RuntimeException("只有已执行(EXECUTED)的记录才能回退，当前状态: " + exec.getStatus());
        }
        if (exec.getRollbackTime() != null) {
            throw new RuntimeException("该记录已回退过（" + exec.getRollbackTime() + "），请勿重复回退");
        }
        ActionDO action = actionMapper.selectById(exec.getActionId());
        if (action == null) {
            throw new RuntimeException("动作不存在: " + exec.getActionId());
        }
        // 函数类型动作无物理表，不支持回退
        if ("FUNCTION".equals(action.getActionType())) {
            throw new RuntimeException("函数类型动作不支持回退");
        }
        if ("COMPOSITE".equals(action.getActionType())) {
            throw new RuntimeException("多目标动作已在同一事务中保证整体成功或整体撤销，暂不支持事后回退");
        }
        // 回退属于写操作：复用提交快照的空间上下文，按原动作类型走字段级严格校验
        Map<String, Object> execParams = applyParamConfig(action, parseParams(exec.getInputParams()), exec.getObjectKey());
        assertTableAccess(action, exec.getSpaceId(), exec.getSpaceCode(),
                extractActionColumns(action, execParams), action.getActionType());
        DbQuery dbQuery = getDbQuery(action.getConceptId());
        List<String> rollbackSqls = buildRollbackSqls(action, exec, dbQuery);
        if (rollbackSqls.isEmpty()) {
            throw new RuntimeException("没有可回退的数据（快照为空）");
        }
        String rollbackSql = String.join(";\n", rollbackSqls);
        // 回退后数据快照：UPDATE/DELETE 回退=还原的旧值快照 / CREATE 回退=删除新行后为空集
        String rollbackAfterData = "CREATE".equals(action.getActionType()) ? "[]" : exec.getBeforeData();
        try {
            // 事务内执行全部还原语句，任一失败整体回滚，避免出现半还原状态
            Connection con = dbQuery.getConnection();
            try {
                con.setAutoCommit(false);
                int totalAffected = 0;
                try (Statement st = con.createStatement()) {
                    for (String sql : rollbackSqls) {
                        totalAffected += st.executeUpdate(sql);
                    }
                }
                con.commit();
                // 原执行记录保留 EXECUTED，仅补回退标记（防止重复回退 + 留痕）：
                // 回退前数据 = 原执行后数据（回退动作执行时的当前状态）
                exec.setRollbackSql(rollbackSql);
                exec.setRollbackTime(new Date());
                exec.setErrorMessage(null);
                exec.setRollbackBeforeData(exec.getAfterData());
                exec.setRollbackAfterData(rollbackAfterData);
                // 回退动作本身新增一条独立执行记录（ROLLED_BACK）：
                // 列表可见「回退记录」，beforeData=回退前 / afterData=回退后，可独立查看前后对比
                ActionExecutionDO rollbackExec = ActionExecutionDO.builder()
                        .actionId(exec.getActionId())
                        .ontologyId(exec.getOntologyId())
                        .spaceId(exec.getSpaceId())
                        .spaceCode(exec.getSpaceCode())
                        .inputParams(exec.getInputParams())
                        .generatedSql(rollbackSql)
                        .previewResult(toJson(Collections.singletonMap("affectedRows", totalAffected)))
                        .targetTable(resolveTargetTable(action.getConceptId()))
                        .beforeData(exec.getAfterData())
                        .afterData(rollbackAfterData)
                        .status(STATUS_ROLLED_BACK)
                        .executeTime(new Date())
                        .build();
                executionMapper.insert(rollbackExec);
                // 回退动作本身产生一条 ROLLED_BACK 执行记录，触发绑定该动作/本体的 webhook 回调
                webhookService.notifyActionExecution(rollbackExec);
            } catch (Exception e) {
                try { con.rollback(); } catch (Exception rbEx) { log.debug("回退事务回滚失败", rbEx); }
                throw e;
            } finally {
                try { con.close(); } catch (Exception cEx) { log.debug("回退连接关闭失败", cEx); }
            }
        } catch (Exception e) {
            log.error("回退SQL失败", e);
            exec.setStatus(STATUS_FAILED);
            exec.setErrorMessage(e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
        executionMapper.updateById(exec);
        return ActionConvert.INSTANCE.convert(exec);
    }

    /**
     * 构建回退 SQL（可溯源回退）：
     * - UPDATE：按 beforeData 旧值全列写回，主键列进 WHERE 精确定位
     * - DELETE：按 beforeData 重 INSERT 被删行
     * - CREATE：按 afterData 主键 DELETE 新插行
     * SELECT 不涉及数据变更，返回空表（无回退动作）。
     * 主键定位双保险：1) 概念属性 isPrimary 标记的列；2) 物理表真实主键(colKey)兜底，
     * 避免属性未标记主键时回退无法精确定位行。
     */
    private List<String> buildRollbackSqls(ActionDO action, ActionExecutionDO exec, DbQuery dbQuery) {
        Long conceptId = action.getConceptId();
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        String table = tables.get(0).getTableName();
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, tables.get(0).getId());
        List<String> pkCols = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (m.primaryKey) {
                pkCols.add(m.physicalColumn);
            }
        }
        List<Map<String, Object>> beforeRows = parseSnapshotRows(exec.getBeforeData());
        List<Map<String, Object>> afterRows = parseSnapshotRows(exec.getAfterData());
        // 物理主键兜底：仅当属性级主键缺失时，用快照行 keys 交集物理表主键(colKey)精确回退
        if (pkCols.isEmpty()) {
            pkCols = resolvePhysicalPkColumns(dbQuery, table, beforeRows, afterRows);
        }
        List<String> sqls = new ArrayList<>();
        switch (action.getActionType()) {
            case "UPDATE":
                if (beforeRows.isEmpty()) {
                    throw new RuntimeException("缺少执行前快照，无法回退 UPDATE");
                }
                if (pkCols.isEmpty()) {
                    throw new RuntimeException("表未配置主键属性且物理表主键无法识别，无法精确定位回退行");
                }
                for (Map<String, Object> row : beforeRows) {
                    sqls.add(buildRollbackUpdate(table, pkCols, row));
                }
                break;
            case "DELETE":
                if (beforeRows.isEmpty()) {
                    throw new RuntimeException("缺少执行前快照，无法回退 DELETE");
                }
                for (Map<String, Object> row : beforeRows) {
                    sqls.add(buildRollbackInsert(table, row));
                }
                break;
            case "CREATE":
                if (afterRows.isEmpty()) {
                    throw new RuntimeException("缺少执行后快照，无法回退 CREATE");
                }
                if (pkCols.isEmpty()) {
                    throw new RuntimeException("表未配置主键属性且物理表主键无法识别，无法精确定位回退行");
                }
                for (Map<String, Object> row : afterRows) {
                    sqls.add(buildRollbackDelete(table, pkCols, row));
                }
                break;
            default:
                // SELECT 无数据变更，无需回退
                log.warn("动作类型 [{}] 不支持回退", action.getActionType());
        }
        return sqls;
    }

    /**
     * 物理表真实主键列兜底解析：
     * 通过已打开连接的 DatabaseMetaData.getPrimaryKeys 获取物理表主键列，
     * 与快照行 keys 求交集（仅保留快照中确有数据的列），避免引入快照外列导致 SQL 报错。
     * 属性级主键已存在时该方法不会被调用。
     */
    private List<String> resolvePhysicalPkColumns(DbQuery dbQuery, String table,
                                                  List<Map<String, Object>> beforeRows,
                                                  List<Map<String, Object>> afterRows) {
        Set<String> snapshotKeys = new LinkedHashSet<>();
        for (Map<String, Object> row : beforeRows) {
            snapshotKeys.addAll(row.keySet());
        }
        for (Map<String, Object> row : afterRows) {
            snapshotKeys.addAll(row.keySet());
        }
        List<String> pkCols = new ArrayList<>();
        if (snapshotKeys.isEmpty()) {
            return pkCols;
        }
        // 大小写不敏感交集：快照列名（结果集 label）与 COLUMN_NAME 可能因方言大小写规则不同
        Map<String, String> snapshotLower = new HashMap<>();
        for (String k : snapshotKeys) {
            snapshotLower.put(k.toLowerCase(), k);
        }
        Connection con = null;
        try {
            con = dbQuery.getConnection();
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col != null) {
                        String actualKey = snapshotLower.get(col.toLowerCase());
                        if (actualKey != null) {
                            pkCols.add(actualKey);
                        }
                    }
                }
            }
            if (!pkCols.isEmpty()) {
                log.info("回退主键兜底：物理表 [{}] 主键列 {} 与快照交集定位", table, pkCols);
            } else {
                log.warn("回退主键兜底：物理表 [{}] 无主键或快照不含主键列，快照列={}",
                        table, snapshotKeys);
            }
        } catch (Exception e) {
            log.warn("回退物理主键解析失败，将按原主键校验报错: {}", e.getMessage());
        } finally {
            if (con != null) {
                try { con.close(); } catch (Exception cEx) { log.debug("主键解析连接关闭失败", cEx); }
            }
        }
        return pkCols;
    }

    private List<Map<String, Object>> parseSnapshotRows(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("快照解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** UPDATE 回退：SET 非主键列 = 旧值，WHERE 主键 = 旧主键 */
    private String buildRollbackUpdate(String table, List<String> pkCols, Map<String, Object> row) {
        StringBuilder set = new StringBuilder();
        List<String> where = new ArrayList<>();
        for (Map.Entry<String, Object> en : row.entrySet()) {
            String col = en.getKey();
            if (pkCols.contains(col)) {
                where.add(col + " = " + rollbackSqlValue(en.getValue()));
            } else {
                if (set.length() > 0) set.append(", ");
                set.append(col).append(" = ").append(rollbackSqlValue(en.getValue()));
            }
        }
        if (where.isEmpty()) {
            throw new RuntimeException("快照行缺少主键值，无法回退");
        }
        return "UPDATE " + table + " SET " + set + " WHERE " + String.join(" AND ", where);
    }

    /** DELETE 回退：按被删行重新 INSERT */
    private String buildRollbackInsert(String table, Map<String, Object> row) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (!first) {
                cols.append(", ");
                vals.append(", ");
            }
            cols.append(en.getKey());
            vals.append(rollbackSqlValue(en.getValue()));
            first = false;
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")";
    }

    /** CREATE 回退：按新插行主键 DELETE */
    private String buildRollbackDelete(String table, List<String> pkCols, Map<String, Object> row) {
        List<String> where = new ArrayList<>();
        for (String pk : pkCols) {
            if (row.containsKey(pk)) {
                where.add(pk + " = " + rollbackSqlValue(row.get(pk)));
            }
        }
        if (where.isEmpty()) {
            throw new RuntimeException("快照行缺少主键值，无法回退");
        }
        return "DELETE FROM " + table + " WHERE " + String.join(" AND ", where);
    }

    /** 回退值渲染：null → NULL（不套引号），其余复用 sqlValue 的引号转义 */
    private String rollbackSqlValue(Object val) {
        if (val == null) {
            return "NULL";
        }
        return sqlValue(val);
    }

    @Override
    public ExecutionRespVO getExecutionById(Long id) {
        return ActionConvert.INSTANCE.convert(executionMapper.selectById(id));
    }

@Override
    public PageResult<ExecutionRespVO> getExecutionPage(ExecutionPageReqVO pageReqVO) {
        PageResult<ActionExecutionDO> page = executionMapper.selectPage(pageReqVO);
        List<ExecutionRespVO> list = ActionConvert.INSTANCE.convertExecutionList(page.getRows());
        fillCanApprove(list);
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public List<ExecutionRespVO> getPendingApprovals(Long ontologyId) {
        List<ExecutionRespVO> list = ActionConvert.INSTANCE.convertExecutionList(executionMapper.selectPendingApprovals(ontologyId));
        if (list != null) {
            // 待办仅返回当前登录用户可审的（未绑定审批人=任意可审；绑定后仅本人）
            list.removeIf(resp -> resp.getId() != null && !approvalService.canApprove(resp.getId()));
        }
        return list;
    }

    /** 执行记录列表回填 canApprove（仅待审批记录判定，非待审批保持 null=按钮不可见） */
    private void fillCanApprove(List<ExecutionRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (ExecutionRespVO resp : list) {
            if (resp.getId() != null && STATUS_PENDING.equals(resp.getStatus())) {
                resp.setCanApprove(approvalService.canApprove(resp.getId()));
            }
        }
    }

    // ================================================================
    //  SQL Engine
    // ================================================================

    private static class ExecutionStep {
        final int stepNo;
        final String name;
        final String actionType;
        final Long conceptId;
        final String paramConfig;

        ExecutionStep(int stepNo, String name, String actionType, Long conceptId, String paramConfig) {
            this.stepNo = stepNo;
            this.name = name;
            this.actionType = actionType;
            this.conceptId = conceptId;
            this.paramConfig = paramConfig;
        }
    }

    private List<ExecutionStep> resolveExecutionSteps(ActionDO action) {
        if (action.getExecutionSteps() == null || action.getExecutionSteps().trim().isEmpty()) {
            throw new RuntimeException("多目标动作未配置执行步骤");
        }
        try {
            List<Map<String, Object>> configs = objectMapper.readValue(action.getExecutionSteps(),
                    new TypeReference<List<Map<String, Object>>>() {});
            if (configs == null || configs.isEmpty()) {
                throw new RuntimeException("多目标动作未配置执行步骤");
            }
            List<ExecutionStep> result = new ArrayList<>();
            int index = 0;
            for (Map<String, Object> config : configs) {
                index++;
                Long conceptId = config.get("conceptId") == null ? null
                        : Long.valueOf(String.valueOf(config.get("conceptId")));
                String actionType = config.get("actionType") == null ? ""
                        : String.valueOf(config.get("actionType")).toUpperCase(Locale.ROOT);
                if (conceptId == null) {
                    throw new RuntimeException("执行步骤 " + index + " 未选择目标对象类型");
                }
                ConceptDO concept = conceptMapper.selectById(conceptId);
                if (concept == null || !Objects.equals(concept.getOntologyId(), action.getOntologyId())) {
                    throw new RuntimeException("执行步骤 " + index + " 的目标对象类型不属于当前本体");
                }
                if (!("CREATE".equals(actionType) || "UPDATE".equals(actionType) || "DELETE".equals(actionType))) {
                    throw new RuntimeException("执行步骤 " + index + " 的操作类型不受支持: " + actionType);
                }
                Object paramConfig = config.get("paramConfig");
                String paramConfigJson = paramConfig == null ? "[]" : objectMapper.writeValueAsString(paramConfig);
                String name = config.get("name") == null ? "步骤 " + index : String.valueOf(config.get("name"));
                result.add(new ExecutionStep(index, name, actionType, conceptId, paramConfigJson));
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("多目标动作执行步骤解析失败: " + e.getMessage(), e);
        }
    }

    private ActionDO targetAction(ActionDO source, ExecutionStep step) {
        ActionDO target = new ActionDO();
        target.setId(source.getId());
        target.setOntologyId(source.getOntologyId());
        target.setName(source.getName() + " / " + step.name);
        target.setActionType(step.actionType);
        target.setConceptId(step.conceptId);
        target.setParamConfig(step.paramConfig);
        return target;
    }

    private void assertSameDatasource(List<ExecutionStep> steps) {
        Long datasourceId = null;
        for (ExecutionStep step : steps) {
            List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(step.conceptId);
            if (tables == null || tables.isEmpty()) {
                throw new RuntimeException("执行步骤 " + step.stepNo + " 的目标对象未绑定物理表");
            }
            Long current = tables.get(0).getDatasourceId();
            if (current == null) {
                throw new RuntimeException("执行步骤 " + step.stepNo + " 的目标对象缺少数据源配置");
            }
            if (datasourceId == null) {
                datasourceId = current;
            } else if (!datasourceId.equals(current)) {
                throw new RuntimeException("一个多目标动作的所有执行步骤必须位于同一数据源，以保证失败时整体撤销");
            }
        }
    }

    /**
     * ColumnMapping pairs a physical column with its semantic property info.
     */
    private static class ColumnMapping {
        final String physicalColumn;   // ConceptTableDO.tableName + PropertyColumnDO.columnName
        final String semanticName;     // PropertyDO.code (used as param key)
        final boolean primaryKey;      // PropertyDO.isPrimary

        ColumnMapping(String physicalColumn, String semanticName, boolean primaryKey) {
            this.physicalColumn = physicalColumn;
            this.semanticName = semanticName;
            this.primaryKey = primaryKey;
        }
    }

    private List<ColumnMapping> resolveColumnMappings(Long conceptId, Long conceptTableId) {
        List<PropertyColumnDO> bindingCols = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        List<ColumnMapping> mappings = new ArrayList<>();
        for (PropertyColumnDO binding : bindingCols) {
            PropertyDO prop = propertyMapper.selectById(binding.getPropertyId());
            String propName = (prop != null && prop.getCode() != null) ? prop.getCode() : binding.getColumnName();
            boolean isPk = (prop != null && prop.getIsPrimary() != null && prop.getIsPrimary());
            mappings.add(new ColumnMapping(binding.getColumnName(), propName, isPk));
        }
        return mappings;
    }

    /**
     * 解析概念绑定的目标物理表名（用于执行记录打标 targetTable）。
     * 概念未绑定物理表时返回 null，打标动作静默跳过，不阻断主流程。
     */
    private String resolveTargetTable(Long conceptId) {
        if (conceptId == null) {
            return null;
        }
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables == null || tables.isEmpty()) {
            return null;
        }
        return tables.get(0).getTableName();
    }

    private String generateSql(ActionDO action, Map<String, Object> params) {
        Long conceptId = action.getConceptId();
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        ConceptTableDO table = tables.get(0);
        String physicalTable = table.getTableName();
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, table.getId());
        List<ConditionSpec> conditionSpecs = enrichPhysicalPkConditions(action, physicalTable,
                mappings, params, resolveConditionSpecs(action));
        switch (action.getActionType()) {
            case "SELECT": return buildSelectSql(physicalTable, mappings, params);
            case "CREATE": return buildInsertSql(physicalTable, mappings, params);
            case "UPDATE": return buildUpdateSql(physicalTable, mappings, params, conditionSpecs);
            case "DELETE": return buildDeleteSql(physicalTable, mappings, params, conditionSpecs);
            default: throw new RuntimeException("不支持的动作类型: " + action.getActionType());
        }
    }

    /**
     * 读取动作参数配置(PARAM_CONFIG)中显式勾选的「条件字段」：
     * condition=true 的列按配置顺序参与 UPDATE/DELETE 的 WHERE 构建。
     * 每个条件可带逻辑组合：
     * - conditionLink: 与前一个条件的连接符，AND(且) / OR(或)，首个条件忽略
     * - conditionNegate: 是否取反(NOT)，true 时生成 NOT (col = value)
     * 未配置 PARAM_CONFIG 或配置中无 condition 标记的旧动作返回空表，
     * 此时保持旧行为（UPDATE 退化为仅属性级主键、DELETE 退化为全部有值列）。
     */
    private List<ConditionSpec> resolveConditionSpecs(ActionDO action) {
        List<ConditionSpec> specs = new ArrayList<>();
        String configJson = action.getParamConfig();
        if (configJson == null || configJson.trim().isEmpty()) {
            return specs;
        }
        try {
            List<Map<String, Object>> configs = objectMapper.readValue(configJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> cfg : configs) {
                boolean condition = Boolean.TRUE.equals(cfg.get("condition"))
                        || "true".equalsIgnoreCase(String.valueOf(cfg.get("condition")));
                if (!condition || cfg.get("propertyCode") == null) {
                    continue;
                }
                String link = "OR".equalsIgnoreCase(String.valueOf(cfg.get("conditionLink"))) ? "OR" : "AND";
                boolean negate = Boolean.TRUE.equals(cfg.get("conditionNegate"))
                        || "true".equalsIgnoreCase(String.valueOf(cfg.get("conditionNegate")));
                String operator = normalizeConditionOperator(cfg.get("conditionOperator"));
                specs.add(new ConditionSpec(String.valueOf(cfg.get("propertyCode")), link, negate, operator));
            }
        } catch (Exception e) {
            log.warn("动作参数配置解析失败（条件字段识别）: {}", e.getMessage());
        }
        return specs;
    }

    /**
     * 物理表主键兜底（行操作精确定位）：
     * 仅当动作无属性级主键且未配置「条件字段」时（典型：对象管理行操作的内置 UPDATE/DELETE 动作），
     * 用物理表真实主键列（DatabaseMetaData.getPrimaryKeys，与回退主键解析同源）匹配属性映射，
     * 命中且已传值的属性追加为 AND 条件，使 WHERE 能按物理主键精确定位行。
     * 解析失败静默降级：保持原逻辑，由 buildUpdateSql/buildDeleteSql 原有校验规则报错。
     */
    private List<ConditionSpec> enrichPhysicalPkConditions(ActionDO action, String table,
                                                           List<ColumnMapping> mappings,
                                                           Map<String, Object> params,
                                                           List<ConditionSpec> conditionSpecs) {
        if (!("UPDATE".equals(action.getActionType()) || "DELETE".equals(action.getActionType()))) {
            return conditionSpecs;
        }
        if (conditionSpecs != null && !conditionSpecs.isEmpty()) {
            return conditionSpecs; // 已显式配置条件字段，尊重用户配置
        }
        for (ColumnMapping m : mappings) {
            if (m.primaryKey) {
                return conditionSpecs; // 已有属性级主键，走原主键定位链路
            }
        }
        List<ConditionSpec> enriched = new ArrayList<>();
        DbQuery dbQuery = null;
        try {
            dbQuery = getDbQuery(action.getConceptId());
            Set<String> pkCols = resolvePhysicalPrimaryKeyColumns(dbQuery, table);
            for (ColumnMapping m : mappings) {
                if (pkCols.contains(m.physicalColumn.toLowerCase()) && params.containsKey(m.semanticName)) {
                    enriched.add(new ConditionSpec(m.semanticName, "AND", false, "eq"));
                }
            }
            if (!enriched.isEmpty()) {
                log.info("SQL生成物理主键兜底：表 [{}] 主键列命中属性条件 {} 个", table, enriched.size());
            }
        } catch (Exception e) {
            log.warn("SQL生成物理主键兜底失败，保持原逻辑处理: {}", e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
        enriched.addAll(conditionSpecs == null ? Collections.emptyList() : conditionSpecs);
        return enriched;
    }

    /**
     * 物理表真实主键列解析（小写集合）：通过已打开连接的 DatabaseMetaData.getPrimaryKeys 获取。
     * 与回退兜底 resolvePhysicalPkColumns 同源，供行操作 SQL 生成定位复用。
     */
    private Set<String> resolvePhysicalPrimaryKeyColumns(DbQuery dbQuery, String table) {
        Set<String> pkCols = new LinkedHashSet<>();
        if (dbQuery == null) {
            return pkCols;
        }
        Connection con = null;
        try {
            con = dbQuery.getConnection();
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col != null) {
                        pkCols.add(col.toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("物理主键解析失败，回退空主键集合: {}", e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception cEx) {
                    log.debug("主键解析连接关闭失败", cEx);
                }
            }
        }
        return pkCols;
    }

    /**
     * 条件字段规格：属性语义名 + 与前一个条件的连接符(首个忽略) + 是否取反(NOT)。
     */
    private static class ConditionSpec {
        final String semanticName;
        final String link;      // "AND" 或 "OR"
        final boolean negate;   // NOT 取反
        final String operator;

        ConditionSpec(String semanticName, String link, boolean negate, String operator) {
            this.semanticName = semanticName;
            this.link = link;
            this.negate = negate;
            this.operator = operator;
        }
    }

    private String normalizeConditionOperator(Object operator) {
        String value = operator == null ? "eq" : String.valueOf(operator).toLowerCase(Locale.ROOT);
        switch (value) {
            case "eq": case "ne": case "gt": case "ge": case "lt": case "le":
            case "contains": case "in":
                return value;
            default:
                return "eq";
        }
    }

    private String buildSelectSql(String table, List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("SELECT ");
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (!first) sb.append(", ");
            sb.append(m.physicalColumn);
            first = false;
        }
        sb.append(" FROM ").append(table);
        String where = buildWhereClause(mappings, params);
        if (!where.isEmpty()) sb.append(" WHERE ").append(where);
        sb.append(" LIMIT 100");
        return sb.toString();
    }

    private String buildInsertSql(String table, List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                if (!first) { cols.append(", "); vals.append(", "); }
                cols.append(m.physicalColumn);
                vals.append(sqlValue(params.get(m.semanticName)));
                first = false;
            }
        }
        if (first) throw new RuntimeException("没有有效的输入参数");
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")";
    }

    private String buildUpdateSql(String table, List<ColumnMapping> mappings, Map<String, Object> params,
                                  List<ConditionSpec> conditionSpecs) {
        StringBuilder setClauses = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (!params.containsKey(m.semanticName)) {
                continue;
            }
            // WHERE 条件 = 属性级主键 或 动作配置中显式勾选的「条件字段」：这些列不进 SET 子句
            boolean isCondition = m.primaryKey || containsSpec(conditionSpecs, m.semanticName);
            if (isCondition) {
                continue;
            }
            if (!first) setClauses.append(", ");
            setClauses.append(m.physicalColumn).append(" = ")
                    .append(sqlAssignmentValue(m, params.get(m.semanticName)));
            first = false;
        }
        // 配置条件按序拼接（AND/OR/NOT），主键条件附加到末尾（AND）
        String whereClause = buildWhereWithConditions(mappings, params, conditionSpecs, true);
        if (whereClause.isEmpty()) throw new RuntimeException("UPDATE 必须包含主键或条件字段");
        if (first) throw new RuntimeException("UPDATE 没有可更新的字段");
        return "UPDATE " + table + " SET " + setClauses + " WHERE " + whereClause;
    }

    private String buildDeleteSql(String table, List<ColumnMapping> mappings, Map<String, Object> params,
                                  List<ConditionSpec> conditionSpecs) {
        String whereClause;
        // 显式配置了「条件字段」则严格只用条件字段（含 AND/OR/NOT 组合）；否则回退全部有值列（旧行为兼容）
        if (!conditionSpecs.isEmpty()) {
            whereClause = buildWhereWithConditions(mappings, params, conditionSpecs, false);
        } else {
            whereClause = buildWhereClause(mappings, params);
        }
        if (whereClause.isEmpty()) throw new RuntimeException("DELETE 必须包含条件字段");
        return "DELETE FROM " + table + " WHERE " + whereClause;
    }

    private boolean containsSpec(List<ConditionSpec> specs, String semanticName) {
        for (ConditionSpec s : specs) {
            if (s.semanticName.equals(semanticName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按条件规格构建 WHERE 子句（支持 AND/OR/NOT 逻辑组合）：
     * - 配置条件按 paramConfig 顺序拼接：expr_0 [link] expr_1 [link] expr_2 ...
     * - NOT(非) 对单个条件取反：NOT (col = value)
     * - includePrimaryKeys=true 时，未在配置条件中的属性级主键补充到末尾（AND 连接）
     */
    private String buildWhereWithConditions(List<ColumnMapping> mappings, Map<String, Object> params,
                                            List<ConditionSpec> conditionSpecs, boolean includePrimaryKeys) {
        List<String> exprs = new ArrayList<>();
        List<String> links = new ArrayList<>();
        // 1. 配置条件（按顺序）
        for (ConditionSpec spec : conditionSpecs) {
            ColumnMapping m = findMapping(mappings, spec.semanticName);
            if (m == null || !params.containsKey(m.semanticName)) {
                continue;
            }
            exprs.add(buildConditionExpr(m, params, spec.operator, spec.negate));
            links.add(spec.link);
        }
        // 2. 补充未配置的属性级主键（AND 连接）
        if (includePrimaryKeys) {
            for (ColumnMapping m : mappings) {
                if (m.primaryKey && !containsSpec(conditionSpecs, m.semanticName)
                        && params.containsKey(m.semanticName)) {
                    exprs.add(buildConditionExpr(m, params, "eq", false));
                    links.add("AND");
                }
            }
        }
        return joinWhere(exprs, links);
    }

    private ColumnMapping findMapping(List<ColumnMapping> mappings, String semanticName) {
        for (ColumnMapping m : mappings) {
            if (m.semanticName.equals(semanticName)) {
                return m;
            }
        }
        return null;
    }

    private String buildConditionExpr(ColumnMapping m, Map<String, Object> params,
                                      String operator, boolean negate) {
        Object value = params.get(m.semanticName);
        String expr;
        switch (normalizeConditionOperator(operator)) {
            case "ne": expr = m.physicalColumn + " <> " + sqlValue(value); break;
            case "gt": expr = m.physicalColumn + " > " + sqlValue(value); break;
            case "ge": expr = m.physicalColumn + " >= " + sqlValue(value); break;
            case "lt": expr = m.physicalColumn + " < " + sqlValue(value); break;
            case "le": expr = m.physicalColumn + " <= " + sqlValue(value); break;
            case "contains":
                expr = m.physicalColumn + " LIKE " + sqlValue("%" + String.valueOf(value) + "%");
                break;
            case "in":
                List<String> values = new ArrayList<>();
                if (value instanceof Collection) {
                    for (Object item : (Collection<?>) value) values.add(sqlValue(item));
                } else {
                    for (String item : String.valueOf(value).split(",")) values.add(sqlValue(item.trim()));
                }
                if (values.isEmpty()) throw new RuntimeException("属于列表条件不能为空: " + m.semanticName);
                expr = m.physicalColumn + " IN (" + String.join(", ", values) + ")";
                break;
            default: expr = m.physicalColumn + " = " + sqlValue(value);
        }
        return negate ? "NOT (" + expr + ")" : expr;
    }

    private String joinWhere(List<String> exprs, List<String> links) {
        if (exprs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(exprs.get(0));
        for (int i = 1; i < exprs.size(); i++) {
            sb.append(" ").append(links.get(i - 1)).append(" ").append(exprs.get(i));
        }
        return sb.toString();
    }

    private String buildWhereClause(List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                if (!first) sb.append(" AND ");
                sb.append(m.physicalColumn).append(" = ").append(sqlValue(params.get(m.semanticName)));
                first = false;
            }
        }
        return sb.toString();
    }

    // ================================================================
    //  Param Config（属性选择 + 目标值配置）
    // ================================================================

    /**
     * 按动作定义的参数配置(PARAM_CONFIG)解析执行参数。
     * 三种目标值模式：
     * - direct:      固定字面值，直接取配置值（正常加引号转义）
     * - placeholder: 引用执行入参，模板形如 ${orderId}
     * - expression:  原生SQL表达式（如 CASE WHEN ... END），拼装时不加引号
     * 未配置 PARAM_CONFIG 的动作保持旧行为：直接使用入参生成SQL。
     */
    private Map<String, Object> applyParamConfig(ActionDO action, Map<String, Object> inputParams,
                                                 String objectKey) {
        return applyParamConfig(action, inputParams, objectKey, action);
    }

    private Map<String, Object> applyParamConfig(ActionDO action, Map<String, Object> inputParams,
                                                 String objectKey, ActionDO conditionSourceAction) {
        String configJson = action.getParamConfig();
        if (configJson == null || configJson.trim().isEmpty()) {
            return inputParams;
        }
        List<Map<String, Object>> configs;
        try {
            configs = objectMapper.readValue(configJson,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("动作参数配置解析失败: " + e.getMessage());
        }
        Map<String, Object> effective = new LinkedHashMap<>();
        for (Map<String, Object> cfg : configs) {
            if (cfg.get("propertyCode") == null) {
                continue;
            }
            String propertyCode = String.valueOf(cfg.get("propertyCode"));
            String valueMode = cfg.get("valueMode") == null ? "direct" : String.valueOf(cfg.get("valueMode"));
            String valueTemplate = cfg.get("valueTemplate") == null ? "" : String.valueOf(cfg.get("valueTemplate"));
            boolean required = Boolean.TRUE.equals(cfg.get("required"))
                    || "true".equalsIgnoreCase(String.valueOf(cfg.get("required")));
            boolean condition = Boolean.TRUE.equals(cfg.get("condition"))
                    || "true".equalsIgnoreCase(String.valueOf(cfg.get("condition")));
            switch (valueMode) {
                case "direct":
                    effective.put(propertyCode, valueTemplate);
                    break;
                case "placeholder": {
                    String paramName = stripPlaceholder(valueTemplate);
                    String conditionSourceProperty = paramName.isEmpty() ? propertyCode : paramName;
                    // 对象绑定条件的值必须来自 objectKey 对应的当前对象，不能信任前端手填值。
                    // 例如条件字段选择“订单编号”，配置自动为 ${orderNo}，实际执行时重新读取
                    // object.orderNo；提交预览和 Worker 真正执行都会各读取一次。
                    Object v = condition && objectKey != null && !objectKey.trim().isEmpty()
                            ? objectStateConditionResolver.resolve("object." + conditionSourceProperty,
                                    conditionSourceAction, objectKey)
                            : inputParams.get(paramName);
                    if (v == null && required) {
                        throw new RuntimeException("缺少必填执行参数: " + paramName);
                    }
                    if (v != null) {
                        effective.put(propertyCode, v);
                    }
                    break;
                }
                case "expression":
                    if (valueTemplate.trim().isEmpty()) {
                        throw new RuntimeException("表达式目标值不能为空: " + propertyCode);
                    }
                    effective.put(propertyCode, new RawExpression(valueTemplate));
                    break;
                case "relative": {
                    if (!"UPDATE".equals(action.getActionType())) {
                        throw new RuntimeException("当前值运算仅支持 UPDATE 动作: " + propertyCode);
                    }
                    String relativeOperator = "ADD".equalsIgnoreCase(String.valueOf(cfg.get("relativeOperator")))
                            ? "ADD" : "SUBTRACT";
                    Object operand;
                    String trimmed = valueTemplate.trim();
                    if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
                        String paramName = stripPlaceholder(trimmed);
                        operand = inputParams.get(paramName);
                        if (operand == null) {
                            throw new RuntimeException("缺少当前值运算参数: " + paramName);
                        }
                        try {
                            operand = new BigDecimal(String.valueOf(operand).trim());
                        } catch (Exception e) {
                            throw new RuntimeException("当前值运算参数必须是数字: " + paramName);
                        }
                    } else {
                        try {
                            operand = new BigDecimal(trimmed);
                        } catch (Exception e) {
                            throw new RuntimeException("当前值运算只支持数字或入参占位符: " + valueTemplate);
                        }
                    }
                    effective.put(propertyCode, new CurrentValueExpression(relativeOperator, operand));
                    break;
                }
                default:
                    throw new RuntimeException("不支持的目标值模式: " + valueMode);
            }
        }
        // 配置未覆盖的入参向后兼容合并（如主键条件）
        for (Map.Entry<String, Object> en : inputParams.entrySet()) {
            effective.putIfAbsent(en.getKey(), en.getValue());
        }
        return effective;
    }

    private String stripPlaceholder(String template) {
        String t = template.trim();
        if (t.startsWith("${") && t.endsWith("}")) {
            return t.substring(2, t.length() - 1).trim();
        }
        return t;
    }

    /**
     * SQL 值渲染：RawExpression 原样输出，其余加引号并转义。
     */
    private String sqlValue(Object val) {
        if (val instanceof RawExpression) {
            return ((RawExpression) val).expr;
        }
        return "'" + escape(val) + "'";
    }

    private String sqlAssignmentValue(ColumnMapping mapping, Object val) {
        if (val instanceof CurrentValueExpression) {
            CurrentValueExpression expression = (CurrentValueExpression) val;
            String operator = "ADD".equals(expression.operator) ? "+" : "-";
            return mapping.physicalColumn + " " + operator + " " + sqlValue(expression.operand);
        }
        return sqlValue(val);
    }

    /**
     * 原生SQL表达式值：仅允许出现在 PARAM_CONFIG 的 expression 模式中。
     */
    private static class RawExpression {
        final String expr;
        RawExpression(String expr) { this.expr = expr; }
        @Override public String toString() { return expr; }
    }

    // ================================================================
    //  Dry-run & Snapshot
    // ================================================================

    /**
     * 预演执行：提交阶段仅预览，不得修改业务数据。
     * SELECT 直接查询返回行；DML 在事务内执行后立即回滚，只返回影响行数。
     * 真正落库只允许发生在 approve 之后的 executeExecution 阶段。
     */
    private Map<String, Object> executeDryRun(Long conceptId, String sql, String actionType) {
        DbQuery dbQuery = null;
        Connection con = null;
        try {
            dbQuery = getDbQuery(conceptId);
            if ("SELECT".equals(actionType)) {
                List<Map<String, Object>> rows = dbQuery.queryList(sql);
                return Collections.singletonMap("rows", rows);
            }
            // DML 在事务内执行后回滚，仅预览影响行数，业务库纹丝不动
            con = dbQuery.getConnection();
            con.setAutoCommit(false);
            int affected;
            try (Statement st = con.createStatement()) {
                affected = st.executeUpdate(sql);
            }
            con.rollback();
            return Collections.singletonMap("affectedRows", affected);
        } catch (Exception e) {
            log.warn("dry-run 异常: {}", e.getMessage());
            if (con != null) {
                try { con.rollback(); } catch (Exception rbEx) { log.debug("dry-run 回滚失败", rbEx); }
            }
            return Collections.singletonMap("error", e.getMessage());
        } finally {
            if (con != null) {
                try { con.close(); } catch (Exception cEx) { log.debug("dry-run 连接关闭失败", cEx); }
            }
            closeDbQuery(dbQuery);
        }
    }

    private String captureSnapshot(Long conceptId, String sql) {
        DbQuery dbQuery = null;
        try {
            dbQuery = getDbQuery(conceptId);
            String selectSql = convertToSelect(sql);
            if (selectSql != null) {
                List<Map<String, Object>> rows = dbQuery.queryList(selectSql);
                return toJson(rows);
            }
        } catch (Exception e) {
            log.warn("快照获取失败: {}", e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
        return null;
    }

    /** UPDATE 专用的安全相对运算：目标列当前值 +/- 数字或已解析的执行入参。 */
    private static class CurrentValueExpression {
        final String operator;
        final Object operand;
        CurrentValueExpression(String operator, Object operand) {
            this.operator = operator;
            this.operand = operand;
        }
        @Override public String toString() { return operator + " " + operand; }
    }

    private String convertToSelect(String dmlSql) {
        String upper = dmlSql.trim().toUpperCase();
        if (upper.startsWith("UPDATE")) {
            int setIdx = upper.indexOf(" SET ");
            int whereIdx = upper.indexOf(" WHERE ", setIdx);
            String fromTable = dmlSql.substring(6, setIdx).trim();
            String where = whereIdx > 0 ? dmlSql.substring(whereIdx) : "";
            return "SELECT * FROM " + fromTable + " " + where;
        }
if (upper.startsWith("DELETE")) {
            int whereIdx = upper.indexOf(" WHERE ");
            String fromTable = dmlSql.substring(7, whereIdx > 0 ? whereIdx : dmlSql.length()).trim();
            String where = whereIdx > 0 ? dmlSql.substring(whereIdx) : "";
            return "SELECT * FROM " + fromTable + " " + where;
        }
        return null;
    }

    /**
     * 捕获执行后的数据快照（可溯源回退）：
     * UPDATE：复用 convertToSelect 重查变更后的新值；
     * DELETE：convertToSelect 重查为空集（行已删，beforeData 保留用于回退=按 beforeData 重 INSERT）；
     * CREATE：INSERT 无 WHERE 可转换，按主键参数构造 SELECT 回查新插入行。
     */
    private String captureAfterData(ActionDO action, String sql, Map<String, Object> params) {
        DbQuery dbQuery = null;
        try {
            dbQuery = getDbQuery(action.getConceptId());
            String selectSql = convertToSelect(sql);
            if (selectSql == null && "CREATE".equals(action.getActionType())) {
                selectSql = buildSelectByPk(action, params);
            }
            if (selectSql != null) {
                List<Map<String, Object>> rows = dbQuery.queryList(selectSql);
                return toJson(rows);
            }
        } catch (Exception e) {
            log.warn("执行后快照获取失败: {}", e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
        return null;
    }

    /**
     * CREATE 动作执行后按主键参数回查新插入行。
     */
    private String buildSelectByPk(ActionDO action, Map<String, Object> params) {
        Long conceptId = action.getConceptId();
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            return null;
        }
        ConceptTableDO table = tables.get(0);
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, table.getId());
        List<String> whereParts = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (m.primaryKey && params.containsKey(m.semanticName)) {
                whereParts.add(m.physicalColumn + " = " + sqlValue(params.get(m.semanticName)));
            }
        }
        if (whereParts.isEmpty()) {
            return null;
        }
        return "SELECT * FROM " + table.getTableName() + " WHERE " + String.join(" AND ", whereParts);
    }

    /**
     * 动作血缘写入（可插拔，静默降级）：
     * LINEAGE_ENABLED=false 时 lineageDataService 为 null 直接跳过；
     * 写入失败仅记 warn，绝不影响动作执行主流程。
     */
    private void writeActionLineageSilently(ActionExecutionDO exec, ActionDO action) {
        if (lineageDataService == null) {
            return;
        }
        try {
            List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
            if (tables.isEmpty()) {
                return;
            }
            ConceptTableDO table = tables.get(0);
            String hostPort = null;
            if (table.getDatasourceId() != null) {
                DatasourceRespDTO ds = datasourceApiService.getDatasourceById(table.getDatasourceId());
                if (ds != null) {
                    hostPort = ds.getIp() + ":" + ds.getPort();
                }
            }
            // 1. 数据/决策共用：upsert 语义对象节点（数据维度 MATERIALIZES 由对象实例层维护，此处保证节点存在供决策关联）
            ConceptDO concept = conceptMapper.selectById(action.getConceptId());
            ObjectNode objectNode = ObjectNode.builder()
                    .objectId(action.getConceptId())
                    .ontologyId(exec.getOntologyId())
                    .conceptId(action.getConceptId())
                    .conceptCode(concept == null ? null : concept.getCode())
                    .conceptName(concept == null ? null : concept.getName())
                    .tableName(table.getTableName())
                    .datasourceHostPort(hostPort)
                    .dbName(table.getDatabaseName())
                    .sid(table.getSchemaName())
                    .build();
            lineageDataService.saveObject(objectNode);
            // 2. 决策维度：ActionExecution -[DECISION_ACTION]-> Object
            ActionExecutionNode node = ActionExecutionNode.builder()
                    .executionId(exec.getId())
                    .actionId(action.getId())
                    .actionName(action.getName())
                    .actionType(action.getActionType())
                    .tableName(table.getTableName())
                    .datasourceHostPort(hostPort)
                    .status(exec.getStatus())
                    .executeTime(exec.getExecuteTime())
                    .objectRels(Collections.singletonList(ObjectDecisionRel.builder()
                            .executionId(exec.getId())
                            .actionId(action.getId())
                            .object(objectNode).build()))
                    .build();
            lineageDataService.saveActionExecution(node);
        } catch (Exception e) {
            log.warn("动作血缘写入失败，不影响执行结果: {}", e.getMessage());
        }
    }

    // ================================================================
    //  Utilities
    // ================================================================

    /**
     * 按概念解析其绑定物理表的数据源并创建 DbQuery。
     * 修复：原实现硬编码平台主库连接且忽略入参，导致所有动作都打到错误数据库。
     * 现依据 ONT_CONCEPT_TABLE.DATASOURCE_ID 经 IDatasourceApiService 解析真实数据源，
     * 由 DataSourceFactory 按方言构建连接（支持 MySQL/Oracle/PG/达梦等多种类型）。
     */
    private DbQuery getDbQuery(Long conceptId) {
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        Long datasourceId = tables.get(0).getDatasourceId();
        if (datasourceId == null) {
            throw new RuntimeException("概念绑定的物理表缺少数据源配置: conceptId=" + conceptId);
        }
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(datasourceId);
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + datasourceId);
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        return dataSourceFactory.createDbQuery(property);
    }

    private void closeDbQuery(DbQuery dbQuery) {
        if (dbQuery != null) {
            try {
                dbQuery.close();
            } catch (Exception e) {
                log.debug("关闭 DbQuery 失败", e);
            }
        }
    }

    /**
     * 动作提交/执行前的权限校验（复用资产治理跨模块 API，与 AI 问数同一链路）。
     * 依据概念绑定物理表的 DATASOURCE_ID + TABLE_NAME 定位注册资产，
     * 由 checkTableAccess 完成表级（AST_ASSET_SPACE_REL，含审批放行回退）与
     * 字段级（AST_ASSET_COLUMN_SPACE_REL 允许列）校验。
     * 增删改（CREATE/UPDATE/DELETE）额外传入涉及列，由治理侧按写操作严格模式校验：
     * 任一涉及列未授权即整体拒绝；查询（SELECT）保持原有表级校验，不传列。
     * 空间上下文 spaceId/spaceCode 由前端拦截器自动注入请求（执行阶段复用提交快照）；
     * 二者均缺失时沿用平台既有语义：跳过空间权限校验。
     */
    private void assertTableAccess(ActionDO action, Long spaceId, String spaceCode,
                                   List<String> columns, String actionType) {
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
        if (tables.isEmpty()) {
            return; // 概念未绑定物理表，后续 generateSql 会给出明确报错
        }
        ConceptTableDO table = tables.get(0);
        if (table.getDatasourceId() == null || table.getTableName() == null) {
            return;
        }
        AssetsTableGovernanceReqDTO reqDTO = new AssetsTableGovernanceReqDTO();
        reqDTO.setDatasourceId(table.getDatasourceId());
        reqDTO.setTableName(table.getTableName());
        reqDTO.setSpaceId(spaceId);
        reqDTO.setSpaceCode(spaceCode);
        reqDTO.setEntrance(entranceOf(actionType));
        if (columns != null && !columns.isEmpty()) {
            reqDTO.setColumnNames(columns);
        }
        tableGovernanceApiService.checkTableAccess(reqDTO);
    }

    /**
     * 提取增删改动作涉及的物理列（与 SQL 生成逻辑一一对应）：
     * - CREATE：入参覆盖的映射列 → INSERT 目标列
     * - UPDATE：入参覆盖的映射列 → SET 列 + 主键 WHERE 列
     * - DELETE：入参覆盖的映射列 → WHERE 条件列
     * 查询（SELECT）返回空列表，维持表级校验语义。
     */
    private List<String> extractActionColumns(ActionDO action, Map<String, Object> params) {
        String actionType = action.getActionType();
        if (!"CREATE".equals(actionType) && !"UPDATE".equals(actionType) && !"DELETE".equals(actionType)) {
            return Collections.emptyList();
        }
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
        if (tables.isEmpty()) {
            return Collections.emptyList();
        }
        ConceptTableDO table = tables.get(0);
        List<ColumnMapping> mappings = resolveColumnMappings(action.getConceptId(), table.getId());
        List<String> columns = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                columns.add(m.physicalColumn);
            }
        }
        return columns;
    }

    /** 动作类型 → 治理 API 入口标识（写操作走严格模式，查询维持原 entrance） */
    private String entranceOf(String actionType) {
        switch (actionType) {
            case "CREATE": return ENTRANCE_ONTOLOGY_CREATE;
            case "UPDATE": return ENTRANCE_ONTOLOGY_UPDATE;
            case "DELETE": return ENTRANCE_ONTOLOGY_DELETE;
            default: return ENTRANCE_ONTOLOGY_ACTION;
        }
    }

    private ActionExecutionDO getExecutionOrThrow(Long id) {
        ActionExecutionDO exec = executionMapper.selectById(id);
        if (exec == null) throw new RuntimeException("执行记录不存在: " + id);
        return exec;
    }

    private Map<String, Object> parseParams(String inputParams) {
        if (inputParams == null || inputParams.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(inputParams, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("参数解析失败: " + e.getMessage());
        }
    }

    /**
     * 函数类型动作：把 param_config 中「字段映射型入参」（kind=field）解析并注入到执行参数。
     * 每条映射 {paramName, sourcePropertyCode} 使 input[paramName] = 数据来源概念属性 code，
     * 脚本通过入参名动态引用要处理的字段；固定值入参（kind=value/缺省）保持用户输入值。
     * 返回合并后的 inputParams JSON 字符串。
     */
    private String applyFunctionParamMapping(ActionDO action, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        if (action.getParamConfig() == null || action.getParamConfig().isEmpty()) {
            return toJson(params);
        }
        try {
            List<Map<String, Object>> cfg = objectMapper.readValue(action.getParamConfig(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> entry : cfg) {
                String kind = entry.get("kind") == null ? "" : String.valueOf(entry.get("kind"));
                if (!"field".equals(kind)) {
                    continue; // 仅处理字段映射型，固定值型保留用户输入
                }
                Object paramName = entry.get("paramName");
                Object sourcePropertyCode = entry.get("sourcePropertyCode");
                if (paramName == null || sourcePropertyCode == null) {
                    continue;
                }
                params.put(String.valueOf(paramName), String.valueOf(sourcePropertyCode));
            }
        } catch (Exception e) {
            log.warn("解析函数动作 param_config 失败：{}", action.getParamConfig(), e);
        }
        return toJson(params);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private Object escape(Object val) {
        if (val == null) return "NULL";
        return val.toString().replace("'", "''");
    }
}
