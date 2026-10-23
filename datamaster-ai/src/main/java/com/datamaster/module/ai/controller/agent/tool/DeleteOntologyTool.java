package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.service.IOntologyService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 删除本体及其关联概念、动作和执行记录。 */
@Component
@AgentTool(risk = AgentToolRisk.DESTRUCTIVE, requiresConfirmation = true, confirmationArgument = "ontologyId")
public class DeleteOntologyTool {
    @Resource private IOntologyService ontologyService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "delete_ontology", value = "删除当前空间的本体及其关联概念、属性、动作和执行记录；不可逆，必须确认同一个 ontologyId")
    public AgentToolResult execute(AgentToolRequest.DeleteOntology input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "删除本体需要 ontologyId", operationId(context));
        if (!resourceGuard.ontologyVisible(id, context)) return AgentToolResult.failure("本体不属于当前空间，拒绝删除");
        ontologyService.deleteOntology(id);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, java.util.Collections.singletonMap("ontologyId", id), "本体已删除", operationId(context));
    }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
