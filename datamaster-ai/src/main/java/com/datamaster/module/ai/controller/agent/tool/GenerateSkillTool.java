package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ai.controller.admin.skill.vo.AiOntologySkillGenerateReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.ai.service.skill.IAiSkillService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** 生成本体决策 Skill，复用现有 Skill 生成服务。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class GenerateSkillTool {

    @Resource
    private IAiSkillService aiSkillService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "generate_skill", value = "根据本体生成决策 Skill，可选择是否发布")
    public AgentToolResult execute(AgentToolRequest.GenerateSkill input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long ontologyId = input == null ? null : AgentToolIds.longValue(input.getOntologyId());
        Object pageOntologyId = context == null || context.getAttributes() == null
                ? null : context.getAttributes().get("ontologyId");
        if (pageOntologyId != null) ontologyId = AgentToolIds.longValue(String.valueOf(pageOntologyId));
        if (ontologyId == null) {
            com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO operation = resourceGuard.currentOperation(context);
            ontologyId = operation == null ? null : operation.getOntologyId();
        }
        if (ontologyId == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 ontologyId，且当前页面/流程没有绑定本体", context == null ? null : context.getOperationId());
        if (!resourceGuard.ontologyVisible(ontologyId, context)) {
            return AgentToolResult.failure("本体不属于当前空间，拒绝生成 Skill");
        }
        AiOntologySkillGenerateReqVO request = new AiOntologySkillGenerateReqVO();
        request.setOntologyId(ontologyId);
        request.setPublish(booleanValue(input == null ? null : input.getPublish()));
        request.setManualNotes(text(input == null ? null : input.getManualNotes()));
        AiSkillRespVO response = aiSkillService.generateOntologyDecisionSkill(request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "Skill 生成完成",
                context == null ? null : context.getOperationId());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }


    private Boolean booleanValue(Object value) {
        return value == null ? Boolean.FALSE : Boolean.valueOf(String.valueOf(value));
    }
}
