package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ai.service.skill.IAiModelGatewayService;
import com.datamaster.module.ontology.controller.admin.action.vo.AiActionDecisionReqVO;
import com.datamaster.module.ontology.controller.admin.action.vo.AiActionDecisionRespVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.service.IActionApprovalService;
import com.datamaster.module.ontology.service.IAiActionDecisionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 动作决策实现。
 *
 * <p>这里是“决策层”，不是执行器：模型只能返回候选动作 ID 和 JSON 参数。
 * 选中的动作仍然经过动作前置条件、引用值解析、字段权限、dry-run、审批和 Worker 执行。</p>
 */
@Service
@Validated
public class AiActionDecisionServiceImpl implements IAiActionDecisionService {

    private static final Logger log = LoggerFactory.getLogger(AiActionDecisionServiceImpl.class);
    private static final String DECISION_ALLOW = "ALLOW";
    private static final String DECISION_ASK_HUMAN = "ASK_HUMAN";
    private static final String DECISION_REJECT = "REJECT";

    @Resource
    private ActionMapper actionMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private IAiModelGatewayService aiModelGatewayService;
    @Resource
    private IActionApprovalService approvalService;

    @Override
    public AiActionDecisionRespVO decide(AiActionDecisionReqVO reqVO) {
        List<ActionDO> actions = actionMapper.selectByOntologyId(reqVO.getOntologyId());
        if (actions == null || actions.isEmpty()) {
            return result(DECISION_ASK_HUMAN, null, null, null,
                    "当前本体没有可执行动作，请先配置动作", 0D, true,
                    Collections.singletonList("action"));
        }
        if (aiModelGatewayService == null || !aiModelGatewayService.available()) {
            return result(DECISION_ASK_HUMAN, null, null, normalizeInput(reqVO.getInputParams()),
                    "AI 服务当前不可用，请人工选择动作", 0D, true,
                    Collections.singletonList("action"));
        }

        String reply;
        try {
            reply = aiModelGatewayService.complete(buildSystemPrompt(), buildUserPrompt(reqVO, actions));
        } catch (Exception e) {
            log.warn("AI 动作决策调用失败: {}", e.getMessage());
            return result(DECISION_ASK_HUMAN, null, null, normalizeInput(reqVO.getInputParams()),
                    "AI 决策调用失败，请人工选择动作", 0D, true,
                    Collections.singletonList("action"));
        }

        JsonNode decision = extractJson(reply);
        if (decision == null) {
            return result(DECISION_ASK_HUMAN, null, null, normalizeInput(reqVO.getInputParams()),
                    "AI 未返回合法结构化决策，请人工确认", 0D, true,
                    Collections.singletonList("action"));
        }

        String decisionType = normalizeDecision(decision.path("decision").asText(null));
        Long actionId = decision.hasNonNull("actionId") ? decision.get("actionId").asLong() : null;
        ActionDO selected = findAction(actions, actionId);
        double confidence = decision.has("confidence") ? decision.path("confidence").asDouble(0D) : 0D;
        confidence = Math.max(0D, Math.min(1D, confidence));
        String reason = StringUtils.defaultIfBlank(decision.path("reason").asText(null), "AI 未提供决策理由");
        List<String> missing = readStringList(decision.path("missingFields"));
        String aiParams = normalizeInputNode(decision.get("inputParams"));

        if (selected == null) {
            return result(DECISION_ASK_HUMAN, null, null, aiParams,
                    "AI 选择的动作不在当前本体候选列表中，请人工选择", confidence, true,
                    Collections.singletonList("action"));
        }
        if (StringUtils.isBlank(aiParams) || "{}".equals(aiParams)) {
            aiParams = normalizeInput(reqVO.getInputParams());
        }
        if (containsUnsafeInstruction(aiParams)) {
            return result(DECISION_REJECT, selected.getId(), selected.getName(), aiParams,
                    "AI 参数包含不允许的 SQL 或脚本内容，已拒绝执行", confidence, true, missing);
        }
        if (!DECISION_ALLOW.equals(decisionType) && !DECISION_REJECT.equals(decisionType)) {
            decisionType = DECISION_ASK_HUMAN;
        }
        if (!missing.isEmpty() || confidence < 0.80D) {
            decisionType = DECISION_ASK_HUMAN;
        }

        String criteriaResult = null;
        try {
            criteriaResult = approvalService.evaluateCriteria(selected, aiParams, reqVO.getObjectKey());
            if (!isCriteriaPassed(criteriaResult)) {
                decisionType = DECISION_REJECT;
                reason = "动作提交前置条件未通过";
            }
        } catch (Exception e) {
            log.warn("AI 决策动作前置条件求值失败 actionId={}: {}", selected.getId(), e.getMessage());
            decisionType = DECISION_ASK_HUMAN;
            reason = "动作前置条件无法自动确认，请人工处理";
        }

        AiActionDecisionRespVO response = result(decisionType, selected.getId(), selected.getName(), aiParams,
                reason, confidence, !DECISION_ALLOW.equals(decisionType), missing);
        response.setCriteriaResult(criteriaResult);

        return response;
    }

    private String buildSystemPrompt() {
        return "你是 DataMaster 本体动作决策器。只能从候选动作中选择一个已存在的 actionId，" +
                "只能返回 JSON，不得生成 SQL、脚本或新动作。" +
                "如果意图、参数、引用对象不完整，decision 必须为 ASK_HUMAN；" +
                "只有动作和参数都明确时才可返回 ALLOW；明确不应执行时返回 REJECT。" +
                "JSON 格式：{decision:'ALLOW|ASK_HUMAN|REJECT',actionId:number|null," +
                "inputParams:object,reason:string,confidence:number,missingFields:string[]}.";
    }

    private String buildUserPrompt(AiActionDecisionReqVO reqVO, List<ActionDO> actions) {
        ArrayNode candidates = objectMapper.createArrayNode();
        for (ActionDO action : actions) {
            ObjectNode node = candidates.addObject();
            node.put("actionId", action.getId());
            node.put("name", action.getName());
            node.put("actionType", action.getActionType());
            node.put("conceptId", action.getConceptId());
            node.put("description", StringUtils.defaultString(action.getDescription()));
            node.put("paramConfig", StringUtils.defaultString(action.getParamConfig()));
            node.put("executionSteps", StringUtils.defaultString(action.getExecutionSteps()));
            node.put("needsApproval", Boolean.TRUE.equals(action.getNeedsApproval()));
            node.put("approvalLevels", action.getApprovalLevels() == null ? 0 : action.getApprovalLevels());
        }
        return "用户意图：" + reqVO.getPrompt() + "\n" +
                "触发对象主键：" + StringUtils.defaultString(reqVO.getObjectKey()) + "\n" +
                "已有上下文参数：" + normalizeInput(reqVO.getInputParams()) + "\n" +
                "候选动作：" + candidates.toString();
    }

    private ActionDO findAction(List<ActionDO> actions, Long id) {
        if (id == null) return null;
        for (ActionDO action : actions) {
            if (id.equals(action.getId())) return action;
        }
        return null;
    }

    private AiActionDecisionRespVO result(String decision, Long actionId, String actionName,
                                           String inputParams, String reason, double confidence,
                                           boolean requiresHuman, List<String> missingFields) {
        AiActionDecisionRespVO result = new AiActionDecisionRespVO();
        result.setDecision(decision);
        result.setActionId(actionId);
        result.setActionName(actionName);
        result.setInputParams(inputParams);
        result.setReason(reason);
        result.setConfidence(confidence);
        result.setRequiresHuman(requiresHuman);
        result.setMissingFields(missingFields == null ? new ArrayList<>() : missingFields);
        return result;
    }

    private JsonNode extractJson(String reply) {
        if (StringUtils.isBlank(reply)) return null;
        String candidate = reply.trim();
        int fenceStart = candidate.indexOf("```json");
        if (fenceStart >= 0) {
            int start = fenceStart + 7;
            int end = candidate.indexOf("```", start);
            if (end > start) candidate = candidate.substring(start, end);
        }
        int start = candidate.indexOf('{');
        int end = candidate.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            return objectMapper.readTree(candidate.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeDecision(String value) {
        if (value == null) return DECISION_ASK_HUMAN;
        String normalized = value.trim().toUpperCase();
        return DECISION_ALLOW.equals(normalized) || DECISION_REJECT.equals(normalized)
                ? normalized : DECISION_ASK_HUMAN;
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && item.isValueNode() && StringUtils.isNotBlank(item.asText())) values.add(item.asText());
        }
        return values;
    }

    private String normalizeInput(String input) {
        if (StringUtils.isBlank(input)) return "{}";
        try {
            JsonNode node = objectMapper.readTree(input);
            return normalizeInputNode(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String normalizeInputNode(JsonNode node) {
        if (node == null || !node.isObject()) return "{}";
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean containsUnsafeInstruction(String json) {
        String text = StringUtils.defaultString(json).toLowerCase();
        return text.contains("select ") || text.contains("insert ") || text.contains("update ")
                || text.contains("delete ") || text.contains("drop ") || text.contains("script")
                || text.contains("__sql") || text.contains("javascript");
    }

    private boolean isCriteriaPassed(String criteriaResult) {
        if (StringUtils.isBlank(criteriaResult)) return true;
        try {
            JsonNode node = objectMapper.readTree(criteriaResult);
            return !node.has("passed") || node.path("passed").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }
}
