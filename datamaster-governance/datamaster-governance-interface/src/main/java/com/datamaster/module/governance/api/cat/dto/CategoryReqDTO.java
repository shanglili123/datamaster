package com.datamaster.module.governance.api.cat.dto;

import lombok.Data;

@Data
public class CategoryReqDTO {

    private static final long serialVersionUID = 1L;

    private String catType;

    private String name;

    private String code;

    private Boolean validFlag;

    private Long spaceId;

    private String spaceCode;
}
