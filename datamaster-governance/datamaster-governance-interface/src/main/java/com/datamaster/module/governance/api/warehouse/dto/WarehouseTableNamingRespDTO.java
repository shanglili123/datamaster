package com.datamaster.module.governance.api.warehouse.dto;

import lombok.Data;

/** Resolved governance and deterministic physical target; business category is metadata only. */
@Data
public class WarehouseTableNamingRespDTO {
    private String tableName;
    private String tableType;
    private Long dataLayerId;
    private String dataLayerCode;
    private String dataLayerName;
    private String dataLayerEngName;
    private Long businessCategoryId;
    private String businessCategoryCode;
    private String businessCategoryName;
    private String businessCategoryEngName;
    private Long dataDomainId;
    private String dataDomainCode;
    private String dataDomainName;
    private String dataDomainEngName;
    private Long themeDomainId;
    private String themeDomainCode;
    private String themeDomainName;
    private String themeDomainEngName;
    private String tableCase;
}
