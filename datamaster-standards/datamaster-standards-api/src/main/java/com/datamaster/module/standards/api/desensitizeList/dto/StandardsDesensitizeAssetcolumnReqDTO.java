

package com.datamaster.module.standards.api.desensitizeList.dto;

import lombok.*;

/**
 * 脱敏清单关联关系 DTO 对象 STD_DESENSITIZE_ASSETCOLUMN
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Data
public class StandardsDesensitizeAssetcolumnReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 资产ID */
    private Long assetId;

    /** 资产字段ID */
    private Long assetcolumnId;

    /** 数据分类ID */
    private Long dataCategoryId;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
