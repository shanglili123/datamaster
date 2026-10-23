package com.datamaster.ingestion.controller.vo;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/** 数据接入消费者真实运行状态，而不是仅返回配置开关。 */
@Data
public class IngestionRuntimeStatusRespVO {

    /** DISABLED / NOT_STARTED / STARTING / RUNNING / PAUSING / PAUSED / RESUMING / NO_TOPICS / CONFIG_ERROR / ERROR / STOPPED */
    private String state;
    private boolean configuredEnabled;
    private boolean active;
    /** Kafka 消费分区是否已经由轮询线程实际暂停。 */
    private boolean paused;
    private String message;
    private Date startedAt;
    private Date lastPollAt;
    private Date lastSuccessAt;
    private Date lastFailureAt;
    private long processedCount;
    private long failedCount;
    /** 最近一次从 Kafka endOffsets 计算出的总积压。 */
    private long totalLag;
    private Date lagMeasuredAt;
    /** key 格式为 topic-partition，例如 customer_stream-2。 */
    private Map<String, Long> lagByPartition;
}
