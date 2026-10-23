package com.datamaster.module.ai.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlRespVO;
import com.datamaster.module.ai.service.skill.IAiAskDataService;
import com.datamaster.common.api.space.ISpaceApi;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** DB-GPT 数据问答 Tool，只负责查询和分析，不负责平台流程写操作。 */
@Component
@AgentTool
public class DbGptQueryTool {

    @Resource
    private IAiAskDataService askDataService;
    @Autowired(required = false)
    private ISpaceApi spaceApi;
    @Tool(name = "dbgpt_query", value = "使用 DB-GPT 基于当前数据资产进行自然语言查询、数据分析或经营决策建议，不执行本体动作")
    public AgentToolResult execute(AgentToolRequest.DbGptQuery input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!hasAuthorizedSpace(context)) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                "请先确定当前空间", context == null ? null : context.getOperationId());
        String question = text(input == null ? null : input.getQuestion());
        if (question == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 question", context.getOperationId());
        com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlReqVO request = new com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlReqVO();
        request.setQuestion(question);
        request.setAssetId(AgentToolIds.longValue(input.getAssetId()));
        request.setDatasourceId(AgentToolIds.longValue(input.getDatasourceId()));
        request.setModel(input.getModel());
        request.setChatMode(input.getChatMode());
        request.setResponseMode(input.getResponseMode());
        request.setSkillIds(AgentToolIds.longValues(input.getSkillIds()));
        request.setReturnSql(input.getReturnSql());
        request.setHistory(toHistoryMaps(input.getHistory()));
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        request.setConversationId(conversationId(context));
        AiAskDataSqlRespVO response = askDataService.chatWithDbGpt(request, context.getUserId());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, response, "DB-GPT 查询分析完成", context.getOperationId());
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
