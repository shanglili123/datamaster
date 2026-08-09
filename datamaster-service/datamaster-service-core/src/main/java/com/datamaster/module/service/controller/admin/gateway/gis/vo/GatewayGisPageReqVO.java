package com.datamaster.module.service.controller.admin.gateway.gis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据服务-GIS网关 Request VO  SVC_GIS_GATEWAY
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "GIS网关 Request VO")
@Data
public class GatewayGisPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Schema(description = "服务地址", example = "")
    private String url;

    @Schema(description = "服务类型", example = "")
    private String type;

    @Schema(description = "请求方式", example = "")
    private String httpMethod;

    @Schema(description = "坐标系", example = "")
    private String coordinateSystem;

}
