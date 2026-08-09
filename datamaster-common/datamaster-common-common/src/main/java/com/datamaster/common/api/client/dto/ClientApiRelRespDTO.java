package com.datamaster.common.api.client.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ClientApiRelRespDTO {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long clientId;
    private Long apiId;
    private String pvFlag;
    private Date startTime;
    private Date endTime;
    private String status;
    private Boolean validFlag;
    private Boolean delFlag;
}
