package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.service.IOntologyService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 修改本体元数据，不重新生成概念结构。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class UpdateOntologyTool {
    @Resource private IOntologyService ontologyService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "update_ontology", value = "修改已有本体的名称、编码、描述或状态；不会重新生成概念和属性，需要用户确认")
    public AgentToolResult execute(AgentToolRequest.UpdateOntology input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        if (id == null) id = currentOntology(context);
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "缺少 ontologyId，且当前页面/流程没有绑定本体", operationId(context));
        if (!resourceGuard.ontologyVisible(id, context)) return AgentToolResult.failure("本体不属于当前空间，拒绝修改");
        OntologyRespVO current = ontologyService.getOntologyById(id);
        if (current == null) return AgentToolResult.failure("本体不存在: " + id);
        OntologySaveReqVO request = new OntologySaveReqVO(); request.setId(id);
        request.setName(blank(input == null ? null : input.getName()) ? current.getName() : input.getName().trim());
        request.setCode(blank(input == null ? null : input.getCode()) ? current.getCode() : input.getCode().trim());
        request.setDescription(input != null && input.getDescription() != null ? input.getDescription() : current.getDescription());
        request.setStatus(input != null && input.getStatus() != null ? input.getStatus() : current.getStatus());
        request.setSpaceId(current.getSpaceId()); request.setSpaceCode(current.getSpaceCode());
        ontologyService.updateOntology(request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, ontologyService.getOntologyById(id), "本体元数据更新完成", operationId(context));
    }
    private Long currentOntology(AgentToolContext context) {
        if (context != null && context.getAttributes() != null && context.getAttributes().get("ontologyId") != null)
            return AgentToolIds.longValue(String.valueOf(context.getAttributes().get("ontologyId")));
        com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
        return operation == null ? null : operation.getOntologyId();
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
