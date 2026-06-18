

package com.datamaster.module.collector.utils.ds.component;

import com.datamaster.common.database.utils.MD5Util;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <P>
 * 用途:SQL-关系数据库 组件
 * </p>
 **/
public class SQLComponent implements ComponentItem {

    /**
     * taskParams SQL (关系数据库)
     * {
     *     "localParams": [],
     *     "resourceList": [],
     *     "type":"MYSQL",
     *     "datasource": 1,
     *     "sql":"",
     *     "sqlType":"0",
     *     "segmentSeparator":";",
     *     "preStatements":[],
     *     "postStatements":[],
     *     "displayRows":10
     * }
     */
    @Override
    public Map<String, Object> parse(Map<String, Object> params) {
        String dbType = (String) params.get("__datasourceType");
        Number dsId = (Number) params.get("__dsDatasourceId");
        if (dbType == null || dsId == null) {
            throw new ServiceException("数据源尚未同步到调度平台，请先同步");
        }

        Map<String, Object> taskParams = new LinkedHashMap<>();
        taskParams.put("localParams", params.getOrDefault("localParams", new ArrayList<>()));
        taskParams.put("resourceList", params.getOrDefault("resourceList", new ArrayList<>()));
        taskParams.put("type", MD5Util.getNormalizedDbType(dbType));
        taskParams.put("datasource", dsId != null ? dsId.longValue() : null);
        taskParams.put("sql", params.getOrDefault("sql", ""));
        taskParams.put("sqlType", params.getOrDefault("sqlType", ""));
        taskParams.put("segmentSeparator", params.getOrDefault("segm", ";"));
        taskParams.put("preStatements", params.getOrDefault("preStatements", new ArrayList<>()));
        taskParams.put("postStatements", params.getOrDefault("postStatements", new ArrayList<>()));
        taskParams.put("displayRows", params.getOrDefault("displayRows", 10));
        return taskParams;
    }

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        Map<String, Object> node = mapNode(nodeCode, nodeVersion, componentType);
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("localParams", taskParams.getOrDefault("localParams", new ArrayList<>()));
        parameter.put("resourceList", taskParams.getOrDefault("resourceList", new ArrayList<>()));
        parameter.put("type", taskParams.getOrDefault("__dsDatasourceType", ""));
        parameter.put("datasource", taskParams.getOrDefault("__dsDatasourceId", ""));
        parameter.put("sql", taskParams.getOrDefault("sql", ""));
        parameter.put("sqlType", taskParams.getOrDefault("sqlType", "0"));
        parameter.put("segmentSeparator", taskParams.getOrDefault("segm", ";"));
        parameter.put("preStatements", taskParams.getOrDefault("preStatements", new ArrayList<>()));
        parameter.put("postStatements", taskParams.getOrDefault("postStatements", new ArrayList<>()));
        parameter.put("displayRows", taskParams.getOrDefault("displayRows", 10));
        node.put("parameter", parameter);
        return node;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.SQL_DEV.getCode();
    }
}
