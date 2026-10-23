package com.datamaster.module.ai.controller.agent.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.enums.ConfigType;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentTool;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolIds;
import com.datamaster.module.ai.agent.tool.AgentToolRequest;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.agent.tool.AgentToolRisk;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceService;
import com.datamaster.module.service.api.api.dto.ExecuteConfig;
import com.datamaster.module.service.api.api.dto.FieldParam;
import com.datamaster.module.service.api.api.dto.ResParam;
import com.datamaster.module.service.api.api.dto.ServiceApiReqDTO;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 根据当前空间可见数据源中的真实表创建只读表单查询服务草稿。 */
@Component
@AgentTool(risk = AgentToolRisk.WRITE, requiresConfirmation = true)
public class CreateDataServiceTool {

    @Resource
    private IAssetsDatasourceService datasourceService;
    @Resource
    private ServiceApiService serviceApiService;
    @Resource
    private AgentToolResourceGuard resourceGuard;

    @Tool(name = "create_data_service", value = "根据当前空间可见数据源中的真实表创建只读表单查询型数据服务草稿；调用前先用 list_assets 定位唯一订单表。不会执行写入 SQL，不会自动发布服务")
    public AgentToolResult execute(AgentToolRequest.CreateDataService input) {
        AgentToolContext context = com.datamaster.module.ai.agent.runtime.AgentToolContextHolder.get();
        if (!resourceGuard.hasSpace(context)) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "创建数据服务前请先确定当前空间", operationId(context));
        }
        if (input == null || blank(input.getDatasourceId()) || blank(input.getTableName())) {
            return AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "创建数据服务需要 datasourceId 和 tableName", operationId(context));
        }

        Long datasourceId = AgentToolIds.longValue(input.getDatasourceId());
        if (!resourceGuard.datasourceVisible(datasourceId, context)) {
            return AgentToolResult.failure("数据源不属于当前空间，拒绝创建数据服务");
        }
        if (!resourceGuard.matchesCurrentDatasource(datasourceId, context)) {
            return AgentToolResult.failure("请求的数据源与当前会话目标不一致，拒绝创建数据服务");
        }
        AssetsDatasourceDO datasource = datasourceService.getDatasourceDOById(datasourceId);
        if (datasource == null) return AgentToolResult.failure("数据源不存在：" + datasourceId);

        TableRef table = parseTable(input.getTableName());
        if (table == null) return AgentToolResult.failure("表名只能包含合法的 schema.table 标识符");
        List<DbColumn> columns = datasourceService.getDbTableColumns(datasourceId, table.tableName);
        if (columns == null || columns.isEmpty()) {
            return AgentToolResult.failure("未找到表结构，请先完成元数据探查或确认表名：" + input.getTableName());
        }

        JSONObject config = parseConfig(datasource.getDatasourceConfig());
        String databaseName = text(config.getString("dbname"));
        String schemaName = firstText(table.schemaName, config.getString("schema"), config.getString("sid"));
        if (schemaName == null && isPostgres(datasource.getDatasourceType())) schemaName = "public";

        ExecuteConfig executeConfig = new ExecuteConfig();
        executeConfig.setSourceId(String.valueOf(datasourceId));
        executeConfig.setApiServiceType(ConfigType.FORM.getKey());
        executeConfig.setTableName(table.tableName);
        executeConfig.setDbName(databaseName);
        executeConfig.setFieldParams(new ArrayList<FieldParam>());

        List<ResParam> responseFields = new ArrayList<>();
        for (DbColumn column : columns) {
            if (column == null || blank(column.getColName())) continue;
            FieldParam field = new FieldParam();
            field.setColumnName(column.getColName());
            field.setDataType(column.getDataType());
            field.setDataLength(toLong(column.getDataLength()));
            field.setDataPrecision(toLong(column.getDataPrecision()));
            field.setDataScale(toLong(column.getDataScale()));
            field.setColumnKey(Boolean.TRUE.equals(column.getColKey()) ? "1" : "0");
            field.setColumnNullable(Boolean.TRUE.equals(column.getNullable()) ? "1" : "0");
            field.setColumnPosition(column.getColPosition() == null ? null : column.getColPosition().longValue());
            field.setDataDefault(column.getDataDefault());
            field.setColumnComment(column.getColComment());
            field.setReqable("0");
            field.setResable("1");
            executeConfig.getFieldParams().add(field);

            ResParam response = new ResParam();
            response.setFieldName(column.getColName());
            response.setFieldComment(blank(column.getColComment()) ? column.getColName() : column.getColComment());
            response.setDataType(blank(column.getDataType()) ? "string" : column.getDataType());
            responseFields.add(response);
        }
        if (responseFields.isEmpty()) return AgentToolResult.failure("表结构没有可作为返回字段的列");

        ServiceApiReqDTO request = new ServiceApiReqDTO();
        request.setName(blank(input.getName()) ? "查询" + table.tableName : input.getName().trim());
        request.setApiVersion("v1");
        request.setApiUrl(blank(input.getApiUrl()) ? "/services/data/" + table.tableName : input.getApiUrl().trim());
        // SVC_API.REQ_METHOD is a CHAR(1) dictionary value: 1 = GET, 2 = POST.
        request.setReqMethod("1");
        request.setApiServiceType(ConfigType.FORM.getKey());
        request.setResDataType("3");
        request.setDescription(blank(input.getDescription()) ? "由数据智能体创建的只读数据服务" : input.getDescription().trim());
        request.setStatus("0");
        request.setValidFlag(Boolean.TRUE);
        request.setSpaceId(context.getSpaceId());
        request.setSpaceCode(context.getSpaceCode());
        request.setExecuteConfig(executeConfig);
        request.setReqParamsList(new ArrayList<com.datamaster.module.service.api.api.dto.ReqParam>());
        request.setResParamsList(responseFields);

        Long serviceId = serviceApiService.createDraftDataService(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceId", serviceId == null ? null : String.valueOf(serviceId));
        result.put("name", request.getName());
        result.put("apiUrl", request.getApiUrl());
        result.put("datasourceId", String.valueOf(datasourceId));
        result.put("datasourceName", datasource.getDatasourceName());
        result.put("databaseName", databaseName);
        result.put("schemaName", schemaName);
        result.put("tableName", table.tableName);
        result.put("status", "0");
        return AgentToolResult.of(AgentToolResult.SUCCEEDED, result,
                "只读订单数据服务草稿创建成功，尚未发布", operationId(context));
    }

    private TableRef parseTable(String value) {
        String raw = text(value);
        if (raw == null) return null;
        String[] parts = raw.replace("\"", "").replace("`", "").split("\\.");
        if (parts.length > 2) return null;
        for (String part : parts) {
            if (!part.matches("[A-Za-z_][A-Za-z0-9_$]*")) return null;
        }
        return parts.length == 2 ? new TableRef(parts[0], parts[1]) : new TableRef(null, parts[0]);
    }

    private JSONObject parseConfig(String value) {
        if (blank(value)) return new JSONObject();
        try {
            JSONObject result = JSON.parseObject(value);
            return result == null ? new JSONObject() : result;
        } catch (Exception ignored) {
            throw new ServiceException("数据源连接配置无法解析");
        }
    }

    private Long toLong(String value) {
        try { return blank(value) ? null : Long.valueOf(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String firstText(String... values) {
        if (values == null) return null;
        for (String value : values) if (!blank(value)) return value.trim();
        return null;
    }

    private boolean isPostgres(String type) {
        String value = type == null ? "" : type.toLowerCase();
        return value.contains("postgres") || value.contains("kingbase");
    }

    private String text(String value) { return blank(value) ? null : value.trim(); }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    private String operationId(AgentToolContext context) { return context == null ? null : context.getOperationId(); }

    private static final class TableRef {
        private final String schemaName;
        private final String tableName;
        private TableRef(String schemaName, String tableName) {
            this.schemaName = schemaName;
            this.tableName = tableName;
        }
    }
}
