package com.datamaster.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据接收工程配置
 *
 * <p>通过 Spring 配置前缀 <code>datamaster.ingestion</code> 注入：</p>
 * <pre>
 * datamaster:
 *   ingestion:
 *     enabled: true
 *     kafka:
 *       bootstrap-servers: localhost:9092
 *       group-id: datamaster-ingestion
 *       topics: [customer_stream]
 *     doris-datasource-id: 1
 *     conflict-strategy: UPSERT   # INSERT / UPSERT / SKIP / ERROR
 *     key-columns: [id]
 *     decision-trigger:
 *       base-url: ${datamaster.server-url:}  # 本体决策数据到达触发回调地址
 *       enabled: false
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "datamaster.ingestion")
public class IngestionProperties {

    /** 是否启用数据接收处理 */
    private boolean enabled = true;

    /** Kafka 消费配置 */
    private Kafka kafka = new Kafka();

    /** 目标数据仓库（Doris）数据源ID */
    private Long dorisDatasourceId;

    /** 冲突策略：INSERT(遇到已存在改插入)/UPSERT(存在则更新)/SKIP(存在则跳过)/ERROR(存在则报错) */
    private String conflictStrategy = "UPSERT";

    /** 冲突判定主键列（必填，用于查询目标表是否存在同键记录） */
    private List<String> keyColumns = new ArrayList<>();

    /** 写入目标表名（缺省从 Kafka 消息 header/字段 table 读取） */
    private String targetTable;

    /** 本体动作数据到达触发（保留 decision-trigger 配置名兼容既有环境） */
    private DecisionTrigger decisionTrigger = new DecisionTrigger();

    /** Kafka 配置 */
    public static class Kafka {
        private String bootstrapServers = "localhost:9092";
        private String groupId = "datamaster-ingestion";
        private List<String> topics = new ArrayList<>();
        /** 单次 poll 超时（ms） */
        private long pollTimeoutMs = 1000L;
        /** 消费轮询间隔（ms） */
        private long pollIntervalMs = 1000L;

        public String getBootstrapServers() { return bootstrapServers; }
        public void setBootstrapServers(String v) { this.bootstrapServers = v; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String v) { this.groupId = v; }
        public List<String> getTopics() { return topics; }
        public void setTopics(List<String> v) { this.topics = v; }
        public long getPollTimeoutMs() { return pollTimeoutMs; }
        public void setPollTimeoutMs(long v) { this.pollTimeoutMs = v; }
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long v) { this.pollIntervalMs = v; }
    }

    /** 本体动作数据到达触发配置 */
    public static class DecisionTrigger {
        /** 是否回调本体决策数据到达触发 */
        private boolean enabled = false;
        /** 本体决策触发接口根地址（如 http://server:port） */
        private String baseUrl;
        /** 本体动作触发接口路径（数据到达自动触发；替代原 /ont/decision/trigger/data-arrival） */
        private String path = "/ont/action/trigger/data-arrival";
        /** 触发时传递的 triggerRef（缺省取 Kafka topic 名） */
        private String triggerRef;
        /** 触发秘钥（可空） */
        private String secret;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getPath() { return path; }
        public void setPath(String v) { this.path = v; }
        public String getTriggerRef() { return triggerRef; }
        public void setTriggerRef(String v) { this.triggerRef = v; }
        public String getSecret() { return secret; }
        public void setSecret(String v) { this.secret = v; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public Kafka getKafka() { return kafka; }
    public void setKafka(Kafka v) { this.kafka = v; }
    public Long getDorisDatasourceId() { return dorisDatasourceId; }
    public void setDorisDatasourceId(Long v) { this.dorisDatasourceId = v; }
    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String v) { this.conflictStrategy = v; }
    public List<String> getKeyColumns() { return keyColumns; }
    public void setKeyColumns(List<String> v) { this.keyColumns = v; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String v) { this.targetTable = v; }
    public DecisionTrigger getDecisionTrigger() { return decisionTrigger; }
    public void setDecisionTrigger(DecisionTrigger v) { this.decisionTrigger = v; }
}
