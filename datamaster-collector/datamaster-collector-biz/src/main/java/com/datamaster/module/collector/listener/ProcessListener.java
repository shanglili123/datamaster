

package com.datamaster.module.collector.listener;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService;
import com.datamaster.module.collector.service.etl.ICollectorEtlIncrementalService;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlTaskStatusPushService;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessListener implements MessageListener {

    @Resource
    private ICollectorEtlTaskInstanceService CollectorEtlTaskInstanceService;
    @Resource
    private ICollectorEtlIncrementalService collectorEtlIncrementalService;
    @Resource
    private CollectorEtlTaskStatusPushService collectorEtlTaskStatusPushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("流程实例创建更新消息开始>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Map map = JSON.parseObject(body, Map.class);
        Integer type = (Integer) map.get("type");
        ProcessInstance processInstance = JSON.parseObject(JSON.toJSONString(map.get("instance")), ProcessInstance.class);
        boolean flag = false;
        try {
            if (type == 1) {
                flag = CollectorEtlTaskInstanceService.createTaskInstance(processInstance);
            } else {
                flag = CollectorEtlTaskInstanceService.updateTaskInstance(processInstance);
            }
        } catch (ServiceException serviceException) {
            log.error("创建更新任务实例异常:{}", serviceException.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        pushTaskStatus(processInstance);
        releaseIncrementalSlot(processInstance);
        log.info(processInstance.getId() + "流程实例创建更新消息结束>>>>>>>>>>>>>>>>>>>>>>>>>>>" + flag);
    }

    private void pushTaskStatus(ProcessInstance processInstance) {
        if (processInstance == null || processInstance.getId() == null) {
            return;
        }
        CollectorEtlTaskInstanceDO instance = CollectorEtlTaskInstanceService.getByDsId(processInstance.getId());
        collectorEtlTaskStatusPushService.pushTaskInstanceStatus(instance);
    }

    private void releaseIncrementalSlot(ProcessInstance processInstance) {
        if (processInstance == null || processInstance.getState() == null || !processInstance.getState().isFinished()) {
            return;
        }
        CollectorEtlTaskInstanceDO instance = CollectorEtlTaskInstanceService.getByDsId(processInstance.getId());
        if (instance != null && instance.getTaskId() != null) {
            try {
                collectorEtlIncrementalService.releaseIncrementalTask(instance.getTaskId(), processInstance.getId());
            } catch (Exception e) {
                log.warn("释放FLINKX增量任务运行占用失败，taskId={}，processInstanceId={}",
                        instance.getTaskId(), processInstance.getId(), e);
            }
        }
    }
}
