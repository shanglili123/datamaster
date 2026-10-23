package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 删除数据源。只有确认的同一个 datasourceId 才能执行，防止确认串用。 */
@Component
@AgentTool(risk = AgentToolRisk.DESTRUCTIVE, requiresConfirmation = true, confirmationArgument = "datasourceId")
public class DeleteDatasourceTool {

    @Resource private IAssetsDatasourceService datasourceService;
    @Resource private AgentToolResourceGuard resourceGuard;
    @Tool(name = "delete_datasource", value = "删除当前空间的数据源及其空间绑定；这是不可逆操作，必须人工确认同一个 datasourceId")
    public AgentToolResult execute(AgentToolRequest.DeleteDatasource input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = AgentToolIds.longValue(input.getDatasourceId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 datasourceId", operationId(context));
        AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(id);
        if (datasource == null) return AgentToolResult.of(AgentToolResult.FAILED, null, "数据源不存在：" + id, operationId(context));
        if (!resourceGuard.datasourceVisible(id, context)) return AgentToolResult.of(AgentToolResult.FAILED, null, "数据源不属于当前空间，拒绝删除", operationId(context));
        datasourceService.removeDatasource(Collections.singletonList(id));
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", id); result.put("deleted", true);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "数据源删除成功", operationId(context));
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
}
