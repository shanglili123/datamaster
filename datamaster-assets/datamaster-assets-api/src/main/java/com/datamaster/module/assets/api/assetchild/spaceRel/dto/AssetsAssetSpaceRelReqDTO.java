


package com.datamaster.module.assets.api.assetchild.spaceRel.dto;

import lombok.Data;

/**
 * 数据资产与空间关联关系 DTO 对象 DA_ASSET_SPACE_REL
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Data
public class AssetsAssetSpaceRelReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 资产id */
    private Long assetId;

    /** 空间id */
    private Long spaceId;

    /** 空间编码 */
    private String spaceCode;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
