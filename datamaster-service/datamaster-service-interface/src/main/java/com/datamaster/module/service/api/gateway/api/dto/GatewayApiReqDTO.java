package com.datamaster.module.service.api.gateway.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据服务-API网关 DTO 对象 SVC_API_GATEWAY
 *
 * @author DATAMASTER
 */
@Data
public class GatewayApiReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 资产id */
    private Long assetId;

    /** API路径 */
    private String url;

    @Schema(description = "开发者", example = "")
    private String developerName;

    @Schema(description = "应用名称", example = "")
    private String appName;

    /** 请求方式 */
    private String httpMethod;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
