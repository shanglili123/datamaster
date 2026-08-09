package com.datamaster.metadata.api.table.dto;

import lombok.Data;

import java.util.Date;

/**
 * 元数据信息 DTO 对象 CAT_TABLE。
 */
@Data
public class CatalogTableRespDTO {

    private Long id;

    private Long dbId;

    private Long datasourceId;

    private Integer version;

    private String tableName;

    private String tableComment;

    private String dbName;

    private String schemaName;

    private String storageType;

    private Integer storageSize;

    private Integer dataQuality;

    private String status;

    private String description;

    private Long columnCount;

    private Long rowCount;

    private String partitionKey;

    private String storageEngine;

    private String primaryKey;

    private Date tbCreateTime;

    private Date dataUpdateTime;
}
