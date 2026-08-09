package com.datamaster.common.api.space.dto;

import lombok.Data;

@Data
public class SpaceReqDTO {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String code;
    private Long assignedDatasourceId;
    private Long managerId;
    private String description;
    private Boolean validFlag;
    private Boolean delFlag;
}
