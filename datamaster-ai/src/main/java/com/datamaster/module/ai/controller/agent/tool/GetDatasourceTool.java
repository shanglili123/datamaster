package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.alibaba.fastjson2.JSON;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 查询受控数据源，不向模型暴露账号、密码等连接凭据。 */
@Component
@AgentTool
public class GetDatasourceTool {

    @Resource
    private IAssetsDatasourceService datasourceService;

    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "get_datasource", value = "查询一个或全部受控数据源的基本信息，不返回数据库密码；优先按 datasourceName 查询，适合在复制连接配置前展示给用户确认")
    public AgentToolResult execute(AgentToolRequest.GetDatasource input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "请先确定当前空间", operationId(context));
        }
        Long id = AgentToolIds.longValue(input.getDatasourceId());
        String name = input.getDatasourceName() == null ? null : input.getDatasourceName().trim();
        if (id != null) {
            AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(id);
            if (datasource == null) return AgentToolResult.failure("数据源不存在: " + id);
            if (!resourceGuard.datasourceVisible(datasource.getId(), context)) return AgentToolResult.failure("当前空间无权访问数据源: " + id);
            return AgentToolResult.of(AgentToolResult.SUCCEEDED, safeDatasource(datasource), "已找到数据源", context.getOperationId());
        }
        if (name != null && !name.isEmpty()) {
            List<AssetsDatasourceDO> datasources = datasourceService.getDatasourceList();
            if (datasources != null) for (AssetsDatasourceDO datasource : datasources) {
                if (datasource != null && name.equalsIgnoreCase(datasource.getDatasourceName())
                        && resourceGuard.datasourceVisible(datasource.getId(), context)) {
                    return AgentToolResult.of(AgentToolResult.SUCCEEDED, safeDatasource(datasource), "已找到数据源", context.getOperationId());
                }
            }
            return AgentToolResult.failure("数据源不存在: " + name);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        List<AssetsDatasourceDO> datasources = datasourceService.getDatasourceList();
        if (datasources != null) {
            for (AssetsDatasourceDO datasource : datasources) {
                if (datasource != null && resourceGuard.datasourceVisible(datasource.getId(), context)) result.add(safeDatasource(datasource));
            }
        }
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "已查询数据源列表", context.getOperationId());
    }

    private Map<String, Object> safeDatasource(DatasourceDO datasource) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 雪花 ID 必须以字符串输出，避免模型侧浮点解析造成末位精度丢失。
        result.put("id", datasource.getId() == null ? null : String.valueOf(datasource.getId()));
        // 同时使用资源语义字段，避免模型把通用 id 改写成自行编造的占位符。
        result.put("datasourceId", datasource.getId() == null ? null : String.valueOf(datasource.getId()));
        result.put("datasourceName", datasource.getDatasourceName());
        result.put("datasourceType", datasource.getDatasourceType());
        result.put("ip", datasource.getIp());
        result.put("port", datasource.getPort());
        result.put("databaseName", databaseName(datasource));
        result.put("description", datasource.getDescription());
        result.put("validFlag", datasource.getValidFlag());
        result.put("dbgptSyncStatus", datasource.getDbgptSyncStatus());
        result.put("dbgptSyncMessage", datasource.getDbgptSyncMessage());
        return result;
    }

    private String databaseName(DatasourceDO datasource) {
        if (datasource == null || datasource.getDatasourceConfig() == null) return null;
        try {
            return JSON.parseObject(datasource.getDatasourceConfig()).getString("dbname");
        } catch (Exception ignored) {
            return null;
        }
    }


    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
