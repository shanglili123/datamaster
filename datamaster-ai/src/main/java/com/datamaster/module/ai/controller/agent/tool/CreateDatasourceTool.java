package com.datamaster.module.ai.controller.agent.tool;

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
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 创建受控数据源；复用数据源服务的连接校验、加密和空间绑定逻辑。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class CreateDatasourceTool {

    @Resource
    private IAssetsDatasourceService datasourceService;
    @Resource
    private AgentToolResourceGuard resourceGuard;
    @Tool(name = "create_datasource", value = "创建受控数据库数据源并绑定当前空间；同一连接已存在时返回已有数据源，不重复创建。复制已有数据源时必须同时提供 sourceDatasourceName 和目标 databaseName，平台会复制加密认证配置并把 dbname、PostgreSQL 的 sid/schema 改为目标值，模型不得猜测或填写密码")
    public AgentToolResult execute(AgentToolRequest.CreateDatasource input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null, "创建数据源前请先确定当前空间", operationId(context));
        }
        String name = text(input.getDatasourceName());
        String type = text(input.getDatasourceType());
        String ip = text(input.getIp());
        Long port = number(input.getPort());
        String databaseName = text(input.getDatabaseName());
        String schemaName = text(input.getSchemaName());
        if (name == null || type == null || ip == null || port == null) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "创建数据源至少需要 datasourceName、datasourceType、ip 和 port", context.getOperationId());
        }

        Object config = input.getDatasourceConfig();
        String datasourceConfig = config instanceof String ? (String) config : config == null ? null : JSON.toJSONString(config);
        if (isBlankConfig(datasourceConfig) && text(input.getSourceDatasourceName()) != null) {
            if (databaseName == null) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "复制数据源时必须提供目标 databaseName，不能沿用源数据源的数据库名", context.getOperationId());
            }
            String sourceName = text(input.getSourceDatasourceName());
            AssetsDatasourceDO source = findVisibleDatasource(sourceName, context);
            if (source == null) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "找不到用于复制连接配置的数据源：" + sourceName, context.getOperationId());
            }
            datasourceConfig = source.getDatasourceConfig();
            if (isBlankConfig(datasourceConfig)) {
                return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                        "源数据源没有可复制的连接配置：" + sourceName, context.getOperationId());
            }
            // 复制连接参数时只复用认证/网络配置；目标库名必须指向用户请求的库，
            // 否则会创建一个名称像新库、实际仍连接旧库的误导性数据源。
            datasourceConfig = normalizeConnectionConfig(datasourceConfig, type, databaseName, schemaName);
        }
        if (isBlankConfig(datasourceConfig)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "数据源配置不能为空；请指定 sourceDatasourceName 复制已有数据源配置", context.getOperationId());
        }
        // 即使模型把连接配置作为参数传回，也必须用目标库和默认模式覆盖旧值，
        // 防止把源库 datamaster_movie 的 dbname 原样带到新数据源。
        if (databaseName != null || schemaName != null) {
            datasourceConfig = normalizeConnectionConfig(datasourceConfig, type, databaseName, schemaName);
        }

        List<AssetsDatasourceDO> existing = datasourceService.getDatasourceList();
        if (existing != null) {
            for (AssetsDatasourceDO item : existing) {
                if (resourceGuard.datasourceVisible(item.getId(), context)
                        && sameConnection(item, name, type, ip, port, databaseName)) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", item.getId() == null ? null : String.valueOf(item.getId()));
                    result.put("datasourceId", item.getId() == null ? null : String.valueOf(item.getId()));
                    result.put("datasourceName", item.getDatasourceName());
                    result.put("databaseName", databaseName);
                    result.put("reused", true);
                    return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "已复用现有数据源", context.getOperationId());
                }
            }
        }

        AssetsDatasourceSaveReqVO request = new AssetsDatasourceSaveReqVO();
        request.setDatasourceName(name);
        request.setDatasourceType(type);
        request.setIp(ip);
        request.setPort(port);
        request.setDescription("由数据智能体 Tool 创建");
        request.setDatasourceConfig(datasourceConfig);
        if (context.getSpaceId() != null || context.getSpaceCode() != null) {
            DatasourceSpaceRelDO relation = new DatasourceSpaceRelDO();
            relation.setSpaceId(context.getSpaceId());
            relation.setSpaceCode(context.getSpaceCode());
            relation.setValidFlag(Boolean.TRUE);
            request.setSpaceList(java.util.Collections.singletonList(relation));
        }
        Long id = datasourceService.createDatasource(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id == null ? null : String.valueOf(id));
        result.put("datasourceId", id == null ? null : String.valueOf(id));
        result.put("datasourceName", name);
        result.put("databaseName", databaseName);
        result.put("reused", false);
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result, "数据源创建成功", context.getOperationId());
    }

    private boolean sameConnection(AssetsDatasourceDO item, String name, String type, String ip,
                                   Long port, String databaseName) {
        if (item == null || !same(item.getDatasourceType(), type) || !same(item.getIp(), ip)
                || !port.equals(item.getPort())) return false;
        if (databaseName != null && item.getDatasourceConfig() != null) {
            try {
                String existingDb = JSON.parseObject(item.getDatasourceConfig()).getString("dbname");
                if (existingDb != null) return databaseName.equalsIgnoreCase(existingDb);
            } catch (Exception ignored) {
                // 旧数据源配置不是 JSON 时仍按名称、地址和端口判断。
            }
        }
        return name.equalsIgnoreCase(item.getDatasourceName());
    }

    private String text(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? null : String.valueOf(value).trim();
    }

    private Long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean same(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private AssetsDatasourceDO findVisibleDatasource(String name, AgentToolContext context) {
        List<AssetsDatasourceDO> existing = datasourceService.getDatasourceList();
        if (existing == null) return null;
        for (AssetsDatasourceDO item : existing) {
            if (item != null && same(item.getDatasourceName(), name)
                    && resourceGuard.datasourceVisible(item.getId(), context)) return item;
        }
        return null;
    }

    private boolean isBlankConfig(String config) {
        return config == null || config.trim().isEmpty() || "{}".equals(config.trim()) || "null".equalsIgnoreCase(config.trim());
    }

    private String replaceDatabaseName(String config, String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) return config;
        try {
            com.alibaba.fastjson2.JSONObject json = JSON.parseObject(config);
            if (json == null) return config;
            // 平台统一读取 dbname；兼容旧别名后始终写入规范键。
            if (json.containsKey("databaseName")) json.put("databaseName", databaseName);
            if (json.containsKey("dbName")) json.put("dbName", databaseName);
            json.put("dbname", databaseName);
            return json.toJSONString();
        } catch (Exception ignored) {
            return config;
        }
    }

    private String normalizeConnectionConfig(String config, String datasourceType,
                                             String databaseName, String schemaName) {
        String normalized = replaceDatabaseName(config, databaseName);
        try {
            com.alibaba.fastjson2.JSONObject json = JSON.parseObject(normalized);
            if (json == null) return normalized;
            String type = datasourceType == null ? "" : datasourceType.toLowerCase();
            String schema = schemaName;
            if (schema == null && (type.contains("postgres") || type.contains("kingbase"))) schema = "public";
            if (schema != null && !schema.trim().isEmpty()) {
                json.put("sid", schema);
                json.put("schema", schema);
            }
            return json.toJSONString();
        } catch (Exception ignored) {
            return normalized;
        }
    }

    private String operationId(AgentToolContext context) {
        return context == null ? null : context.getOperationId();
    }
}
