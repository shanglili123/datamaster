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

    /**
     * 正向可查字段清单；为空/未配置字段级权限时表示全部字段可查。
     * 命中资产时返回空间已授权字段，无资产降级到元数据时为空（全量放行）。
     */
    private List<String> allowedColumns;

    private String message;
}
