package com.datamaster.module.ai.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.module.ai.controller.admin.skill.vo.AiSkillReportTemplateGenerateReqVO;
import com.datamaster.module.ai.service.skill.IAiSkillService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

/** 保留现有 AI 报告模板生成能力，并作为 Agent 可调用的生成 Tool。 */
@Component
@AgentTool(risk = AgentToolRisk.READ_ONLY, requiresConfirmation = false)
public class GenerateReportTemplateTool {

    @Resource
    private IAiSkillService skillService;
    @Autowired(required = false)
    private ISpaceApi spaceApi;
    @Tool(name = "generate_report_template", value = "根据自然语言要求生成报告模板 JSON；只生成模板，不执行数据查询或业务动作")
    public AgentToolResult execute(AgentToolRequest.GenerateReportTemplate input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (context == null || spaceApi == null
                || !spaceApi.hasUserSpace(context.getUserId(), context.getSpaceId(), context.getSpaceCode())) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "请先确定当前空间", context == null ? null : context.getOperationId());
        }
        Long skillId = AgentToolIds.longValue(input.getSkillId());
        String prompt = text(input.getPrompt());
        if (skillId == null || prompt == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "生成报告模板需要 skillId 和 prompt", context.getOperationId());
        }
        AiSkillReportTemplateGenerateReqVO request = new AiSkillReportTemplateGenerateReqVO();
        request.setPrompt(prompt);
        String template = skillService.generateReportTemplate(skillId, request);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, template, "报告模板生成完成", context.getOperationId());
    }
    private String text(Object value) { return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim(); }
}
