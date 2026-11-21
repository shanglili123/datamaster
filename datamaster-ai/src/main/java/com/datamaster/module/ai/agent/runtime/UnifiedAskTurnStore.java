package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Request results belong to an authenticated conversation, not to model or browser history. */
@Component
final class UnifiedAskTurnStore {
    private static final String STEP = "统一问数";
    private static final String PREFIX = "ASK_UNIFIED_";
    private final IAiAgentOperationService operations;

    private final com.datamaster.module.ai.dal.mapper.agent.AiAgentOperationMapper mapper;

    UnifiedAskTurnStore(IAiAgentOperationService operations,
                        com.datamaster.module.ai.dal.mapper.agent.AiAgentOperationMapper mapper) {
        this.operations = operations;
        this.mapper = mapper;
    }

    AgentRunResponse previous(AskUnifiedReqVO request, AgentToolContext context) {
        AgentRunResponse combined = new AgentRunResponse();
        // Carry the latest formal query set and proposal independently.
        // A report or definition-only turn must not erase either reference.
        for (AiAgentOperationDO prior : mapper.selectRecentUnifiedTurns(key(request), context.getUserId())) {
            AgentRunResponse saved = read(prior, request, context, AgentToolResult.SUCCEEDED, "PARTIAL");
            if (saved == null) continue;
            if (combined.getToolResults().isEmpty()) combined.setToolResults(saved.getToolResults());
            if (combined.getDecision() == null) combined.setDecision(saved.getDecision());
            if (!combined.getToolResults().isEmpty() && combined.getDecision() != null) break;
        }
        return combined.getToolResults().isEmpty() && combined.getDecision() == null ? null : combined;
    }

    boolean ownsReference(String id) {
        return id != null && id.matches("ASK_UNIFIED_[0-9a-fA-F-]{36}");
    }

    void restoreInput(AskUnifiedReqVO request, AgentToolContext context) {
        AiAgentOperationDO prior = operations.get(request.getContinuationOperationId());
        if (read(prior, request, context, AgentToolResult.NEED_INPUT) == null)
            throw new com.datamaster.common.exception.ServiceException("待补充任务不属于当前问数上下文");
        AskUnifiedReqVO original = JSON.parseObject(prior.getRequestJson(), AskUnifiedReqVO.class);
        if (!Objects.equals(original.getObjectKey(), request.getObjectKey()))
            throw new com.datamaster.common.exception.ServiceException("补充条件不能变更原任务绑定的业务对象");
        request.setQuestion(AskInputContinuation.question(prior.getGoal(), request.getQuestion()));
        request.setContinuationOperationId(null);
    }

    AgentRunResponse explicitAnalysis(AskUnifiedReqVO request, AgentToolContext context) {
        AgentRunResponse result = read(operations.get(request.getAnalysisOperationId()), request, context, AgentToolResult.SUCCEEDED, "PARTIAL");
        if (result == null || result.getToolResults().isEmpty())
            throw new com.datamaster.common.exception.ServiceException("当前会话没有可重新查询的原分析条件");
        return result;
    }

    private AgentRunResponse read(AiAgentOperationDO prior, AskUnifiedReqVO request, AgentToolContext context, String... statuses) {
        if (prior == null || !ownsReference(prior.getId()) || Boolean.TRUE.equals(prior.getDelFlag())
                || !STEP.equals(prior.getCurrentStep()) || !java.util.Arrays.asList(statuses).contains(prior.getStatus())
                || !Objects.equals(context.getUserId(), prior.getCreatorId())
                || !Objects.equals(context.getSpaceId(), prior.getSpaceId())
                || !Objects.equals(context.getSpaceCode(), prior.getSpaceCode())
                || !Objects.equals(request.getOntologyId(), prior.getOntologyId())
                || !Objects.equals(request.getDatasourceId(), prior.getDatasourceId())) return null;
        try {
            AskUnifiedReqVO stored = JSON.parseObject(prior.getRequestJson(), AskUnifiedReqVO.class);
            if (stored == null || !Objects.equals(request.getSessionId(), stored.getSessionId())
                    || !Objects.equals(request.getConversationId(), stored.getConversationId())
                    || !Objects.equals(request.getAssetId(), stored.getAssetId())) return null;
            AgentRunResponse result = JSON.parseObject(prior.getResultJson(), AgentRunResponse.class);
            if (result == null || !prior.getId().equals(result.getOperationId()) || result.getToolResults() == null) return null;
            // Reject a malformed reference set rather than silently renumbering its entries.
            if (result.getToolResults().stream().anyMatch(reference -> replayReference(reference, prior.getId()) == null)) return null;
            return result;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    AgentToolContext begin(AskUnifiedReqVO request, AgentToolContext caller) {
        AiAgentOperationDO operation = new AiAgentOperationDO();
        operation.setId(PREFIX + UUID.randomUUID());
        operation.setCurrentStep(STEP);
        operation.setFlowKey(key(request) + ":" + operation.getId());
        operation.setStatus(AgentToolResult.RUNNING);
        operation.setGoal(request.getQuestion());
        operation.setCreatorId(caller.getUserId());
        operation.setCreateBy(caller.getUserName());
        operation.setSpaceId(caller.getSpaceId());
        operation.setSpaceCode(caller.getSpaceCode());
        operation.setOntologyId(request.getOntologyId());
        operation.setDatasourceId(request.getDatasourceId());
        AskUnifiedReqVO identity = new AskUnifiedReqVO();
        identity.setSessionId(request.getSessionId());
        identity.setConversationId(request.getConversationId());
        identity.setAssetId(request.getAssetId());
        identity.setObjectKey(request.getObjectKey());
        operation.setRequestJson(JSON.toJSONString(identity));
        operation.setCreateTime(new Date());
        operation.setUpdateTime(operation.getCreateTime());
        operation.setDelFlag(false);
        operations.create(operation);
        Map<String, Object> attributes = new java.util.LinkedHashMap<>(caller.getAttributes());
        attributes.put("confirmed", false);
        attributes.put(com.datamaster.module.ai.agent.tool.AskTimeScope.GOAL, request.getQuestion());
        return new AgentToolContext(operation.getId(), caller.getUserId(), caller.getUserName(),
                caller.getSpaceId(), caller.getSpaceCode(), attributes);
    }

    void finish(AgentRunResponse response) {
        AiAgentOperationDO operation = new AiAgentOperationDO();
        operation.setId(response.getOperationId());
        operation.setStatus(response.getStatus());
        // Store only executable query references and proposal identity for subsequent turns.
        AgentRunResponse references = new AgentRunResponse();
        references.setOperationId(response.getOperationId());
        for (AgentToolResult result : response.getToolResults()) {
            AgentToolResult reference = replayReference(result, response.getOperationId());
            if (reference != null) references.getToolResults().add(reference);
        }
        if (response.getDecision() != null) {
            var identity = new com.datamaster.module.ai.agent.decision.DecisionModels.Proposal();
            identity.setId(response.getDecision().getId());
            references.setDecision(identity);
        }
        operation.setResultJson(JSON.toJSONString(references));
        operation.setUpdateTime(new Date());
        operations.update(operation);
    }

    static AgentToolResult replayReference(AgentToolResult result, String operationId) {
        if (result == null || !Objects.equals(operationId, result.getOperationId())
                || !UnifiedAskAgentService.isMetricEvidence(result)
                || !(result.getData() instanceof Map<?, ?> data)) return null;
        String key = "compare_periods".equals(result.getToolName()) ? "comparisonDefinition" : "queryDefinition";
        Object definition = data.get(key);
        if (definition == null) return null;
        Map<String, Object> referenceData = new java.util.LinkedHashMap<>();
        referenceData.put(key, definition);
        for (String field : java.util.List.of("metricName", "startTime", "endTime")) {
            if (data.get(field) instanceof String value) referenceData.put(field, value);
        }
        AgentToolResult reference = AgentToolResult.of(AgentToolResult.SUCCEEDED, referenceData,
                "可按原条件重新查询", operationId);
        reference.setToolName(result.getToolName());
        return reference;
    }

    private static String key(AskUnifiedReqVO request) {
        // Keep the lookup key short even for a maximum-length conversation ID.
        String data = JSON.toJSONString(java.util.Arrays.asList(request.getSpaceId(), request.getOntologyId(),
                request.getDatasourceId(), request.getAssetId(), request.getSessionId(), request.getConversationId()));
        return "ASK_UNIFIED:" + UUID.nameUUIDFromBytes(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
