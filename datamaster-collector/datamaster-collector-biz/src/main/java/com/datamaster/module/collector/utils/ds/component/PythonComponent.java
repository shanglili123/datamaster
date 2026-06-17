

package com.datamaster.module.collector.utils.ds.component;

import com.datamaster.common.enums.TaskComponentTypeEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        taskParams.put("rawScript", params.getOrDefault("rawScript", ""));
        return taskParams;
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.PYTHON_DEV.getCode();
    }
}
