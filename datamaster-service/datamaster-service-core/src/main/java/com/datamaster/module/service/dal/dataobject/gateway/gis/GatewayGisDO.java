package com.datamaster.module.service.dal.dataobject.gateway.gis;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据服务-GIS网关 DO  SVC_GIS_GATEWAY
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "SVC_GIS_GATEWAY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GatewayGisDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 资产id */
    private Long assetId;

    /** 服务地址 */
    private String url;

    /** 服务类型 */
    private String type;

    /** 请求方式 */
    private String httpMethod;

    /** 坐标系 */
    private String coordinateSystem;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

}
