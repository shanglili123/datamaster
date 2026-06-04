

package com.datamaster.module.collector.listener;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeInstanceService;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskLogListener implements MessageListener {

    @Resource
    private ICollectorEtlNodeInstanceService CollectorEtlNodeInstanceService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Map map = JSON.parseObject(body, Map.class);
        String taskInstanceId = String.valueOf(map.get("taskInstanceId"));
        String processInstanceId = String.valueOf(map.get("workflowInstanceId"));
        String logStr = String.valueOf(map.get("log"));
        try {
            CollectorEtlNodeInstanceService.taskInstanceLogInsert(taskInstanceId, processInstanceId, logStr);
        } catch (Exception e) {
            log.error("任务实例日志插入异常:{}", e.getMessage());
        }
    }
}
