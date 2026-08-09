package com.datamaster.common.api.client.dto;

import lombok.Data;

@Data
public class ClientRespDTO {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String type;
    private String secret;
    private String homepageUrl;
    private String allowUrl;
    private String syncUrl;
    private String logo;
    private String description;
    private String publicFlag;
    private Boolean validFlag;
    private Boolean delFlag;
}
