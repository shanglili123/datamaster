

package com.datamaster.spark.etl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.enums.*;
import com.datamaster.spark.etl.reader.ReaderFactory;
import com.datamaster.spark.etl.transition.TransitionFactory;
import com.datamaster.spark.etl.utils.IDGeneratorUtils;
import com.datamaster.spark.etl.utils.LogUtils;
import com.datamaster.spark.etl.utils.RedisUtils;
import com.datamaster.spark.etl.utils.db.DBUtils;
import com.datamaster.spark.etl.writer.WriterFactory;


import java.util.*;

/**
 * <P>
 * 用途:ETL程序入口
 * </p>
 *
 * @author: FXB
 * @create: 2025-04-16 09:43
 **/
@Slf4j
public class EtlApplication {

    public static void main(String[] args) {
        DBUtils.init();

        Date now = new Date();
        log.info(args[0]);
        String jsonStr = Base64.decodeStr(args[0]);
        log.info(jsonStr);
        JSONObject taskParams = JSONObject.parseObject(jsonStr);
        JSONObject config = taskParams.getJSONObject("config");
        JSONObject redis = config.getJSONObject("redis");
        JSONObject taskInfo = config.getJSONObject("taskInfo");

        // 初始化redis
        if (redis != null && redis.size() > 0) {
            RedisUtils.init(redis);
        }

        //创建流程实例
        ProcessInstance processInstance = createProcess(taskInfo, now);

        //注册spark
        SparkConf conf = new SparkConf().setAppName("EtlApplication");

        SparkSession spark = SparkSession.builder()
                .config(conf)
                .getOrCreate();

        //读取配置
        JSONObject reader = taskParams.getJSONObject("reader");
        //参数信息
        JSONObject readParameter = reader.getJSONObject("parameter");

        //输入类型
        TaskComponentTypeEnum readerComponentType = TaskComponentTypeEnum.findEnumByType(reader.getString("componentType"));

        //输入字段
        List<String> readerColumns = new ArrayList<>();

        //创建输入节点实例
        TaskInstance readerTaskInstance = createTask(processInstance, reader, now);
        LogUtils.Params readerLogParams = new LogUtils.Params(readerTaskInstance.getProcessInstanceId(), readerTaskInstance.getId());

        //读取数据集
        Dataset<Row> data;
        try {
            data = ReaderFactory.getReader(readerComponentType.getCode())
                    .read(spark, reader, readerColumns, readerLogParams);
            if (data == null) {
                LogUtils.writeLog(readerLogParams, "任务失败");
                updateProcess(processInstance, WorkflowExecutionStatus.FAILURE);
                //更新输入节点实例执行失败
                updateTask(readerTaskInstance, TaskExecutionStatus.FAILURE);
                spark.stop();
                return;
            }
        } catch (Exception e) {
            log.error("任务失败", e);
            updateProcess(processInstance, WorkflowExecutionStatus.FAILURE);
            //更新输入节点实例执行失败
            updateTask(readerTaskInstance, TaskExecutionStatus.FAILURE);
            LogUtils.writeLog(readerLogParams, "失败原因:" + e.getMessage());
            LogUtils.writeLog(readerLogParams, "任务失败");
            LogUtils.writeLog(readerLogParams, "FINALIZE_SESSION");
            spark.stop();
            return;
        }

        //更新输入节点实例执行成功
        updateTask(readerTaskInstance, TaskExecutionStatus.SUCCESS);
        LogUtils.writeLog(readerLogParams, "任务成功");
        LogUtils.writeLog(readerLogParams, "FINALIZE_SESSION");

//        if (readParameter.containsKey("batchSize")) {
//            //分批处理
//            data = data.repartition(readParameter.getInteger("batchSize"));
//        }

        if (taskParams.getJSONArray("transition") != null && taskParams.getJSONArray("transition").size() > 0) {
            //读取配置
            JSONArray transitionArr = taskParams.getJSONArray("transition");
            for (int i = 0; i < transitionArr.size(); i++) {
                JSONObject transition = (JSONObject) transitionArr.get(i);
                //转换类型
                TaskComponentTypeEnum transitionComponentType = TaskComponentTypeEnum.findEnumByType(transition.getString("componentType"));

                //创建转换节点实例
                TaskInstance transitionTaskInstance = createTask(processInstance, transition, now);
                LogUtils.Params transitionLogParams = new LogUtils.Params(transitionTaskInstance.getProcessInstanceId(), transitionTaskInstance.getId());

                try {
                    data = TransitionFactory.getTransition(transitionComponentType.getCode())
                            .transition(spark, data, transition, transitionLogParams);
                } catch (Exception e) {
                    //更新清洗节点实例执行失败
                    updateProcess(processInstance, WorkflowExecutionStatus.FAILURE);
                    updateTask(transitionTaskInstance, TaskExecutionStatus.FAILURE);
                    spark.stop();
                    LogUtils.writeLog(transitionLogParams, "失败原因:" + e.getMessage());
                    LogUtils.writeLog(transitionLogParams, "任务失败");
                    LogUtils.writeLog(transitionLogParams, "FINALIZE_SESSION");
                    spark.stop();
                    return;
                }
                //更新输入节点实例执行成功
                updateTask(transitionTaskInstance, TaskExecutionStatus.SUCCESS);
                LogUtils.writeLog(transitionLogParams, "任务成功");
                LogUtils.writeLog(transitionLogParams, "FINALIZE_SESSION");
            }
        }
        //写入配置
        JSONObject writer = taskParams.getJSONObject("writer");
        //输出类型
        TaskComponentTypeEnum writerComponentType = TaskComponentTypeEnum.findEnumByType(writer.getString("componentType"));


        //创建输出节点实例
        TaskInstance writerTaskInstance = createTask(processInstance, writer, now);

        LogUtils.Params writerLogParams = new LogUtils.Params(writerTaskInstance.getProcessInstanceId(), writerTaskInstance.getId());

        Boolean flag = false;
        try {
            flag = WriterFactory.getWriter(writerComponentType.getCode())
                    .writer(config, data, writer, writerLogParams);
        } catch (Exception e) {
            log.error("任务失败", e);
            LogUtils.writeLog(writerLogParams, "失败原因:" + e.getMessage());
        }

        if (flag) {
            updateTask(writerTaskInstance, TaskExecutionStatus.SUCCESS);
            updateProcess(processInstance, WorkflowExecutionStatus.SUCCESS);
            LogUtils.writeLog(writerLogParams, "任务成功");
            LogUtils.writeLog(writerLogParams, "FINALIZE_SESSION");
            //判断是否存在数据缓存
            if (reader.containsKey("cacheDataMap")) {
                Map<String, String> cacheDataMap = (Map<String, String>) reader.get("cacheDataMap");
                cacheDataMap.forEach((key, value) -> {
                    RedisUtils.set(key, value, -1);
                });
            }
        } else {
            updateTask(writerTaskInstance, TaskExecutionStatus.FAILURE);
            updateProcess(processInstance, WorkflowExecutionStatus.FAILURE);
            LogUtils.writeLog(writerLogParams, "任务失败");
            LogUtils.writeLog(writerLogParams, "FINALIZE_SESSION");
        }
        spark.stop();
    }

    public static ProcessInstance createProcess(JSONObject taskInfo, Date now) {
        ProcessInstance processInstance = ProcessInstance.builder()
                .id(IDGeneratorUtils.getLongId())
                .name(taskInfo.getString("name") + "-" + taskInfo.getInteger("taskVersion") + "-" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS"))
                .projectCode(taskInfo.getString("projectCode"))
                .processDefinitionCode(taskInfo.getString("taskCode"))
                .processDefinitionVersion(taskInfo.getInteger("taskVersion"))
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

        RedisUtils.publish("ds:channel:processInstance", processInstanceMap);
        return processInstance;
    }

    public static void updateProcess(ProcessInstance processInstance, WorkflowExecutionStatus status) {
        processInstance.setState(status);
        processInstance.setEndTime(new Date());

        Map<String, Object> processInstanceMap = new HashMap<>();
        processInstanceMap.put("type", 2);
        processInstanceMap.put("instance", processInstance);

        RedisUtils.publish("ds:channel:processInstance", processInstanceMap);
    }

    public static TaskInstance createTask(ProcessInstance processInstance, JSONObject config, Date now) {
        String nodeName = config.getString("nodeName");
        String nodeCode = config.getString("nodeCode");
        Integer nodeVersion = config.getInteger("nodeVersion");
        TaskInstance taskInstance = TaskInstance.builder()
                .id(IDGeneratorUtils.getLongId())
                .name(nodeName)
                .taskCode(nodeCode)
                .taskDefinitionVersion(nodeVersion)
                .taskType("SPARK")
                .processInstanceId(processInstance.getId())
                .processInstanceName(processInstance.getName())
                .projectCode(config.getString("projectCode"))
                .taskInstancePriority(Priority.MEDIUM)
                .startTime(now)
                .state(TaskExecutionStatus.RUNNING_EXECUTION)
                .build();
        RedisUtils.publish("ds:channel:taskInstance:insert", taskInstance);
        return taskInstance;
    }


    public static void updateTask(TaskInstance taskInstance, TaskExecutionStatus status) {
        taskInstance.setState(status);
        taskInstance.setEndTime(new Date());
        RedisUtils.publish("ds:channel:taskInstance:update", taskInstance);
    }
}
