

package com.datamaster.module.taxonomy.api.sourceSystem.dto;

import lombok.Data;

/**
 * 来源系统 DTO 对象 TAX_SOURCE_SYSTEM
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Data
public class TaxonomySourceSystemRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 系统名称 */
    private String name;

    /** 系统类型 */
    private String type;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 负责人 */
    private String responsiblePerson;

    /** 对接人 */
    private String contactPerson;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
