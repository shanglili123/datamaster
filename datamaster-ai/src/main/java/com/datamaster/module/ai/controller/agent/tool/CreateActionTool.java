package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionSaveReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import com.datamaster.module.ontology.controller.admin.function.vo.FunctionSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.service.IFunctionService;
import com.datamaster.module.ontology.service.IActionService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Locale;

/** 直接保存动作定义；AI 生成动作草稿使用 generate_actions。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class CreateActionTool {
    @Resource private IActionService actionService;
    @Resource private IFunctionService functionService;
    @Resource private AgentToolResourceGuard resourceGuard;
    @Resource private AgentActionPreviewStore previewStore;
    @Resource private IAiAgentOperationService operationService;
    @Resource private ConceptMapper conceptMapper;
    @Resource private RelationMapper relationMapper;

    @Transactional(rollbackFor = Exception.class)
    @Tool(name = "create_action", value = "保存一个新的动作定义；如果来自 generate_actions 预览，确认阶段只传 ontologyId、previewIndex 和必要的审批参数，工具会按预览自动解析概念/关系 ID；创建动作需要用户确认")
    public AgentToolResult execute(AgentToolRequest.CreateAction input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (input == null || blank(input.getOntologyId()))
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "创建动作需要 ontologyId", operationId(context));
        Long ontologyId = AgentToolIds.longValue(input.getOntologyId());
        if (!resourceGuard.ontologyVisible(ontologyId, context)) return AgentToolResult.failure("本体不属于当前空间，拒绝创建动作");

        AiActionPreviewRespVO.GeneratedActionPreview preview = resolvePreview(input, context);
        String name = text(input.getName());
        String actionType = text(input.getActionType());
        if (preview == null && (blank(name) || blank(actionType))) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "创建动作需要 name 和 actionType；如果来自动作预览，请提供有效的 previewIndex",
                    operationId(context));
        }
        String description = input.getDescription();
        String executionSteps = input.getExecutionSteps();
        String functionBody = input.getFunctionBody();
        String functionLang = input.getFunctionLang();
        String functionParams = input.getFunctionParams();
        String sourceConceptId = input.getSourceConceptId();
        String sourceRelationIds = input.getSourceRelationIds() == null
                ? null : JSON.toJSONString(input.getSourceRelationIds());
        String outputConceptId = input.getOutputConceptId();
        Integer readLimit = input.getReadLimit();
        Long conceptId = AgentToolIds.longValue(input.getConceptId());

        if (preview != null) {
            name = blank(preview.getActionName()) ? name : preview.getActionName().trim();
            actionType = blank(preview.getActionType()) ? actionType : preview.getActionType().trim().toUpperCase(Locale.ROOT);
            description = preview.getActionDescription();
            conceptId = resolveConceptId(ontologyId, preview.getTriggerConceptCode(), conceptId);
            executionSteps = preview.getExecutionSteps();
            functionBody = preview.getFunctionBody();
            functionLang = preview.getFunctionLang();
            functionParams = preview.getFunctionParams();
            sourceConceptId = resolveConceptIdAsString(ontologyId, preview.getSourceConceptCode(), sourceConceptId);
            outputConceptId = resolveConceptIdAsString(ontologyId, preview.getOutputConceptCode(), outputConceptId);
        }

        ActionSaveReqVO request = new ActionSaveReqVO();
        request.setOntologyId(ontologyId);
        request.setName(name);
        request.setActionType(actionType);
        request.setConceptId(conceptId);
        request.setDescription(description);
        request.setNeedsApproval(input.getNeedsApproval() == null ? Boolean.TRUE : input.getNeedsApproval());
        request.setParamConfig(input.getParamConfig());
        request.setSourceConceptId(AgentToolIds.longValue(sourceConceptId));
        request.setSourceRelationIds(sourceRelationIds);
        request.setOutputConceptId(AgentToolIds.longValue(outputConceptId));
        request.setReadLimit(readLimit);

        if ("FUNCTION".equalsIgnoreCase(actionType)) {
            Long functionId = AgentToolIds.longValue(input.getFunctionId());
            if (functionId == null) {
                if (blank(functionBody) || blank(functionLang)) {
                    return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                            "FUNCTION 动作需要 functionId，或提供可保存的 functionBody/functionLang；请先使用 generate_actions 生成预览",
                            operationId(context));
                }
                FunctionSaveReqVO function = new FunctionSaveReqVO();
                function.setOntologyId(ontologyId);
                function.setName(name + "函数");
                function.setCode(functionCode(ontologyId, name));
                function.setLang(functionLang.trim().toUpperCase(Locale.ROOT));
                function.setFuncType("TRANSFORM");
                function.setBody(functionBody);
                function.setParams(functionParams);
                function.setSourceConceptId(request.getSourceConceptId());
                function.setSourceRelationIds(sourceRelationIds);
                function.setOutputConceptId(request.getOutputConceptId());
                function.setReadLimit(readLimit);
                function.setNeedsApproval(request.getNeedsApproval());
                function.setDescription(description);
                functionId = functionService.createFunction(function);
            }
            request.setFunctionId(functionId);
        } else if (!blank(executionSteps)) {
            request.setExecutionSteps(normalizeExecutionSteps(ontologyId, executionSteps));
        }

        Long id = actionService.createAction(request); ActionRespVO response = actionService.getActionById(id);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response == null ? id : response, "动作定义创建完成", operationId(context));
    }

    private AiActionPreviewRespVO.GeneratedActionPreview resolvePreview(AgentToolRequest.CreateAction input,
                                                                         AgentToolContext context) {
        if (blank(input.getPreviewIndex())) return null;
        if (context == null || blank(context.getOperationId())) {
            throw new IllegalArgumentException("动作预览上下文已失效，请重新生成动作预览");
        }
        Integer index;
        try {
            index = Integer.valueOf(input.getPreviewIndex().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("previewIndex 必须是从 0 开始的动作预览索引");
        }
        AiActionPreviewRespVO.GeneratedActionPreview preview = previewStore.get(context.getOperationId(), index);
        if (preview == null && operationService != null) {
            AiAgentOperationDO operation = operationService.get(context.getOperationId());
            if (operation != null) {
                preview = previewStore.restoreFromResultJson(
                        context.getOperationId(), operation.getResultJson(), index);
            }
        }
        if (preview == null) throw new IllegalArgumentException("动作预览已过期或索引不存在，请重新调用 generate_actions");
        return preview;
    }

    private String normalizeExecutionSteps(Long ontologyId, String json) {
        JSONArray steps;
        try {
            steps = JSON.parseArray(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("执行步骤 JSON 格式错误: " + e.getMessage(), e);
        }
        if (steps == null) return "[]";
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            if (step == null) continue;
            String targetType = step.getString("targetType");
            if (blank(targetType)) targetType = blank(step.getString("relationCode")) ? "CONCEPT" : "RELATION";
            step.put("targetType", targetType.toUpperCase(Locale.ROOT));
            if ("RELATION".equalsIgnoreCase(targetType)) {
                Long relationId = AgentToolIds.longValue(step.getString("relationId"));
                if (relationId == null) relationId = resolveRelationId(ontologyId, step.getString("relationCode"));
                if (relationId == null) throw new IllegalArgumentException("执行步骤引用了不存在的关系: " + step.getString("relationCode"));
                step.put("relationId", relationId);
                step.remove("relationCode");
            } else {
                Long conceptId = AgentToolIds.longValue(step.getString("conceptId"));
                if (conceptId == null) conceptId = resolveConceptId(ontologyId, step.getString("conceptCode"), null);
                if (conceptId == null) throw new IllegalArgumentException("执行步骤引用了不存在的概念: " + step.getString("conceptCode"));
                step.put("conceptId", conceptId);
                step.remove("conceptCode");
            }
            JSONArray params = step.getJSONArray("paramConfig");
            if (params == null) params = new JSONArray();
            JSONArray conditions = step.getJSONArray("conditionConfig");
            if (conditions != null) {
                for (int j = 0; j < conditions.size(); j++) {
                    JSONObject condition = conditions.getJSONObject(j);
                    if (condition == null) continue;
                    condition.put("condition", true);
                    params.add(condition);
                }
                step.put("paramConfig", params);
                step.remove("conditionConfig");
            }
        }
        return steps.toJSONString();
    }

    private Long resolveConceptId(Long ontologyId, String code, Long fallback) {
        if (blank(code)) return fallback;
        ConceptDO concept = conceptMapper.selectOne(new LambdaQueryWrapperX<ConceptDO>()
                .eq(ConceptDO::getOntologyId, ontologyId).eq(ConceptDO::getCode, code));
        return concept == null ? fallback : concept.getId();
    }

    private String resolveConceptIdAsString(Long ontologyId, String code, String fallback) {
        Long id = resolveConceptId(ontologyId, code, AgentToolIds.longValue(fallback));
        return id == null ? fallback : String.valueOf(id);
    }

    private Long resolveRelationId(Long ontologyId, String code) {
        if (blank(code)) return null;
        RelationDO relation = relationMapper.selectOne(new LambdaQueryWrapperX<RelationDO>()
                .eq(RelationDO::getOntologyId, ontologyId).eq(RelationDO::getCode, code));
        return relation == null ? null : relation.getId();
    }

    private String functionCode(Long ontologyId, String name) {
        String safe = name == null ? "action" : name.replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_").replaceAll("^_|_$", "").toLowerCase(Locale.ROOT);
        if (safe.isEmpty()) safe = "action";
        return "agent_" + ontologyId + "_" + safe + "_" + System.currentTimeMillis();
    }

    private String text(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
