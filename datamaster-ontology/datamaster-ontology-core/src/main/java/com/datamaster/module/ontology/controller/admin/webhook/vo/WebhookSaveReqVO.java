package com.datamaster.module.ontology.controller.admin.webhook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "Webhook 保存 Request VO")
@Data
public class WebhookSaveReqVO {

    @Schema(description = "ID（新增为空，编辑必填）", example = "1")
    private Long id;

    @Schema(description = "Webhook 名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户源系统回调")
    @NotEmpty(message = "Webhook 名称不能为空")
    private String name;

    @Schema(description = "所属本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "绑定的动作ID（可选，绑定单个动作执行；为空则绑定本体级）", example = "1")
    private Long actionId;

    @Schema(description = "回调地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://source-system/api/callback")
    @NotEmpty(message = "回调地址不能为空")
    private String url;

    @Schema(description = "请求方法：POST/PUT，默认 POST", example = "POST")
    private String method;

    @Schema(description = "请求头（JSON，可选）", example = "{\"Authorization\":\"Bearer xxx\"}")
    private String headers;

    @Schema(description = "请求体模板（JSON 字符串，可选，空时按执行记录生成默认结构）")
    private String payloadTemplate;

    @Schema(description = "回调密钥（可选）")
    private String secret;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "失败最大重试次数（0 表示不重试）", example = "2")
    private Integer maxRetry;

    @Schema(description = "描述")
    private String description;
}