package com.datamaster.module.collector.utils.ds.component;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.utils.MD5Util;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;

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
        parameter.put("column", taskParams.get("columns"));
        parameter.put("tableFields", taskParams.get("tableFields"));

        Map<String, Object> connection = new HashMap<>();
        // 多源表 UNION ALL 生成 querySql -> 走 customSql 路径
        connection.put("querySql", buildUnionAllSql(taskParams));
        connection.put("jdbcUrl", readerProperty.trainToJdbcUrl());
        parameter.put("connection", connection);
        parameter.put("readerProperty", readerProperty);
        return reader;
    }

    /**
     * 由多个源表生成 UNION ALL SQL。
     * SELECT 字段列表取 taskParams.columns；每个源表可带独立 where 条件。
     */
    private String buildUnionAllSql(Map<String, Object> taskParams) {
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
        String columnSql = String.join(", ", columns);

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
            StringBuilder part = new StringBuilder("SELECT ").append(columnSql).append(" FROM ").append(tableName);
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
}
