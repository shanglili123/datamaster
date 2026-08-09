package com.datamaster.module.service.api.gateway.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据服务-API网关-参数 DTO 对象 SVC_API_GATEWAY_PARAM
 *
 * @author DATAMASTER
 */
@Data
public class GatewayApiParamReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** API id */
    private Long apiId;

    /** 父级id */
    private Long parentId;

    /** 参数名称 */
    private String name;

    @Schema(description = "数据默认值", example = "")
    private String defaultValue;
    @Schema(description = "示例值", example = "")
    private String exampleValue;
    @Schema(description = "描述", example = "")
    private String description;

    /** 参数类型 */
    private String type;

    /** 是否必填 */
    private String requestFlag;

    /** 字段类型 */
    private String columnType;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
