

package com.datamaster.module.taxonomy.api.cat.dto;

import lombok.Data;

/**
 * 数据质量类目 DTO 对象 TAX_QUALITY_CAT
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Data
public class TaxonomyQualityCatRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 类别名称 */
    private String name;

    /** 关联上级ID */
    private Long parentId;

    /** 类别排序 */
    private Long sortOrder;

    /** 层级编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
