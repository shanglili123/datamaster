package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import com.datamaster.module.ontology.service.IOntologyGenerateService;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** 生成本体动作预览；只生成预览，不自动保存或执行动作。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY, requiresConfirmation = false)
public class GenerateActionsTool {

    @Resource
    private IOntologyGenerateService ontologyGenerateService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Resource
    private AgentActionPreviewStore previewStore;
    @Tool(name = "generate_actions", value = "用于‘生成动作’或‘生成动作草稿’：根据已有 ontologyId 和本体模型生成动作定义预览；未明确要求脚本/函数时默认生成 COMPOSITE 多步骤动作；不需要 actionId，也不会提交或执行已有动作")
    public AgentToolResult execute(AgentToolRequest.GenerateActions input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (input == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "生成动作需要当前本体 ontologyId 和动作功能描述 prompt",
                context == null ? null : context.getOperationId());
        Long ontologyId = AgentToolIds.longValue(input.getOntologyId());
        AiAgentOperationDO operation = resourceGuard.currentOperation(context);
        // 当前页面/流程上下文是可信来源，优先于模型从历史消息带来的旧 ontologyId。
        Object pageOntologyId = context == null || context.getAttributes() == null
                ? null : context.getAttributes().get("ontologyId");
        Long pageOntology = pageOntologyId == null ? null
                : AgentToolIds.longValue(String.valueOf(pageOntologyId));
        if (pageOntology != null) ontologyId = pageOntology;
        else if (operation != null && operation.getOntologyId() != null) ontologyId = operation.getOntologyId();
        String prompt = text(input.getPrompt());
        if (ontologyId == null) {
            java.util.Map<String, Object> lookup = new java.util.LinkedHashMap<>();
            lookup.put("requiresOntologyLookup", true);
            lookup.put("suggestedTool", "search_ontologies");
            return AgentToolResult.of(AgentToolResult.RUNNING, lookup,
                    "当前页面/流程没有绑定 ontologyId；请自主调用 search_ontologies 按本体名称或编码查询，唯一匹配后再调用 generate_actions",
                    context == null ? null : context.getOperationId());
        }
        if (prompt == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "生成动作需要动作功能描述 prompt", context == null ? null : context.getOperationId());
        if (!resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝生成动作");
        }
        AiActionPreviewReqVO request = new AiActionPreviewReqVO();
        request.setOntologyId(ontologyId);
        request.setPrompt(prompt);
        request.setActionTypes(resolveActionTypes(input.getActionTypes(), prompt));
        AiActionPreviewRespVO response = ontologyGenerateService.generateActionsPreview(request);
        if (context != null) {
            previewStore.put(context.getOperationId(), response);
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "动作预览生成完成，需审核后保存", context.getOperationId());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }

    private List<String> resolveActionTypes(List<String> requested, String prompt) {
        if (requested != null && !requested.isEmpty()) return requested;
        String text = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        boolean explicitFunction = text.contains("function") || text.contains("函数")
                || text.contains("脚本") || text.contains("typescript") || text.contains("javascript")
                || text.contains("python") || text.contains("自定义代码") || text.contains("自定义算法");
        return explicitFunction ? Arrays.asList("FUNCTION") : Arrays.asList("COMPOSITE");
    }

}
