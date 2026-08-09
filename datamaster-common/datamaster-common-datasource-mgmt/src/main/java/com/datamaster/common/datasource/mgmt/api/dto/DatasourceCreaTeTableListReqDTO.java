package com.datamaster.common.datasource.mgmt.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class DatasourceCreaTeTableListReqDTO {

    private String datasourceType;

    private String datasourceConfig;

    private String ip;

    private Long port;

    private List<DatasourceCreaTeTableReqDTO> dtoList;
}
