package com.datamaster.ingestion.service;

import com.datamaster.ingestion.config.IngestionProperties;
import com.datamaster.ingestion.dal.dataobject.IngestionLogDO;
import com.datamaster.ingestion.dal.mapper.IngestionLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 死信查询和人工重放服务。 */
@Slf4j
@Service
public class IngestionDeadLetterService {

    @Resource
    private IngestionLogMapper ingestionLogMapper;

    @Resource
    private IngestionProperties properties;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        try {
            IngestionProperties.Kafka kafka = properties == null ? null : properties.getKafka();
            if (properties == null || !properties.isEnabled() || kafka == null
                    || kafka.getBootstrapServers() == null || kafka.getBootstrapServers().trim().isEmpty()) {
                log.info("ingestion 未启用或未配置 Kafka，跳过死信重放生产者初始化");
                return;
            }
            Map<String, Object> configs = new HashMap<>();
            configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            configs.put(ProducerConfig.ACKS_CONFIG, "all");
            configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
            producer = new KafkaProducer<>(configs);
        } catch (Exception e) {
            // ingestion 未启用或 Kafka 未配置时不阻断主服务启动；调用重放时返回失败。
            log.warn("初始化 ingestion 死信重放生产者失败", e);
        }
    }

    public List<IngestionLogDO> list(Integer limit) {
        return list(limit, null, null);
    }

    public List<IngestionLogDO> list(Integer limit, String topic, String targetTable) {
        try {
            int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
            return ingestionLogMapper.selectDeadLetters(safeLimit, topic, targetTable);
        } catch (Exception e) {
            log.warn("查询 ingestion 死信失败，可能尚未执行可靠性迁移", e);
            return java.util.Collections.emptyList();
        }
    }

    public boolean replay(Long logId) {
        if (logId == null || producer == null || !ingestionLogMapper.tryMarkReplaying(logId)) {
            return false;
        }
        IngestionLogDO deadLetter = ingestionLogMapper.selectById(logId);
        if (deadLetter == null || !"REPLAYING".equals(deadLetter.getStatus())
                || deadLetter.getTopic() == null || deadLetter.getMessageBody() == null) {
            ingestionLogMapper.updateReplayStatus(logId, "DEAD_LETTER", "死信重放前校验失败");
            return false;
        }
        if (!isConfiguredSourceTopic(deadLetter.getTopic())) {
            ingestionLogMapper.updateReplayStatus(logId, "DEAD_LETTER", "死信来源 topic 不在当前 ingestion topics 配置中");
            log.warn("拒绝重放未配置为源 topic 的死信 logId={} topic={}", logId, deadLetter.getTopic());
            return false;
        }
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    deadLetter.getTopic(), deadLetter.getMessageKey(), deadLetter.getMessageBody());
            record.headers().add(new RecordHeader("x-replayed-from-ingestion-log-id",
                    String.valueOf(logId).getBytes(StandardCharsets.UTF_8)));
            producer.send(record).get(10, TimeUnit.SECONDS);
            ingestionLogMapper.updateReplayStatus(logId, "REPLAYED", "死信已人工重放");
            return true;
        } catch (Exception e) {
            log.error("死信重放失败 logId={} topic={}", logId, deadLetter.getTopic(), e);
            ingestionLogMapper.updateReplayStatus(logId, "DEAD_LETTER", "死信重放失败: " + e.getMessage());
            return false;
        }
    }

    private boolean isConfiguredSourceTopic(String topic) {
        if (topic == null || properties == null || properties.getKafka() == null
                || properties.getKafka().getTopics() == null) {
            return false;
        }
        for (String configured : properties.getKafka().getTopics()) {
            if (configured != null && topic.equals(configured.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 批量重放，去重后最多处理 100 条，返回实际成功数量。 */
    public int replayBatch(Collection<Long> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            return 0;
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long id : logIds) {
            if (id != null && distinct.size() < 100) {
                distinct.add(id);
            }
        }
        int success = 0;
        for (Long id : distinct) {
            if (replay(id)) {
                success++;
            }
        }
        return success;
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            try {
                producer.close(Duration.ofSeconds(5));
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}
