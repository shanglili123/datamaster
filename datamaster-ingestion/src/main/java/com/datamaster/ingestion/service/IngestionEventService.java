package com.datamaster.ingestion.service;

import com.datamaster.ingestion.dal.dataobject.IngestionEventDO;
import com.datamaster.ingestion.dal.mapper.IngestionEventMapper;
import com.datamaster.ingestion.config.IngestionProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 跨实例 Kafka 事件租约与幂等状态服务。 */
@Slf4j
@Service
public class IngestionEventService {

    @Resource
    private IngestionEventMapper mapper;

    @Resource
    private IngestionProperties properties;

    private final String owner = UUID.randomUUID().toString();
    public ClaimResult claim(ConsumerRecord<String, String> record) {
        String eventKey = eventKey(record);
        Date leaseUntil = new Date(System.currentTimeMillis() + leaseMs());
        try {
            if (mapper.tryInsertClaim(eventKey, record.topic(), record.partition(), record.offset(), owner, leaseUntil) > 0) {
                return ClaimResult.CLAIMED;
            }
            IngestionEventDO existing = mapper.selectByEventKey(eventKey);
            if (existing != null && ("SUCCESS".equals(existing.getStatus())
                    || "CONFLICT_SKIP".equals(existing.getStatus())
                    || "DEAD_LETTER".equals(existing.getStatus()))) {
                return ClaimResult.COMPLETED;
            }
            if (mapper.reclaimExpired(eventKey, owner, leaseUntil) > 0) {
                return ClaimResult.CLAIMED;
            }
            return ClaimResult.BUSY;
        } catch (Exception e) {
            // 迁移未执行或数据库短暂不可用时，保留旧的日志幂等逻辑，不阻断主消费。
            log.warn("Kafka 事件幂等租约不可用，回退日志幂等检查 eventKey={}", eventKey, e);
            return ClaimResult.UNAVAILABLE;
        }
    }

    public void complete(ConsumerRecord<String, String> record, String status) {
        update(record, status, true);
    }

    public void fail(ConsumerRecord<String, String> record) {
        update(record, "FAILED", false);
    }

    /** 处理单条消息耗时较长时续租，避免租约过期后被其他实例重复领取。 */
    public void renew(ConsumerRecord<String, String> record) {
        try {
            mapper.renewLease(eventKey(record), owner,
                    new Date(System.currentTimeMillis() + leaseMs()));
        } catch (Exception e) {
            log.warn("续期 Kafka 事件租约失败 eventKey={}", eventKey(record), e);
        }
    }

    /** 返回当前持久化尝试次数；数据库不可用时返回 -1，由调用方回退内存计数。 */
    public int getAttemptCount(ConsumerRecord<String, String> record) {
        try {
            IngestionEventDO event = mapper.selectByEventKey(eventKey(record));
            return event == null || event.getAttemptCount() == null ? -1 : event.getAttemptCount();
        } catch (Exception e) {
            log.warn("读取 Kafka 事件持久化重试次数失败 eventKey={}", eventKey(record), e);
            return -1;
        }
    }

    private long leaseMs() {
        long configured = properties == null ? TimeUnit.MINUTES.toMillis(5)
                : properties.getKafka().getEventLeaseMs();
        return Math.max(10000L, configured);
    }

    private void update(ConsumerRecord<String, String> record, String status, boolean completed) {
        try {
            String eventKey = eventKey(record);
            if (completed) {
                mapper.markCompleted(eventKey, owner, status);
            } else {
                mapper.markFailed(eventKey, owner);
            }
        } catch (Exception e) {
            log.warn("更新 Kafka 事件幂等状态失败 eventKey={}", eventKey(record), e);
        }
    }

    private String eventKey(ConsumerRecord<String, String> record) {
        return record.topic() + ":" + record.partition() + ":" + record.offset();
    }

    public enum ClaimResult {
        CLAIMED,
        COMPLETED,
        BUSY,
        UNAVAILABLE
    }
}
