package com.datamaster.module.service.controller.admin.gateway.gis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 数据服务-GIS网关 Save Request VO  SVC_GIS_GATEWAY
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "GIS网关 Save Request VO")
@Data
public class GatewayGisSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "资产id", example = "")
    private Long assetId;

    @Schema(description = "服务地址", example = "")
    @Size(max = 256, message = "服务地址不能超过256个字符")
    private String url;

    @Schema(description = "服务类型", example = "")
    @Size(max = 256, message = "服务类型不能超过256个字符")
    private String type;

    @Schema(description = "请求方式", example = "")
    @Size(max = 256, message = "请求方式不能超过256个字符")
    private String httpMethod;

    @Schema(description = "坐标系", example = "")
    @Size(max = 256, message = "坐标系不能超过256个字符")
    private String coordinateSystem;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述不能超过256个字符")
    private String description;

}
