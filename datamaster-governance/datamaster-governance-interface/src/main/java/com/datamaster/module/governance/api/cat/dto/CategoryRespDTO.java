package com.datamaster.module.governance.api.cat.dto;

import lombok.Data;

@Data
public class CategoryRespDTO {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String catType;

    private String name;

    private Long parentId;

    private Long sortOrder;

    private String description;

    private String code;

    private Boolean validFlag;

    private Boolean delFlag;
}
