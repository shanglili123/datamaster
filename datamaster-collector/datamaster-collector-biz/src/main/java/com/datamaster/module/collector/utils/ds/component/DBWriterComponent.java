

package com.datamaster.module.collector.utils.ds.component;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.utils.MD5Util;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.datax.FlinkxJson;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.*;
import java.util.stream.Collectors;

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
        String preSql = MapUtils.getString(taskParams, "preSql", "");
        String postSql = MapUtils.getString(taskParams, "postSql", "");

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
        //输入字段
        parameter.put("column", taskParams.get("columns"));
        //输出字段
        parameter.put("target_column", taskParams.get("target_columns"));
        parameter.put("column", taskParams.get("columns"));
        parameter.put("selectedColumns", taskParams.getOrDefault("selectedColumns", new ArrayList<>()));
        parameter.put("writeModeType", taskParams.get("writeModeType"));
        if (StringUtils.isNotBlank(preSql)) {
            parameter.put("preSql", preSql.split(","));
        }
        if (StringUtils.isNotBlank(postSql)) {
            parameter.put("postSql", postSql.split(","));
        }
        Map<String, Object> connection = new HashMap<>();
        connection.put("table", resolveTargetTableName(taskParams));
        connection.put("jdbcUrl", writerProperty.trainToJdbcUrl());
        parameter.put("connection", connection);
        parameter.put("writerProperty", writerProperty);
        return reader;
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
}
