package com.datamaster.module.collector.utils.ds.component;

import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformSqlComponent implements ComponentItem {
    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        Map<String, Object> node = mapNode(nodeCode, nodeVersion, componentType);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("sql", taskParams.getOrDefault("sql", ""));
        parameter.put("inputFields", taskParams.getOrDefault("inputFields", new java.util.ArrayList<>()));
        node.put("parameter", parameter);
        return node;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.TRANSFORM_SQL.getCode();
    }
}
