package com.datamaster.module.assets.api.governance.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AssetsTableGovernanceReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long datasourceId;

    private String tableName;

    private Long projectId;

    private String projectCode;

    private String entrance;
}
