package com.datamaster.module.ai.controller.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceSpaceRelService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 查询当前空间可见的数据源目录，不返回连接配置。 */
@Component
@AgentTool
public class ListDatasourcesTool {
    @Resource private IAssetsDatasourceService datasourceService;
    @Resource private IAssetsDatasourceSpaceRelService relationService;
    @Resource private AgentToolResourceGuard resourceGuard;

    @Tool(name = "list_datasources", value = "列出当前空间可见的数据源名称、类型和连接地址，不返回密码或连接配置")
    public AgentToolResult execute(AgentToolRequest.ListDatasources input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        AssetsDatasourceSpaceRelDO query = AssetsDatasourceSpaceRelDO.builder().build();
        query.setSpaceId(context.getSpaceId()); query.setSpaceCode(context.getSpaceCode());
        List<AssetsDatasourceSpaceRelDO> relations = relationService.getDatasourceSpaceRelList(query);
        List<Map<String, Object>> result = new ArrayList<>();
        if (relations != null) for (AssetsDatasourceSpaceRelDO relation : relations) {
            if (relation == null || relation.getDatasourceId() == null || Boolean.FALSE.equals(relation.getValidFlag())) continue;
            AssetsDatasourceDO item = datasourceService.getDatasourceDOById(relation.getDatasourceId());
            if (item == null) continue;
            // 数据源 ID 是 19 位雪花 ID，不能以 JSON number 交给模型，否则模型/提供商
            // 可能按 IEEE-754 浮点数解析并把末位改写（例如 ...7058 变成 ...7056）。
            // Tool 返回字符串，后续 get_datasource 仍可安全转回 Long 查询。
            Map<String, Object> row = new LinkedHashMap<>();
            String datasourceId = item.getId() == null ? null : String.valueOf(item.getId());
            row.put("id", datasourceId);
            row.put("datasourceId", datasourceId);
            row.put("datasourceName", item.getDatasourceName()); row.put("datasourceType", item.getDatasourceType());
            row.put("ip", item.getIp()); row.put("port", item.getPort()); row.put("dbgptSyncStatus", item.getDbgptSyncStatus());
            result.add(row);
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "已查询当前空间数据源", operationId(context));
    }
    private String operationId(AgentToolContext c) { return c == null ? null : c.getOperationId(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
