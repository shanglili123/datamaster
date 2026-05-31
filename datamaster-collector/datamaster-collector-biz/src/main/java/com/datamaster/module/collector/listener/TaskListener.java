

package com.datamaster.module.collector.listener;

import com.alibaba.fastjson2.JSON;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeInstanceService;

import javax.annotation.Resource;
import java.util.Map;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-24 14:26
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskListener {

    @Resource
    private ICollectorEtlNodeInstanceService CollectorEtlNodeInstanceService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(name = "ds.exchange.taskInstance", type = "direct", durable = "true", autoDelete = "false"),
            key = {"ds.queue.taskInstance.insert"},
            value = @Queue(value = "ds.queue.taskInstance.insert", durable = "true", exclusive = "false", autoDelete = "false")))
    public void taskInstanceInsert(Map map, Channel channel, Message message) {
        log.info("任务实例创建消息开始>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        TaskInstance taskInstance = JSON.parseObject(JSON.toJSONString(map), TaskInstance.class);
        try {
            CollectorEtlNodeInstanceService.createNodeInstance(taskInstance);
        } catch (ServiceException serviceException) {
            log.error("创建任务实例异常:{}", serviceException.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("任务实例创建消息结束>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }


    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(name = "ds.exchange.taskInstance", type = "direct", durable = "true", autoDelete = "false"),
            key = {"ds.queue.taskInstance.update"},
            value = @Queue(value = "ds.queue.taskInstance.update", durable = "true", exclusive = "false", autoDelete = "false")))
    public void taskInstanceUpdate(Map map, Channel channel, Message message) {
        log.info("任务实例更新消息开始>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        TaskInstance taskInstance = JSON.parseObject(JSON.toJSONString(map), TaskInstance.class);
        Boolean flag = false;
        try {
            flag = CollectorEtlNodeInstanceService.updateNodeInstance(taskInstance);
        } catch (ServiceException serviceException) {
            log.error("更新任务实例异常:{}", serviceException.getMessage());
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            e.printStackTrace();
            return;
        }
        if (flag) {
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
        log.info(taskInstance.getId() + "任务实例更新消息结束>>>>>>>>>>>>>>>>>>>>>>>>>>>" + flag);
    }
}
