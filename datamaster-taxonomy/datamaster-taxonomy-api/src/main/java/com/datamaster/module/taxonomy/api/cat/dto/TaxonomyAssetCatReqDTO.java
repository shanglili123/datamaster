

package com.datamaster.module.taxonomy.api.cat.dto;

import lombok.Data;

/**
 * 数据资产类目管理 DTO 对象 TAX_ASSET_CAT
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Data
public class TaxonomyAssetCatReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 类别名称 */
    private String name;

    /** 关联上级ID */
    private Long parentId;

    /** 类别排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 层级编码 */
    private String code;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
