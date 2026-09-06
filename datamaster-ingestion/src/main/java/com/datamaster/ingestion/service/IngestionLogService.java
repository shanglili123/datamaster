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

    public void record(String topic, Integer partition, Long offset, String targetTable,
                       String status, String conflictAction, String keyValue, String messageBody, String message) {
        try {
            String body = messageBody;
            if (body != null && body.length() > MAX_BODY) {
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