package com.datamaster.module.service.dal.dataobject.gateway.api;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据服务-API网关 DO  SVC_API_GATEWAY
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "SVC_API_GATEWAY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GatewayApiDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 资产id */
    private Long assetId;

    /** API */
    private String url;

    @Schema(description = "开发者", example = "")
    private String developerName;

    @Schema(description = "应用名称", example = "")
    private String appName;

    /**  */
    private String httpMethod;

    /** API 描述 */
    private String description;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
