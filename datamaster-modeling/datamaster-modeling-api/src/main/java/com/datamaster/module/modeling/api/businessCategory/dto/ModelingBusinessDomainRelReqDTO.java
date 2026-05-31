

package com.datamaster.module.modeling.api.businessCategory.dto;

import lombok.Data;

/**
 * 业务分类数据域关联关系 DTO 对象 Modeling_BUSINESS_DOMAIN_REL
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Data
public class ModelingBusinessDomainRelReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 业务分类ID */
    private Long businessCategoryId;

    /** 数据域ID */
    private Long dataDomainId;

    /** 业务分类名称 */
    private String businessCategoryName;

    /** 数据域名称 */
    private String dataDomainName;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
