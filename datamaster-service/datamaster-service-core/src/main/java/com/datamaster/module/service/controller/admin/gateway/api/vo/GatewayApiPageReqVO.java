package com.datamaster.module.service.controller.admin.gateway.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.Map;

/**
 * 数据服务-API网关 Request VO  SVC_API_GATEWAY
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "API网关 Request VO")
@Data
public class GatewayApiPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Schema(description = "API", example = "")
    private String url;

    @Schema(description = "请求方式", example = "")
    private String httpMethod;

    @Schema(description = "开发者", example = "")
    private String developerName;

    @Schema(description = "应用名称", example = "")
    private String appName;

    Map<String, Object> queryParams;
}
