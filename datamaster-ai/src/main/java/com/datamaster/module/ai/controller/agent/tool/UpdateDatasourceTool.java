package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.tool.AgentToolIds;

import dev.langchain4j.agent.tool.Tool;
import com.alibaba.fastjson2.JSON;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSaveReqVO;
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

/** 更新当前空间可见的数据源，保留原有空间绑定，避免 update service 清空关系。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class UpdateDatasourceTool {

    @Resource
    private IAssetsDatasourceService datasourceService;

    @Resource
    private IAssetsDatasourceSpaceRelService datasourceSpaceRelService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "update_datasource", value = "更新当前空间中的数据源；只修改展示名称时可省略 databaseName，若修改连接配置则必须提供目标 databaseName，PostgreSQL 默认模式为 public；Tool 会回读配置校验 dbname/schema，实际未变更时不会报告成功")
    public AgentToolResult execute(AgentToolRequest.UpdateDatasource input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        Long id = AgentToolIds.longValue(input.getDatasourceId());
        if (id == null) return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "缺少 datasourceId", operationId(context));
        AssetsDatasourceDO existing = datasourceService.getDatasourceDOById(id);
        if (existing == null) return AgentToolResult.of(AgentToolResult.FAILED, null, "数据源不存在：" + id, operationId(context));
        if (!resourceGuard.datasourceVisible(id, context)) {
            return AgentToolResult.of(AgentToolResult.FAILED, null, "数据源不属于当前空间，拒绝更新", operationId(context));
        }
        boolean hasAnyPatch = input.getDatasourceName() != null || input.getDatabaseName() != null
                || input.getSchemaName() != null || input.getDatasourceType() != null
                || input.getDatasourceConfig() != null || input.getIp() != null
                || input.getPort() != null || input.getDescription() != null;
        if (!hasAnyPatch) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "没有提供任何更新字段；请明确 databaseName/schemaName、连接配置或展示名称", operationId(context));
        }

        AssetsDatasourceSaveReqVO request = new AssetsDatasourceSaveReqVO();
        request.setId(id);
        request.setDatasourceName(valueOr(input.getDatasourceName(), existing.getDatasourceName()));
        request.setDatasourceType(valueOr(input.getDatasourceType(), existing.getDatasourceType()));
        request.setIp(valueOr(input.getIp(), existing.getIp()));
        request.setPort(input.getPort() != null ? input.getPort() : existing.getPort());
        request.setDescription(valueOr(input.getDescription(), existing.getDescription()));
        String databaseName = text(input.getDatabaseName());
        String schemaName = text(input.getSchemaName());
        boolean hasConnectionPatch = input.getDatasourceConfig() != null || input.getIp() != null
                || input.getPort() != null || input.getDatasourceType() != null
                || databaseName != null || schemaName != null;
        if (hasConnectionPatch && databaseName == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "修改数据源连接时必须提供目标 databaseName；当前请求只能修改展示信息", operationId(context));
        }
        if (input.getDatasourceConfig() != null) {
            Object config = input.getDatasourceConfig();
            request.setDatasourceConfig(normalizeConnectionConfig(
                    config instanceof String ? (String) config : JSON.toJSONString(config),
                    request.getDatasourceType(), databaseName, schemaName));
        } else {
            request.setDatasourceConfig(normalizeConnectionConfig(existing.getDatasourceConfig(),
                    request.getDatasourceType(), databaseName, schemaName));
        }
        request.setSpaceList(copyRelations(id));
        int updated = datasourceService.updateDatasource(request);
        if (updated <= 0) return AgentToolResult.failure("数据源更新未写入数据库");
        AssetsDatasourceDO verified = datasourceService.getDatasourceDOById(id);
        String actualDatabaseName = databaseNameFromConfig(verified == null ? null : verified.getDatasourceConfig());
        String actualSchemaName = schemaFromConfig(verified == null ? null : verified.getDatasourceConfig());
        if (databaseName != null && !databaseName.equalsIgnoreCase(actualDatabaseName)) {
            return AgentToolResult.failure("数据源更新校验失败：数据库名仍为 " + actualDatabaseName + "，目标应为 " + databaseName);
        }
        String expectedSchema = schemaName;
        if (expectedSchema == null && isPostgres(request.getDatasourceType())) expectedSchema = "public";
        if (expectedSchema != null && !expectedSchema.equalsIgnoreCase(actualSchemaName)) {
            return AgentToolResult.failure("数据源更新校验失败：模式仍为 " + actualSchemaName + "，目标应为 " + expectedSchema);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("datasourceName", request.getDatasourceName());
        result.put("databaseName", actualDatabaseName);
        result.put("schemaName", actualSchemaName);
        result.put("verified", true);
        result.put("updated", true);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "数据源更新成功", operationId(context));
    }

    private List<DatasourceSpaceRelDO> copyRelations(Long datasourceId) {
        AssetsDatasourceSpaceRelDO query = AssetsDatasourceSpaceRelDO.builder().build();
        query.setDatasourceId(datasourceId);
        List<AssetsDatasourceSpaceRelDO> rows = datasourceSpaceRelService.getDatasourceSpaceRelList(query);
        List<DatasourceSpaceRelDO> result = new ArrayList<>();
        if (rows != null) for (AssetsDatasourceSpaceRelDO row : rows) {
            DatasourceSpaceRelDO copy = new DatasourceSpaceRelDO();
            copy.setSpaceId(row.getSpaceId());
            copy.setSpaceCode(row.getSpaceCode());
            copy.setDescription(row.getDescription());
            copy.setValidFlag(row.getValidFlag());
            copy.setDppAssigned(row.getDppAssigned());
            result.add(copy);
        }
        return result;
    }

    private String valueOr(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String text(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }

    private String normalizeConnectionConfig(String config, String type, String databaseName, String schemaName) {
        try {
            com.alibaba.fastjson2.JSONObject json = JSON.parseObject(config);
            if (json == null) return config;
            if (databaseName != null) json.put("dbname", databaseName);
            String schema = schemaName;
            if (schema == null && isPostgres(type)) schema = "public";
            if (schema != null) {
                json.put("sid", schema);
                json.put("schema", schema);
            }
            return json.toJSONString();
        } catch (Exception ignored) { return config; }
    }

    private String databaseNameFromConfig(String config) {
        try { return config == null ? null : JSON.parseObject(config).getString("dbname"); }
        catch (Exception ignored) { return null; }
    }

    private String schemaFromConfig(String config) {
        try {
            if (config == null) return null;
            com.alibaba.fastjson2.JSONObject json = JSON.parseObject(config);
            String schema = json.getString("schema");
            return schema == null ? json.getString("sid") : schema;
        } catch (Exception ignored) { return null; }
    }

    private boolean isPostgres(String type) {
        String value = type == null ? "" : type.toLowerCase();
        return value.contains("postgres") || value.contains("kingbase");
    }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
