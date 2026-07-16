package com.datamaster.module.assets.api.governance.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AssetsTableGovernanceRespDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SOURCE_ASSET = "ASSET";
    public static final String SOURCE_CATALOG = "CATALOG";
    public static final String SOURCE_NONE = "NONE";

    private Boolean enabled;

    private String mode;

    private String source;

    private Long assetId;

    private Long catalogTableId;

    private Long datasourceId;

    private String tableName;

    private Boolean accessAllowed;

    private List<String> deniedColumns;

    private String message;
}
