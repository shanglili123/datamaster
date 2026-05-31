

package com.datamaster.module.modeling.api.businessCategory.dto;

import lombok.Data;

/**
 * 业务分类 DTO 对象 Modeling_BUSINESS_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Data
public class ModelingBusinessCategoryReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 层级编码 */
    private String code;

    /** 业务分类名称 */
    private String name;

    /** 关联上级ID */
    private Long parentId;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 英文缩写名 */
    private String engName;

    /** 负责人手机号 */
    private String ownerPhone;

    /** 负责人ID */
    private Long ownerId;

    /** 数据域ID */
    private Long domainId;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
