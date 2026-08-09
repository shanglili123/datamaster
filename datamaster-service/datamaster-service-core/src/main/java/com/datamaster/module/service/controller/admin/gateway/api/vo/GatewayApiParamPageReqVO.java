package com.datamaster.module.service.controller.admin.gateway.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据服务-API网关-参数 Request VO  SVC_API_GATEWAY_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "API网关-参数 Request VO")
@Data
public class GatewayApiParamPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "API id", example = "")
    private Long apiId;

    @Schema(description = "父级id", example = "")
    private Long parentId;

    @Schema(description = "参数名称", example = "")
    private String name;

    @Schema(description = "参数类型", example = "")
    private String type;

    @Schema(description = "是否必填", example = "")
    private String requestFlag;

    @Schema(description = "字段类型", example = "")
    private String columnType;

    @Schema(description = "数据默认值", example = "")
    private String defaultValue;
    @Schema(description = "示例值", example = "")
    private String exampleValue;
    @Schema(description = "描述", example = "")
    private String description;

}
