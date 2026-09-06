package com.datamaster.module.ontology.controller.admin.webhook.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "Webhook 分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "绑定的动作ID", example = "1")
    private Long actionId;

    @Schema(description = "名称（模糊）", example = "客户")
    private String name;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}