package com.datamaster.module.collector.utils.ds.component;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.utils.MD5Util;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;
import com.datamaster.module.collector.utils.SourceMetadataSupport;
import com.datamaster.module.collector.utils.JoinQueryBuilder;

import java.util.*;

/**
 * <P>
 * 用途:合表组件(多个源表合并为一个逻辑表输入)
 * </p>
 *
 * <p>
 * 合表是专用输入节点:多个结构相同的源表通过 UNION ALL 合并后作为单 reader 输入。
 * 生成的 querySql 走 FlinkxEtlTaskConverter 的 customSql 路径(虚拟表名 sourceTable),
 * 因此不修改多 reader 后端机制。
 * </p>
 *
 * <p>
 * taskParams 结构:
 * {
 *   "batchSize": 1024,
 *   "readerDatasource": { "datasourceId": 1, ... },
 *   "columns": ["ID","NAME"],          // 合并后输出的字段列表(各源表字段必须一致)
 *   "sourceTables": [                   // 源表列表
 *     { "table_name": "T1", "where": "" },
 *     { "table_name": "T2", "where": "CREATE_TIME > '2025-01-01'" }
 *   ]
 * }
 * </p>
 *
 * <p>
 * parse2 输出结构与 DBReaderComponent 对齐(connection.querySql + jdbcUrl),
 * 以便下游 FlinkxEtlTaskConverter.buildReader 直接复用 customSql 逻辑。
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-12 16:31
 **/
public class TableMergeComponent implements ComponentItem {
    @Override
    public Map<String, Object> parse(Map<String, Object> params) {
        Map<String, Object> taskParams = new LinkedHashMap<>();

        taskParams.put("localParams", params.getOrDefault("localParams", new ArrayList<>())); // 默认空列表
        taskParams.put("resourceList", params.getOrDefault("resourceList", new ArrayList<>())); // 默认空列表
        taskParams.put("customConfig", params.getOrDefault("customConfig", 1)); // 默认写死1
        taskParams.put("xms", params.getOrDefault("xms", 1)); // 默认1
        taskParams.put("xmx", params.getOrDefault("xmx", 1)); // 默认1
        taskParams.put("json", ""); // 默认空的JSON字符串
        return taskParams;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.TABLE_MERGE.getCode();
    }

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        taskParams = SourceMetadataSupport.withRequiredMetadata(taskParams);
        // reader 配置
        Map<String, Object> reader = new HashMap<>();
        // 输入readerDatasource
        Map<String, Object> readerDatasource = (Map<String, Object>) MapUtils.getObject(taskParams, "readerDatasource");
        DbQueryProperty readerProperty = MD5Util.buildJobDatasource(readerDatasource);
        reader.put("nodeCode", nodeCode);
        reader.put("nodeVersion", nodeVersion);
        reader.put("componentType", componentType.getCode());

        //参数
        Map<String, Object> parameter = new HashMap<>();
        reader.put("parameter", parameter);

        parameter.put("batchSize", taskParams.getOrDefault("batchSize", 1024));
        parameter.put("datasourceId", readerDatasource.get("datasourceId"));
        parameter.put("username", readerProperty.getUsername());
        parameter.put("password", readerProperty.getPassword());
        parameter.put("dbType", readerProperty.getDbType());
        parameter.put("host", readerProperty.getHost());
        parameter.put("port", readerProperty.getPort());
        parameter.put("datasourceConfig", readerProperty.getDatasourceConfig());
        parameter.put("config", readerProperty.getConfig());
        if (StringUtils.isNotBlank(readerProperty.getDbName())) {
            parameter.put("dbName", readerProperty.getDbName());
        }
        if (StringUtils.isNotBlank(readerProperty.getSid())) {
            parameter.put("sid", readerProperty.getSid());
        }
        boolean sourceMetadataEnabled = SourceMetadataSupport.enabled(taskParams);
        if (sourceMetadataEnabled) {
            // 合表 SQL 会真实返回 source_table；其它来源字段由 transformer 派生。
            parameter.put("column", taskParams.get("columns"));
            parameter.put("sourceMetadataColumns", SourceMetadataSupport.appendColumns(
                    taskParams.get("columns"), Collections.singletonList(
                            SourceMetadataSupport.column(taskParams, "sourceTableColumn",
                                    SourceMetadataSupport.SOURCE_TABLE))));
            Map<String, Object> sourceMetadata = SourceMetadataSupport.copyWithRuntimeValues(
                    taskParams, nodeCode,
                    String.valueOf(readerDatasource.get("datasourceId")), "");
            String sourceDatabase = MapUtils.getString(taskParams, "sourceDatabaseValue", null);
            sourceDatabase = StringUtils.defaultIfBlank(sourceDatabase,
                    MapUtils.getString(readerDatasource, "dbname", null));
            sourceMetadata.putIfAbsent("sourceDatabaseValue",
                    StringUtils.defaultIfBlank(sourceDatabase, readerProperty.getDbName()));
            parameter.put("sourceMetadata", sourceMetadata);
        } else {
            parameter.put("column", taskParams.get("columns"));
        }
        parameter.put("tableFields", taskParams.get("tableFields"));
        parameter.put("schemaMonitoring", taskParams.get("schemaMonitoring"));

        Map<String, Object> connection = new HashMap<>();
        // 多源表 UNION ALL 生成 querySql -> 走 customSql 路径
        if (taskParams.get("joinConfig") instanceof Map) {
            connection.put("querySql", JoinQueryBuilder.build(
                    (List<Map<String, Object>>) taskParams.get("sourceTables"),
                    (Map<String, Object>) taskParams.get("joinConfig"), readerProperty.getDbType(),
                    SourceMetadataSupport.config(taskParams)));
        } else {
            connection.put("querySql", buildUnionAllSql(taskParams, readerProperty.getDbType()));
        }
        connection.put("jdbcUrl", readerProperty.trainToJdbcUrl());
        parameter.put("connection", connection);
        parameter.put("readerProperty", readerProperty);
        return reader;
    }

    /**
     * 由多个源表生成 UNION ALL SQL。
     * SELECT 字段列表取 taskParams.columns；每个源表可带独立 where 条件。
     */
    private String buildUnionAllSql(Map<String, Object> taskParams, String dbType) {
        boolean component = Boolean.TRUE.equals(taskParams.get("componentStage"));
        Object columnsObj = taskParams.get("columns");
        List<String> columns = new ArrayList<>();
        if (columnsObj instanceof List) {
            for (Object item : (List<?>) columnsObj) {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    columns.add(String.valueOf(item));
                }
            }
        }
        if (columns.isEmpty()) {
            columns.add("*");
        }
        String columnSql = columns.stream().map(column -> component && !"*".equals(column)
                ? com.datamaster.module.collector.utils.StageIncrementalSql.quotedColumn(column, dbType) : column)
                .collect(java.util.stream.Collectors.joining(", "));
        String sourceTableColumn = SourceMetadataSupport.column(taskParams, "sourceTableColumn",
                SourceMetadataSupport.SOURCE_TABLE);

        Object sourceTablesObj = taskParams.get("sourceTables");
        if (!(sourceTablesObj instanceof List) || ((List<?>) sourceTablesObj).isEmpty()) {
            return null;
        }
        List<String> selectParts = new ArrayList<>();
        for (Object item : (List<?>) sourceTablesObj) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> table = (Map<String, Object>) item;
            String tableName = MapUtils.getString(table, "table_name");
            if (StringUtils.isBlank(tableName)) {
                tableName = MapUtils.getString(table, "tableName");
            }
            if (StringUtils.isBlank(tableName)) {
                continue;
            }
            String where = MapUtils.getString(table, "where");
            StringBuilder part = new StringBuilder("SELECT ").append(columnSql);
            if (SourceMetadataSupport.enabled(taskParams)
                    && !containsColumn(columns, sourceTableColumn)) {
                part.append(", '").append(escapeSqlLiteral(tableName)).append("' AS ")
                        .append(component ? com.datamaster.module.collector.utils.StageIncrementalSql.quotedColumn(sourceTableColumn, dbType) : sourceTableColumn);
            }
            part.append(" FROM ").append(component
                    ? com.datamaster.module.collector.utils.StageIncrementalSql.quotedTable(tableName, dbType) : tableName);
            if (StringUtils.isNotBlank(where)) {
                part.append(" WHERE ").append(where);
            }
            selectParts.add(part.toString());
        }
        if (selectParts.isEmpty()) {
            return null;
        }
        return String.join(" UNION ALL ", selectParts);
    }

    private boolean containsColumn(List<String> columns, String target) {
        if (columns == null || target == null) return false;
        for (String column : columns) {
            if (target.equalsIgnoreCase(column)) return true;
        }
        return false;
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
