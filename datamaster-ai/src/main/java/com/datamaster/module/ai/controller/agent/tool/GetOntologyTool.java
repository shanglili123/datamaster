package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.service.IOntologyService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 查询当前空间可见的本体摘要，不执行生成或修改。 */
@Component
@AgentTool
public class GetOntologyTool {

    @Resource
    private IOntologyService ontologyService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    @Tool(name = "get_ontology", value = "查询当前空间可见的本体详情和生成状态；ontologyId 可省略并从当前页面/流程复用，不执行本体修改")
    public AgentToolResult execute(AgentToolRequest.GetOntology input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "请先确定当前空间", operationId(context));
        }
        Long ontologyId = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        Object pageOntologyId = context == null || context.getAttributes() == null
                ? null : context.getAttributes().get("ontologyId");
        if (pageOntologyId != null) ontologyId = AgentToolIds.longValue(String.valueOf(pageOntologyId));
        if (ontologyId == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            ontologyId = operation == null ? null : operation.getOntologyId();
        }
        if (ontologyId == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "缺少 ontologyId，且当前页面/流程没有绑定本体", operationId(context));
        }
        if (!resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝查询");
        }
        OntologyRespVO ontology = ontologyService.getOntologyById(ontologyId);
        if (ontology == null) {
            return AgentToolResult.failure("本体不存在: " + ontologyId);
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, ontology,
                "已查询本体详情", operationId(context));
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
