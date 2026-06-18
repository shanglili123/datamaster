

package com.datamaster.module.collector.utils.ds.component;

import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <P>
 * 用途:Python开发 组件
 * </p>
 **/
public class PythonComponent implements ComponentItem {

    /**
     * taskParams PYTHON
     * {
     *     "localParams": [],
     *     "resourceList": [],
     *     "rawScript": "脚本"
     * }
     */
    @Override
    public Map<String, Object> parse(Map<String, Object> params) {
        Map<String, Object> taskParams = new LinkedHashMap<>();
        taskParams.put("localParams", params.getOrDefault("localParams", new ArrayList<>()));
        taskParams.put("resourceList", params.getOrDefault("resourceList", new ArrayList<>()));
        taskParams.put("rawScript", params.getOrDefault("rawScript", params.getOrDefault("sql", "")));
        return taskParams;
    }

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion, TaskComponentTypeEnum componentType, Map<String, Object> taskParams, String resourceUrl, List<DsResource> resourceList) {
        Map<String, Object> node = mapNode(nodeCode, nodeVersion, componentType);
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("localParams", taskParams.getOrDefault("localParams", new ArrayList<>()));
        parameter.put("resourceList", taskParams.getOrDefault("resourceList", new ArrayList<>()));
        parameter.put("rawScript", taskParams.getOrDefault("rawScript", taskParams.getOrDefault("sql", "")));
        node.put("parameter", parameter);
        return node;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.PYTHON_DEV.getCode();
    }
}
