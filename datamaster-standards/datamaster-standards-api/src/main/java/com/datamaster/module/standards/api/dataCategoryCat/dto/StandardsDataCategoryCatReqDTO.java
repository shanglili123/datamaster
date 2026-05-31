

package com.datamaster.module.standards.api.dataCategoryCat.dto;

import lombok.*;

/**
 * 数据分类-类目 DTO 对象 STD_DATA_CATEGORY_CAT
 *
 * @author FXB
 * @date 2026-04-07
 */
@Data
public class StandardsDataCategoryCatReqDTO {

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

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
