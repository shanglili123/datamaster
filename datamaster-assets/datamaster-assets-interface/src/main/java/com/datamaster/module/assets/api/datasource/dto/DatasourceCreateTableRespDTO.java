package com.datamaster.module.assets.api.datasource.dto;

import lombok.Data;

/** 建表和平台登记是两个独立阶段，部分成功不得当作可回滚失败。 */
@Data
public class DatasourceCreateTableRespDTO {
    private Long assetId;
    private Long datasourceId;
    private String tableName;
    private boolean tableCreated;
    private boolean assetRegistered;
    private String message;
}
