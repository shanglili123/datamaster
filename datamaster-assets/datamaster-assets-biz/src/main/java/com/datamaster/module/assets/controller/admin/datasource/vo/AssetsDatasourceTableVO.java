package com.datamaster.module.assets.controller.admin.datasource.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AssetsDatasourceTableVO {

    /**
     * datasourceId
     */
    @NotNull(message = "id null")
    private Long id;

    private String tableName;

    private Integer withRule;

}
