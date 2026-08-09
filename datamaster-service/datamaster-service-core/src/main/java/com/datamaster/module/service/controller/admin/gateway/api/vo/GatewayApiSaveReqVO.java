package com.datamaster.module.service.controller.admin.gateway.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 数据服务-API网关 Save Request VO  SVC_API_GATEWAY
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "API网关 Save Request VO")
@Data
public class GatewayApiSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Schema(description = "API", example = "")
    @Size(max = 256, message = "API不能超过256个字符")
    private String url;

    @Schema(description = "请求方式", example = "")
    @Size(max = 256, message = "请求方式不能超过256个字符")
    private String httpMethod;

    @Schema(description = "开发者", example = "")
    private String developerName;

    @Schema(description = "应用名称", example = "")
    private String appName;

    @Schema(description = "API描述", example = "")
    @Size(max = 256, message = "描述不能超过256个字符")
    private String description;

}
