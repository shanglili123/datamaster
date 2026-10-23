package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceSpaceRelService;
import com.datamaster.module.assets.service.datasource.DatasourceSpaceRelEnsureResult;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** 将已有数据源绑定到当前空间，重复绑定直接复用已有关系。 */
@Component
@AgentTool(risk = com.datamaster.module.ai.agent.tool.AgentToolRisk.WRITE, requiresConfirmation = true)
public class BindDatasourceSpaceTool {

    @Resource
    private IAssetsDatasourceService datasourceService;
    @Resource
    private IAssetsDatasourceSpaceRelService relationService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    @Tool(name = "bind_datasource_space", value = "将已有数据源绑定到当前空间；已有绑定会复用，不重复创建关系")
    public AgentToolResult execute(AgentToolRequest.BindDatasourceSpace input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "请先确定当前空间", operationId(context));
        }
        Long datasourceId = null;
        if (input != null) {
            try {
                datasourceId = AgentToolIds.longValue(input.getDatasourceId());
            } catch (com.datamaster.common.exception.ServiceException ignored) {
                // 长会话确认时模型偶尔会重放占位符（例如 ds-xxx）。
                // 当前流程已经由 get_datasource/create_datasource 绑定真实数据源，
                // 此时回退到受信任的流程目标，避免把占位符送入业务层。
            }
        }
        if (datasourceId == null) datasourceId = resourceGuard.currentDatasourceId(context);
        if (datasourceId == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "缺少 datasourceId", operationId(context));
        }
        AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(datasourceId);
        if (datasource == null) return AgentToolResult.failure("数据源不存在: " + datasourceId);
        DatasourceSpaceRelEnsureResult ensureResult = relationService.ensureDatasourceSpaceRel(
                datasourceId, context.getSpaceId(), context.getSpaceCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasourceId", datasourceId);
        result.put("datasourceName", datasource.getDatasourceName());
        result.put("relationId", ensureResult.getRelationId());
        result.put("reused", ensureResult.isReused());
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                "数据源已绑定到当前空间", operationId(context));
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
