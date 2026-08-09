package com.datamaster.common.datasource.mgmt.api.dto;

import lombok.Data;

@Data
public class DatasourceRespDTO {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String datasourceName;

    private String datasourceType;

    private String datasourceConfig;

    private String ip;

    private Long port;

    private Long listCount;

    private Long syncCount;

    private Long dataSize;

    private String description;

    private Boolean validFlag;

    private Long dsDatasourceId;

}
