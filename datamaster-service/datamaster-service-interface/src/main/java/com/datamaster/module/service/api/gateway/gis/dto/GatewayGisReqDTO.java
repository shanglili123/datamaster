package com.datamaster.module.service.api.gateway.gis.dto;

import lombok.Data;

/**
 * 数据服务-GIS网关 DTO 对象 SVC_GIS_GATEWAY
 *
 * @author DATAMASTER
 */
@Data
public class GatewayGisReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

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
    private Boolean delFlag;


}
