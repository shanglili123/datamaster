package com.datamaster.flinkx.core;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.enums.CommandType;
import com.datamaster.common.enums.FailureStrategy;
import com.datamaster.common.enums.Flag;
import com.datamaster.common.enums.TaskExecutionStatus;
import com.datamaster.common.enums.WorkflowExecutionStatus;
import com.datamaster.flinkx.core.model.FlinkxJobBundle;
import com.datamaster.flinkx.core.model.FlinkxJobConfig;
import com.datamaster.flinkx.core.service.DolphinSchedulerTaskPublisher;
import com.datamaster.flinkx.core.service.FlinkxTaskConfigConverter;
import com.datamaster.flinkx.core.service.RabbitmqProcessPublisher;
import com.datamaster.flinkx.core.service.TaskLifecyclePublisher;
import com.datamaster.flinkx.core.util.EtlIdGenerator;
import com.datamaster.flinkx.core.util.EtlLog;
import com.datamaster.flinkx.core.util.RuntimeContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FlinkxEtlApplication {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            log.error("missing job payload");
            return;
        }

        RuntimeContext.init();

        String jsonStr = Base64.decodeStr(args[0]);
        JSONObject taskParams = JSONObject.parseObject(jsonStr);
        JSONObject config = taskParams.getJSONObject("config");
        JSONObject rabbitmq = config == null ? null : config.getJSONObject("rabbitmq");
        JSONObject redis = config == null ? null : config.getJSONObject("redis");
        JSONObject taskInfo = config == null ? null : config.getJSONObject("taskInfo");

        if (redis != null && !redis.isEmpty()) {
            RuntimeContext.initRedis(redis);
        }

        Date now = new Date();
        ProcessInstance processInstance = createProcess(taskInfo, now, rabbitmq);

        JSONObject reader = taskParams.getJSONObject("reader");
        JSONObject writer = taskParams.getJSONObject("writer");
        JSONArray transitionArr = taskParams.getJSONArray("transition");

        TaskInstance readerTaskInstance = createTask(processInstance, reader, now, rabbitmq);
        EtlLog.Params readerLogParams = new EtlLog.Params(rabbitmq, readerTaskInstance.getProcessInstanceId(), readerTaskInstance.getId());

        try {
            FlinkxJobBundle jobBundle = FlinkxTaskConfigConverter.convert(taskParams);
            FlinkxJobConfig jobConfig = jobBundle.getJobConfig();
            EtlLog.write(readerLogParams, "FlinkX任务配置已生成");

            TaskLifecyclePublisher.publishSuccess(readerTaskInstance, rabbitmq);

            if (transitionArr != null && !transitionArr.isEmpty()) {
                for (int i = 0; i < transitionArr.size(); i++) {
                    JSONObject transition = transitionArr.getJSONObject(i);
                    TaskInstance transitionTaskInstance = createTask(processInstance, transition, now, rabbitmq);
                    EtlLog.Params transitionLogParams = new EtlLog.Params(rabbitmq, transitionTaskInstance.getProcessInstanceId(), transitionTaskInstance.getId());
                    EtlLog.write(transitionLogParams, "FlinkX转换节点已折叠到单任务JSON：" + transition.getString("componentType"));
                    TaskLifecyclePublisher.publishSuccess(transitionTaskInstance, rabbitmq);
                    EtlLog.write(transitionLogParams, "任务成功");
                    EtlLog.write(transitionLogParams, "FINALIZE_SESSION");
                }
            }

            TaskInstance writerTaskInstance = createTask(processInstance, writer, now, rabbitmq);
            EtlLog.Params writerLogParams = new EtlLog.Params(rabbitmq, writerTaskInstance.getProcessInstanceId(), writerTaskInstance.getId());
            DolphinSchedulerTaskPublisher.publish(jobConfig, taskParams, writer, writerLogParams);
            TaskLifecyclePublisher.publishSuccess(writerTaskInstance, rabbitmq);
            updateProcess(processInstance, WorkflowExecutionStatus.SUCCESS, rabbitmq);
            EtlLog.write(writerLogParams, "任务成功");
            EtlLog.write(writerLogParams, "FINALIZE_SESSION");
        } catch (Exception e) {
            log.error("FlinkX ETL failed", e);
            TaskLifecyclePublisher.publishFailure(processInstance, rabbitmq);
            updateProcess(processInstance, WorkflowExecutionStatus.FAILURE, rabbitmq);
            EtlLog.write(new EtlLog.Params(rabbitmq, processInstance.getId(), processInstance.getId()), "失败原因:" + e.getMessage());
        }
    }

    public static ProcessInstance createProcess(JSONObject taskInfo, Date now, JSONObject rabbitmq) {
        ProcessInstance processInstance = ProcessInstance.builder()
                .id(EtlIdGenerator.getLongId())
                .name(taskInfo == null ? "flinkx-etl" : taskInfo.getString("name") + "-" + taskInfo.getInteger("taskVersion") + "-" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS"))
                .projectCode(taskInfo == null ? "" : taskInfo.getString("projectCode"))
                .processDefinitionCode(taskInfo == null ? "" : taskInfo.getString("taskCode"))
                .processDefinitionVersion(taskInfo == null ? 1 : taskInfo.getInteger("taskVersion"))
                .runTimes(1)
                .scheduleTime(now)
                .startTime(now)
                .commandStartTime(now)
                .commandType(CommandType.START_PROCESS)
                .failureStrategy(FailureStrategy.CONTINUE)
                .isSubProcess(Flag.NO)
                .state(WorkflowExecutionStatus.RUNNING_EXECUTION)
                .build();

        Map<String, Object> processInstanceMap = new HashMap<>();
        processInstanceMap.put("type", 1);
        processInstanceMap.put("instance", processInstance);
        RabbitmqProcessPublisher.publish(rabbitmq, processInstanceMap);
        return processInstance;
    }

    public static void updateProcess(ProcessInstance processInstance, WorkflowExecutionStatus status, JSONObject rabbitmq) {
        processInstance.setState(status);
        processInstance.setEndTime(new Date());
        Map<String, Object> processInstanceMap = new HashMap<>();
        processInstanceMap.put("type", 2);
        processInstanceMap.put("instance", processInstance);
        RabbitmqProcessPublisher.publish(rabbitmq, processInstanceMap);
    }

    public static TaskInstance createTask(ProcessInstance processInstance, JSONObject config, Date now, JSONObject rabbitmq) {
        String nodeName = config == null ? "node" : config.getString("nodeName");
        String nodeCode = config == null ? "node" : config.getString("nodeCode");
        Integer nodeVersion = config == null ? 1 : config.getInteger("nodeVersion");
        if (nodeVersion == null) {
            nodeVersion = 1;
        }
        TaskInstance taskInstance = TaskInstance.builder()
                .id(EtlIdGenerator.getLongId())
                .name(StrUtil.blankToDefault(nodeName, "node") + "-" + nodeVersion + "-" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS"))
                .processInstanceId(processInstance.getId())
                .taskCode(StrUtil.blankToDefault(nodeCode, "node"))
                .taskDefinitionVersion(nodeVersion)
                .taskType("CHUNJUN")
                .state(TaskExecutionStatus.RUNNING_EXECUTION)
                .startTime(now)
                .endTime(null)
                .retryTimes(0)
                .build();

        Map<String, Object> taskInstanceMap = new HashMap<>();
        taskInstanceMap.put("type", 1);
        taskInstanceMap.put("instance", taskInstance);
        TaskLifecyclePublisher.publish(rabbitmq, taskInstanceMap);
        return taskInstance;
    }
}
