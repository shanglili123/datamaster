package com.datamaster.module.ai.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.ai.service.skill.IAiAskDataService;
import com.datamaster.common.api.space.ISpaceApi;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** DB-GPT 报告 Tool，返回平台负责安全渲染的 Markdown/HTML 内容。 */
@Component
@AgentTool
public class DbGptReportTool {

    @Resource
    private IAiAskDataService askDataService;
    @Autowired(required = false)
    private ISpaceApi spaceApi;
    @Tool(name = "dbgpt_report", value = "使用 DB-GPT 根据真实数据生成统计或经营分析报告，返回 Markdown/HTML 内容")
    public AgentToolResult execute(AgentToolRequest.DbGptReport input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!hasAuthorizedSpace(context)) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "请先确定当前空间", context == null ? null : context.getOperationId());
        String question = text(input == null ? null : input.getQuestion());
        if (question == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 question", context.getOperationId());
        if (input.getDatasourceId() == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "缺少 datasourceId", context.getOperationId());
        com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportReqVO request = new com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportReqVO();
        request.setQuestion(question);
        request.setDatasourceId(AgentToolIds.longValue(input.getDatasourceId()));
        request.setSkillId(AgentToolIds.longValue(input.getSkillId()));
        request.setTemplateId(AgentToolIds.longValue(input.getTemplateId()));
        request.setModel(input.getModel());
        request.setReturnSql(input.getReturnSql());
        request.setParams(input.getParams());
        request.setHistory(toHistoryMaps(input.getHistory()));
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        request.setConversationId(conversationId(context));
        AiAskDataReportRespVO response = askDataService.generateReport(request, context.getUserId());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "数据报告生成完成", context.getOperationId());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }

    private String conversationId(AgentToolContext context) {
        if (context != null && context.getAttributes() != null && context.getAttributes().get("conversationId") != null) {
            return String.valueOf(context.getAttributes().get("conversationId"));
        }
        return context == null ? null : context.getOperationId();
    }

    private boolean hasAuthorizedSpace(AgentToolContext context) {
        return context != null && spaceApi != null
                && spaceApi.hasUserSpace(context.getUserId(), context.getSpaceId(), context.getSpaceCode());
    }

    private List<java.util.Map<String, String>> toHistoryMaps(
            List<AgentToolRequest.ConversationHistoryItem> history) {
        if (history == null) return null;
        List<java.util.Map<String, String>> result = new ArrayList<>();
        for (AgentToolRequest.ConversationHistoryItem item : history) {
            if (item == null) continue;
            java.util.Map<String, String> value = new HashMap<>();
            value.put("role", item.getRole());
            value.put("content", item.getContent());
            result.add(value);
        }
        return result;
    }
}
