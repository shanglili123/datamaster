

package com.datamaster.module.taxonomy.api.Rel.dto;

import lombok.Data;

/**
 * 标签与资产关联关系 DTO 对象 TAX_TAG_ASSET_REL
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Data
public class TaxonomyTagAssetRelRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long ID;

    /** 标签管理id */
    private String tagId;

    /** 资产id */
    private String assetId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
