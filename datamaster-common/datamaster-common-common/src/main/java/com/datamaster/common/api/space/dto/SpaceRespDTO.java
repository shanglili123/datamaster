package com.datamaster.common.api.space.dto;

import lombok.Data;

@Data
public class SpaceRespDTO {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Long managerId;
    private String description;
    private Boolean validFlag;
    private Boolean delFlag;
    private Boolean dppAssigned;
    private String code;
    private Integer workerGroupId;
    private String workerGroup;
    private String nickName;
    private String managerPhone;
}
