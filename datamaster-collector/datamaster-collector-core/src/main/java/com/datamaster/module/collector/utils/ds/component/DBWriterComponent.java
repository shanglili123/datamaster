

package com.datamaster.module.collector.utils.ds.component;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.utils.MD5Util;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.datax.FlinkxJson;
import com.datamaster.module.collector.utils.model.DsResource;
import com.datamaster.module.collector.utils.SourceMetadataSupport;
import com.datamaster.common.utils.sql.SqlStatementParser;

import java.util.*;

/**
 * <P>
 * 用途:数据库输出组件
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-12 16:31
 **/
public class DBWriterComponent implements ComponentItem {

    /**
     * {
     * "batchSize":1024,//一次性写入量，可空，空时datax默认1024
     * "preSql": "",//前置sql 生成datax json时会转换成数组(已分号进行切割)
     * "postSql": "",//后置置sql  生成datax json时会转换成数组(已分号进行切割)
     * "target_datasource_id":1,//目标数据源id 如果是输入节点时该字段为null
     * "target_asset_id":1,//目标资产id 如果是输入节点时该字段为null
     * "target_table_name":1,//目标表名 如果是输入节点时该字段为在后端自动生成
     * "target_columns":["ID","NAME","AGE"],//目标表同步字段列表 如果是输入节点时该字段为在后端自动生成
     * "writeModeType": //写入类型 1 全量，2 追加写，3 增量更新
     * "selectedColumns":["ID"],//增量更新主键
     * }
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> parse(Map<String, Object> params) {
        Map<String, Object> taskParams = new LinkedHashMap<>();
        taskParams.put("localParams", params.getOrDefault("localParams", new ArrayList<>())); // 默认空列表
        taskParams.put("resourceList", params.getOrDefault("resourceList", new ArrayList<>())); // 默认空列表
        taskParams.put("customConfig", params.getOrDefault("customConfig", 1)); // 默认写死1
        taskParams.put("xms", params.getOrDefault("xms", 1)); // 默认1
        taskParams.put("xmx", params.getOrDefault("xmx", 1)); // 默认1
        taskParams.put("json", FlinkxJson.buildJobJsonMasterdata(params)); // 默认空的JSON字符串
        return taskParams;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.DB_WRITER.getCode();
    }

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        List<String> preSql = SqlStatementParser.parse(taskParams.get("preSql"));
        List<String> postSql = SqlStatementParser.parse(taskParams.get("postSql"));
        if (preSql.isEmpty() && "1".equals(String.valueOf(taskParams.get("writeModeType")))) {
            String targetTable = resolveTargetTableName(taskParams);
            if (StringUtils.isNotBlank(targetTable)) {
                preSql.add("TRUNCATE TABLE " + targetTable);
            }
        }

        // reader 配置
        Map<String, Object> reader = new HashMap<>();
        // writerDatasource
        Map<String, Object> writerDatasource = (Map<String, Object>) MapUtils.getObject(taskParams, "writerDatasource");
        DbQueryProperty writerProperty = MD5Util.buildJobDatasource(writerDatasource);
        reader.put("nodeCode", nodeCode);
        reader.put("nodeVersion", nodeVersion);
        reader.put("componentType", componentType.getCode());

        //参数
        Map<String, Object> parameter = new HashMap<>();
        reader.put("parameter", parameter);

        parameter.put("batchSize", taskParams.getOrDefault("batchSize", 1024));
        parameter.put("datasourceId", writerDatasource.get("datasourceId"));
        parameter.put("username", writerProperty.getUsername());
        parameter.put("password", writerProperty.getPassword());
        parameter.put("dbType", writerProperty.getDbType());
        parameter.put("host", writerProperty.getHost());
        parameter.put("port", writerProperty.getPort());
        parameter.put("datasourceConfig", writerProperty.getDatasourceConfig());
        if (StringUtils.isNotBlank(writerProperty.getDbName())) {
            parameter.put("dbName", writerProperty.getDbName());
        }
        if (StringUtils.isNotBlank(writerProperty.getSid())) {
            parameter.put("sid", writerProperty.getSid());
        }
        // 输入/输出字段。来源元数据由 reader/transformation 生成，同时补到 writer 字段列表。
        if (SourceMetadataSupport.enabled(taskParams)) {
            List<String> metadataColumns = SourceMetadataSupport.metadataColumns(taskParams);
            Object inputColumns = taskParams.get("columns");
            Object targetColumns = taskParams.get("target_columns");
            // 历史草稿或逻辑节点可能只保存了输入字段；目标字段为空时按同名字段写入，
            // 再追加治理列，避免最终 writer 只剩 source_table / write_time。
            if (isEmptyColumns(targetColumns)) {
                targetColumns = inputColumns;
            }
            parameter.put("column", SourceMetadataSupport.appendColumns(inputColumns, metadataColumns));
            parameter.put("target_column", SourceMetadataSupport.appendColumns(
                    targetColumns, metadataColumns));
            parameter.put("sourceMetadata", SourceMetadataSupport.copyWithRuntimeValues(
                    taskParams, nodeCode,
                    writerDatasource == null ? null : String.valueOf(writerDatasource.get("datasourceId")),
                    resolveTargetTableName(taskParams)));
        } else {
            parameter.put("column", taskParams.get("columns"));
            parameter.put("target_column", taskParams.get("target_columns"));
        }
        Object targetFields = taskParams.get("outputFields");
        if (!(targetFields instanceof List) || ((List<?>) targetFields).isEmpty()) targetFields = taskParams.get("toColumnsList");
        parameter.put("tableFields", targetFields);
        parameter.put("qualityConfig", taskParams.get("qualityConfig"));
        parameter.put("selectedColumns", normalizeColumnList(taskParams.get("selectedColumns")));
        parameter.put("writeModeType", taskParams.get("writeModeType"));
        if (!preSql.isEmpty()) {
            parameter.put("preSql", preSql);
        }
        if (!postSql.isEmpty()) {
            parameter.put("postSql", postSql);
        }

        // Kafka 输出：无 JDBC/表，透传 kafkaWriterConfig（topic/分区键）供 Flinkx kafkawriter 使用
        if (DbType.KAFKA.getDb().equalsIgnoreCase(writerProperty.getDbType())) {
            Object kafkaWriterConfigObj = taskParams.get("kafkaWriterConfig");
            @SuppressWarnings("unchecked")
            Map<String, Object> kafkaWriterConfig = kafkaWriterConfigObj instanceof Map
                    ? (Map<String, Object>) kafkaWriterConfigObj
                    : new LinkedHashMap<String, Object>();
            parameter.put("kafkaWriterConfig", kafkaWriterConfig);
            Object topic = kafkaWriterConfig.get("topic");
            if (StringUtils.isNotBlank(MapUtils.getString(taskParams, "topic"))) {
                topic = MapUtils.getString(taskParams, "topic");
            }
            if (topic != null) {
                parameter.put("topic", topic);
            }
            Object topics = kafkaWriterConfig.get("topics");
            if (topics != null) {
                parameter.put("topics", topics);
            }
            List<String> partitionAssignColumns = new ArrayList<>();
            Object rawPartition = kafkaWriterConfig.get("partitionAssignColumns");
            if (rawPartition instanceof List) {
                for (Object item : (List<?>) rawPartition) {
                    partitionAssignColumns.add(String.valueOf(item));
                }
            } else if (rawPartition instanceof String && StringUtils.isNotBlank((String) rawPartition)) {
                partitionAssignColumns.add((String) rawPartition);
            }
            if (!partitionAssignColumns.isEmpty()) {
                parameter.put("partitionAssignColumns", partitionAssignColumns);
            }
            // Kafka 无连接参数
            parameter.put("writerProperty", writerProperty);
            return reader;
        }

        Map<String, Object> connection = new HashMap<>();
        connection.put("table", resolveTargetTableName(taskParams));
        connection.put("jdbcUrl", writerProperty.trainToJdbcUrl());
        parameter.put("connection", connection);
        parameter.put("writerProperty", writerProperty);
        return reader;
    }

    private boolean isEmptyColumns(Object columns) {
        if (columns == null) return true;
        if (columns instanceof Collection) return ((Collection<?>) columns).isEmpty();
        return StringUtils.isBlank(String.valueOf(columns));
    }

    private String resolveTargetTableName(Map<String, Object> taskParams) {
        String tableName = MapUtils.getString(taskParams, "target_table_name");
        if (StringUtils.isNotBlank(tableName)) {
            return tableName;
        }
        tableName = MapUtils.getString(taskParams, "table_name");
        if (StringUtils.isNotBlank(tableName)) {
            return tableName;
        }
        tableName = MapUtils.getString(taskParams, "target_asset_id");
        if (StringUtils.isNotBlank(tableName)) {
            return tableName;
        }
        return null;
    }

    private List<String> normalizeColumnList(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    result.add(String.valueOf(item).trim());
                }
            }
            return result;
        }
        if (raw != null && StringUtils.isNotBlank(String.valueOf(raw))) {
            for (String item : String.valueOf(raw).split(",")) {
                if (StringUtils.isNotBlank(item)) result.add(item.trim());
            }
        }
        return result;
    }
}
