package com.datamaster.ingestion.controller.vo;

import com.datamaster.ingestion.config.IngestionProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 对外返回的数据接入配置摘要。
 *
 * <p>不能直接序列化 {@code IngestionProperties}，其中包含动作触发 secret 等
 * 仅供服务端使用的凭据。</p>
 */
@Data
public class IngestionConfigRespVO {

    private boolean enabled;
    private String kafkaBootstrapServers;
    private String kafkaGroupId;
    private List<String> kafkaTopics = new ArrayList<>();
    private String kafkaDeadLetterTopic;
    private long kafkaPollTimeoutMs;
    private long kafkaPollIntervalMs;
    private int kafkaMaxRetries;
    private long kafkaRetryBackoffMs;
    private long kafkaEventLeaseMs;
    private long kafkaLeaseRenewIntervalMs;
    private Long dorisDatasourceId;
    private String conflictStrategy;
    private List<String> keyColumns = new ArrayList<>();
    private String targetTable;
    private List<String> allowedTables = new ArrayList<>();
    private List<String> requiredColumns = new ArrayList<>();
    private String schemaVersion;
    private boolean decisionTriggerEnabled;
    private String decisionTriggerBaseUrl;
    private String decisionTriggerPath;
    private String decisionTriggerRef;
    /** 与旧版直接返回 properties 时的字段层级兼容，但只包含脱敏字段。 */
    private KafkaSummary kafka;
    private DecisionTriggerSummary decisionTrigger;

    public static IngestionConfigRespVO from(IngestionProperties properties) {
        IngestionConfigRespVO result = new IngestionConfigRespVO();
        if (properties == null) {
            return result;
        }
        result.setEnabled(properties.isEnabled());
        result.setDorisDatasourceId(properties.getDorisDatasourceId());
        result.setConflictStrategy(properties.getConflictStrategy());
        result.setKeyColumns(copy(properties.getKeyColumns()));
        result.setTargetTable(properties.getTargetTable());
        result.setAllowedTables(copy(properties.getAllowedTables()));
        result.setRequiredColumns(copy(properties.getRequiredColumns()));
        result.setSchemaVersion(properties.getSchemaVersion());

        IngestionProperties.Kafka kafka = properties.getKafka();
        if (kafka != null) {
            result.setKafkaBootstrapServers(kafka.getBootstrapServers());
            result.setKafkaGroupId(kafka.getGroupId());
            result.setKafkaTopics(copy(kafka.getTopics()));
            result.setKafkaDeadLetterTopic(kafka.getDeadLetterTopic());
            result.setKafkaPollTimeoutMs(kafka.getPollTimeoutMs());
            result.setKafkaPollIntervalMs(kafka.getPollIntervalMs());
            result.setKafkaMaxRetries(kafka.getMaxRetries());
            result.setKafkaRetryBackoffMs(kafka.getRetryBackoffMs());
            result.setKafkaEventLeaseMs(kafka.getEventLeaseMs());
            result.setKafkaLeaseRenewIntervalMs(kafka.getLeaseRenewIntervalMs());
            KafkaSummary summary = new KafkaSummary();
            summary.bootstrapServers = kafka.getBootstrapServers();
            summary.groupId = kafka.getGroupId();
            summary.topics = copy(kafka.getTopics());
            summary.deadLetterTopic = kafka.getDeadLetterTopic();
            summary.pollTimeoutMs = kafka.getPollTimeoutMs();
            summary.pollIntervalMs = kafka.getPollIntervalMs();
            summary.maxRetries = kafka.getMaxRetries();
            summary.retryBackoffMs = kafka.getRetryBackoffMs();
            summary.eventLeaseMs = kafka.getEventLeaseMs();
            summary.leaseRenewIntervalMs = kafka.getLeaseRenewIntervalMs();
            result.setKafka(summary);
        }

        IngestionProperties.DecisionTrigger trigger = properties.getDecisionTrigger();
        if (trigger != null) {
            result.setDecisionTriggerEnabled(trigger.isEnabled());
            result.setDecisionTriggerBaseUrl(trigger.getBaseUrl());
            result.setDecisionTriggerPath(trigger.getPath());
            result.setDecisionTriggerRef(trigger.getTriggerRef());
            DecisionTriggerSummary summary = new DecisionTriggerSummary();
            summary.enabled = trigger.isEnabled();
            summary.baseUrl = trigger.getBaseUrl();
            summary.path = trigger.getPath();
            summary.triggerRef = trigger.getTriggerRef();
            result.setDecisionTrigger(summary);
            // trigger.secret intentionally omitted.
        }
        return result;
    }

    private static List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    @Data
    public static class KafkaSummary {
        private String bootstrapServers;
        private String groupId;
        private List<String> topics = new ArrayList<>();
        private String deadLetterTopic;
        private long pollTimeoutMs;
        private long pollIntervalMs;
        private int maxRetries;
        private long retryBackoffMs;
        private long eventLeaseMs;
        private long leaseRenewIntervalMs;
    }

    @Data
    public static class DecisionTriggerSummary {
        private boolean enabled;
        private String baseUrl;
        private String path;
        private String triggerRef;
    }
}
