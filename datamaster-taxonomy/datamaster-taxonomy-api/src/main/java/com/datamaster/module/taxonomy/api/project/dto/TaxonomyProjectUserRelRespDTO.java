

package com.datamaster.module.taxonomy.api.project.dto;

import lombok.Data;

/**
 * 项目与用户关联关系 DTO 对象 TAX_PROJECT_USER_REL
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
public class TaxonomyProjectUserRelRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 项目空间ID */
    private Long projectId;

    /** 用户ID */
    private Long userId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
