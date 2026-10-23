package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.service.IOntologyService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 手工创建空本体；数据驱动本体生成使用 generate_ontology。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class CreateOntologyTool {
    @Resource private IOntologyService ontologyService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "create_ontology", value = "创建一个空的本体定义；如果目标是根据数据表自动生成概念、属性和关系，应使用 generate_ontology；创建本体需要用户确认")
    public AgentToolResult execute(AgentToolRequest.CreateOntology input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "请先确定当前空间", operationId(context));
        if (input == null || blank(input.getName()) || blank(input.getCode())) return AgentToolResult.of(
                AgentToolResult.NEED_INPUT, null, "创建本体需要 name 和 code", operationId(context));
        OntologySaveReqVO request = new OntologySaveReqVO();
        request.setName(input.getName().trim()); request.setCode(input.getCode().trim());
        request.setDescription(input.getDescription()); request.setStatus(input.getStatus() == null ? 0 : input.getStatus());
        request.setSpaceId(context.getSpaceId()); request.setSpaceCode(context.getSpaceCode());
        Long id = ontologyService.createOntology(request);
        OntologyRespVO response = ontologyService.getOntologyById(id);
        resourceGuard.bindOntology(context, id);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response == null ? id : response,
                "空本体创建完成", operationId(context));
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
