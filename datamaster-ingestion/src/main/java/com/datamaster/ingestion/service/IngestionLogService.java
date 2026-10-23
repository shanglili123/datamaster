package com.datamaster.ingestion.service;

import com.datamaster.ingestion.dal.dataobject.IngestionLogDO;
import com.datamaster.ingestion.dal.mapper.IngestionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 数据接收处理日志记录
 */
@Slf4j
@Service
public class IngestionLogService {

    private static final int MAX_BODY = 2000;

    @Resource
    private IngestionLogMapper ingestionLogMapper;

    /**
     * Kafka 消费前检查是否已经完成落库。查询失败时按未处理返回，
     * 让主流程继续处理，避免日志库短暂不可用直接阻断数据接入。
     */
    public boolean isProcessed(String topic, Integer partition, Long offset) {
        try {
            return ingestionLogMapper.selectProcessed(topic, partition, offset) != null;
        } catch (Exception e) {
            log.warn("查询数据接入幂等记录失败，按未处理继续 topic={} partition={} offset={}",
                    topic, partition, offset, e);
            return false;
        }
    }

    public void record(String topic, Integer partition, Long offset, String targetTable,
                       String status, String conflictAction, String keyValue, String messageBody, String message) {
        record(topic, partition, offset, null, targetTable, status, conflictAction,
                keyValue, messageBody, message);
    }

    /** 记录带 Kafka key 的消息日志；保留旧重载兼容已有调用方。 */
    public void record(String topic, Integer partition, Long offset, String messageKey,
                       String targetTable, String status, String conflictAction,
                       String keyValue, String messageBody, String message) {
        try {
            String body = messageBody;
            // 死信必须保留完整原文，否则人工重放可能发送截断 JSON；普通运行日志继续限制大小。
            if (body != null && body.length() > MAX_BODY && !"DEAD_LETTER".equals(status)) {
                body = body.substring(0, MAX_BODY);
            }
            String msg = message;
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            IngestionLogDO logDO = IngestionLogDO.builder()
                    .topic(topic)
                    .partition(partition)
                    .msgOffset(offset)
                    .messageKey(messageKey)
                    .targetTable(targetTable)
                    .status(status)
                    .conflictAction(conflictAction)
                    .keyValue(keyValue)
                    .messageBody(body)
                    .message(msg)
                    .build();
            ingestionLogMapper.insert(logDO);
        } catch (Exception e) {
            // 日志写入失败不影响主流程
            log.warn("记录数据接收日志失败 topic={} offset={}", topic, offset, e);
        }
    }
}
