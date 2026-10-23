package com.datamaster.ingestion.dal.dataobject;

import lombok.Data;

import java.util.Date;

/** Kafka 事件幂等领取记录。eventKey = topic:partition:offset。 */
@Data
public class IngestionEventDO {

    private String eventKey;
    private String topic;
    private Integer partition;
    private Long msgOffset;
    private String status;
    private String owner;
    private Date leaseUntil;
    private Integer attemptCount;
}
