package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.ai.service.dbgpt.IDbGptSkillSyncService;
import com.datamaster.module.assets.service.dbgpt.IDbGptDatasourceSyncService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 将平台 Skill 和指定数据源能力同步到数据智能体运行时。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class SyncAgentCapabilityTool {

    @Resource
    private IDbGptSkillSyncService skillSyncService;
    @Resource
    private IDbGptDatasourceSyncService datasourceSyncService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "sync_agent_capability", value = "同步 Skill 和数据源能力，使数据智能体可以使用最新的平台知识和数据源")
    public AgentToolResult execute(AgentToolRequest.SyncAgentCapability input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long datasourceId = AgentToolIds.longValue(input.getDatasourceId());
        if (datasourceId != null && !resourceGuard.datasourceVisible(datasourceId, context)) {
            return AgentToolResult.failure("数据源不属于当前空间，拒绝同步数据源能力");
        }
        AjaxResult skillResult = skillSyncService.syncAllSkills();
        AjaxResult datasourceResult = datasourceId == null ? null : datasourceSyncService.syncById(datasourceId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skillSync", skillResult);
        data.put("datasourceSync", datasourceResult);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, data, "智能体能力同步完成", context.getOperationId());
    }

}
