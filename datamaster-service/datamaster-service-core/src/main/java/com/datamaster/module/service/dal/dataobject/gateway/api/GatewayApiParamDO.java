package com.datamaster.module.service.dal.dataobject.gateway.api;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据服务-API网关-参数 DO  SVC_API_GATEWAY_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "SVC_API_GATEWAY_PARAM")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GatewayApiParamDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** API id */
    private Long apiId;

    /** 父级id */
    private Long parentId;

    /** 参数名称 */
    private String name;

    /** 参数类型 */
    private String type;

    /** 是否必填 */
    private String requestFlag;

    /** 字段类型 */
    private String columnType;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

    @Schema(description = "数据默认值", example = "")
    private String defaultValue;
    @Schema(description = "示例值", example = "")
    private String exampleValue;
    @Schema(description = "描述", example = "")
    private String description;

}
