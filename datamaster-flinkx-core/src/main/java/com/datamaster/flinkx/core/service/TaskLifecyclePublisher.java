package com.datamaster.flinkx.core.service;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.flinkx.core.util.EtlLog;

import java.util.Map;

public class TaskLifecyclePublisher {
    public static void publishSuccess(TaskInstance taskInstance, JSONObject rabbitmq) {
        taskInstance.setState(com.datamaster.common.enums.TaskExecutionStatus.SUCCESS);
    }

    public static void publishFailure(ProcessInstance processInstance, JSONObject rabbitmq) {
        processInstance.setState(com.datamaster.common.enums.WorkflowExecutionStatus.FAILURE);
    }

    public static void publish(JSONObject rabbitmq, Map<String, Object> payload) {
        // bridge reserved for future MQ integration
    }
}
