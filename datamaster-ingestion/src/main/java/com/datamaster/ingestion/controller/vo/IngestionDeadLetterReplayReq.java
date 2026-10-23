package com.datamaster.ingestion.controller.vo;

import lombok.Data;

/** 死信重放请求。 */
@Data
public class IngestionDeadLetterReplayReq {
    private Long logId;
}
