

package com.datamaster.module.collector.listener;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeInstanceService;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskListener implements MessageListener {

    @Resource
    private ICollectorEtlNodeInstanceService CollectorEtlNodeInstanceService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String channel = new String(pattern, StandardCharsets.UTF_8);
        log.info("任务实例消息开始, channel: {} >>>>>>>>>>>>>>>>>>>>>>>>>>>>>", channel);
        TaskInstance taskInstance = JSON.parseObject(body, TaskInstance.class);
        try {
            if (channel.endsWith(":insert")) {
                CollectorEtlNodeInstanceService.createNodeInstance(taskInstance);
            } else {
                CollectorEtlNodeInstanceService.updateNodeInstance(taskInstance);
            }
        } catch (ServiceException serviceException) {
            log.error("任务实例处理异常:{}", serviceException.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info(taskInstance.getId() + "任务实例消息结束, channel: {} >>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + channel);
    }
}
