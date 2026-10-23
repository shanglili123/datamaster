package com.datamaster.ingestion.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 批量重放数据接入死信请求。 */
@Data
@Schema(description = "批量重放数据接入死信请求")
public class IngestionDeadLetterBatchReplayReq {

    @Schema(description = "死信日志 ID 列表，最多 100 条", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> logIds;
}
