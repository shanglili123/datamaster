package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateRespVO;
import com.datamaster.module.ontology.service.IOntologyGenerateService;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 根据已同步的数据资产调用现有本体 AI 生成能力。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class GenerateOntologyTool {

    @Resource
    private IOntologyGenerateService ontologyGenerateService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "generate_ontology", value = "生成或复用本体模型（不是生成动作）：根据当前数据源已采集的全部表元数据生成本体、概念、属性和关系；当前流程已有 ontologyId 时必须复用；这是写操作，需要用户确认")
    public AgentToolResult execute(AgentToolRequest.GenerateOntology input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long datasourceId = input == null ? null : AgentToolIds.longValue(input.getDatasourceId());
        if (datasourceId == null) datasourceId = resourceGuard.currentDatasourceId(context);
        if (datasourceId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 datasourceId", operationId(context));
        if (!resourceGuard.datasourceVisible(datasourceId, context)) {
            return AgentToolResult.failure("当前空间无权访问数据源，拒绝生成本体");
        }
        if (!resourceGuard.matchesCurrentDatasource(datasourceId, context)) {
            return AgentToolResult.failure("请求的数据源与当前会话目标不一致；当前目标为 "
                    + resourceGuard.currentDatasourceDescription(context) + "，已拒绝为其他数据源生成本体");
        }
        resourceGuard.bindDatasource(context, datasourceId);
        Long ontologyId = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        AiAgentOperationDO operation = resourceGuard.currentOperation(context);
        if (ontologyId == null && operation != null && datasourceId.equals(operation.getDatasourceId())) {
            ontologyId = operation.getOntologyId();
        }
        if (ontologyId != null && !resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("目标本体不属于当前空间，拒绝生成");
        }
        // 生成本体是当前数据源流程的幂等步骤。已有本体时直接返回流程中的本体，
        // 不再次调用底层生成器，避免重复创建本体或重复插入概念/属性。
        if (ontologyId != null) {
            OntologyAiGenerateRespVO reused = new OntologyAiGenerateRespVO();
            reused.setOntologyId(ontologyId);
            return AgentToolResult.of(AgentToolResult.SUCCEEDED, reused,
                    "当前数据源流程已存在本体，已复用 ontologyId=" + ontologyId, operationId(context));
        }
        OntologyAiGenerateReqVO request = new OntologyAiGenerateReqVO();
        request.setDatasourceId(datasourceId);
        // 自主 Agent 的生成本体能力固定按数据源全量生成。单表参数不暴露给模型，
        // 避免模型从页面上下文随意挑一张表，最终只生成一个概念。
        request.setTableName(null);
        request.setOntologyId(ontologyId);
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        OntologyAiGenerateRespVO response = ontologyGenerateService.generate(request);
        if (response != null && response.getOntologyId() != null) resourceGuard.bindOntology(context, response.getOntologyId());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "本体模型生成完成", operationId(context));
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }

}
