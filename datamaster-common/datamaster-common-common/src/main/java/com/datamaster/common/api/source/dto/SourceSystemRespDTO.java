package com.datamaster.common.api.source.dto;

import lombok.Data;

@Data
public class SourceSystemRespDTO {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String type;
    private Long sortOrder;
    private String description;
    private Boolean validFlag;
    private String responsiblePerson;
    private String contactPerson;
    private Boolean delFlag;
}
