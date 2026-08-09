package com.datamaster.module.service.controller.admin.gateway.api.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 数据服务-API网关-参数 Save Request VO  SVC_API_GATEWAY_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "API网关-参数 Save Request VO")
@Data
public class GatewayApiParamSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "API id", example = "")
    private Long apiId;

    @Schema(description = "父级id", example = "")
    private Long parentId;

    @Schema(description = "参数名称", example = "")
    @Size(max = 256, message = "参数名称不能超过256个字符")
    private String name;

    @Schema(description = "参数类型", example = "")
    @Size(max = 256, message = "参数类型不能超过256个字符")
    private String type;

    @Schema(description = "数据默认值", example = "")
    private String defaultValue;
    @Schema(description = "示例值", example = "")
    private String exampleValue;
    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否必填", example = "")
    @Size(max = 256, message = "是否必填不能超过256个字符")
    private String requestFlag;

    @Schema(description = "字段类型", example = "")
    @Size(max = 256, message = "字段类型不能超过256个字符")
    private String columnType;

    @TableField(exist = false)
    private List<GatewayApiParamSaveReqVO> gatewayApiParamList;

}
