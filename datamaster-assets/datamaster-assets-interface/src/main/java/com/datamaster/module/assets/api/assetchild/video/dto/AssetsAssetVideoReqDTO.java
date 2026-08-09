

package com.datamaster.module.assets.api.assetchild.video.dto;

import lombok.Data;

/**
 * 数据资产-视频数据 DTO 对象 DA_ASSET_VIDEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
public class AssetsAssetVideoReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 资产id */
    private Long assetId;

    /** IP */
    private String ip;

    /** 端口号 */
    private Long port;

    /** 协议 */
    private String protocol;

    /** 平台 */
    private String platform;

    /** 配置JSON */
    private String config;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
