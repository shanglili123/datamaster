

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
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService;

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
public class ProcessListener {

    @Resource
    private ICollectorEtlTaskInstanceService CollectorEtlTaskInstanceService;

//    @SneakyThrows
//    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(name = "ds.exchange.processInstance", type = "direct", durable = "true", autoDelete = "false"),
//            key = {"ds.queue.processInstance.insert"},
//            value = @Queue(value = "ds.queue.processInstance.insert", durable = "true", exclusive = "false", autoDelete = "false")))
//    public void processInstanceInsert(Map map, Channel channel, Message message) {
//        log.error("流程实例创建消息开始>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        ProcessInstance processInstance = JSON.parseObject(JSON.toJSONString(map), ProcessInstance.class);
//        try {
//            CollectorEtlTaskInstanceService.createTaskInstance(processInstance);
//        } catch (ServiceException serviceException) {
//            log.error("创建流程实例异常:{}", serviceException.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        // 手动确认
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        log.info("流程实例创建消息结束>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//    }


    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(name = "ds.exchange.processInstance", type = "direct", durable = "true", autoDelete = "false"),
            key = {"ds.queue.processInstance"},
            value = @Queue(value = "ds.queue.processInstance", durable = "true", exclusive = "false", autoDelete = "false")))
    public void processInstanceUpdate(Map map, Channel channel, Message message) {
        log.error("流程实例创建更新消息开始>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Integer type = (Integer) map.get("type");
        ProcessInstance processInstance = JSON.parseObject(JSON.toJSONString(map.get("instance")), ProcessInstance.class);
        Boolean flag = false;
        try {
            if (type == 1) {
                flag = CollectorEtlTaskInstanceService.createTaskInstance(processInstance);
            } else {
                flag = CollectorEtlTaskInstanceService.updateTaskInstance(processInstance);
            }
        } catch (ServiceException serviceException) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.error("创建更新任务实例异常:{}", serviceException.getMessage());
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            e.printStackTrace();
            return;
        }
        if (flag) {
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
        log.info(processInstance.getId() + "流程实例创建更新消息结束>>>>>>>>>>>>>>>>>>>>>>>>>>>" + flag);
    }
}
