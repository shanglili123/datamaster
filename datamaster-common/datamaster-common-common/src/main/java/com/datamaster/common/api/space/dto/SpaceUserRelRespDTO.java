package com.datamaster.common.api.space.dto;

import lombok.Data;

@Data
public class SpaceUserRelRespDTO {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long spaceId;
    private Long userId;
    private Boolean validFlag;
    private Boolean delFlag;
}
