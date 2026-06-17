

package com.datamaster.module.collector.utils.ds.component;

import com.datamaster.common.database.utils.MD5Util;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <P>
 * 用途:存储过程
 * </p>
 **/
public class ProcedureComponent implements ComponentItem {

    /**
     *
     * taskParams PROCEDURE (存储过程)
     * {
     *     "localParams": [],
     *     "resourceList": [],
     *     "type":"MYSQL",
     *     "datasource": 1,
     *     "method":"call test(${in1},${out1});",
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
        taskParams.put("method", params.getOrDefault("sql", ""));
        taskParams.put("preStatements", params.getOrDefault("preStatements", new ArrayList<>()));
        taskParams.put("postStatements", params.getOrDefault("postStatements", new ArrayList<>()));
        taskParams.put("displayRows", params.getOrDefault("displayRows", 10));
        return taskParams;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.PROCEDURE_DEV.getCode();
    }
}
