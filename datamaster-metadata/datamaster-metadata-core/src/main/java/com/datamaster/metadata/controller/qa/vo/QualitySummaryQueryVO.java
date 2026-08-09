package com.datamaster.metadata.controller.qa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 表质量结果摘要查询 VO
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
@Schema(description = "表质量结果摘要查询 VO")
@Data
public class QualitySummaryQueryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "表名")
    private String tableName;
}
