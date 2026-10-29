package com.datamaster.module.system.api.message.dto;

import lombok.Data;

@Data
public class SystemNoticeReqDTO {
    private Long receiverId;
    private String title;
    private String content;
    private Long entityId;
    private String entityUrl;
}
