package com.datamaster.module.governance.api.warehouse.dto;

import lombok.Data;

/** Unified warehouse naming input. Governance IDs, not client supplied labels, are authoritative. */
@Data
public class WarehouseTableNamingReqDTO {
    private String baseTableName;
    /** 1 detail, 2 aggregate, 3 dimension, 4 application. */
    private String tableType;
    private Long dataLayerId;
    private Long businessCategoryId;
    private Long dataDomainId;
    private Long themeDomainId;
    /** 1 uppercase, 2 lowercase (default). */
    private String tableCase;
    private String datasourceType;
}
