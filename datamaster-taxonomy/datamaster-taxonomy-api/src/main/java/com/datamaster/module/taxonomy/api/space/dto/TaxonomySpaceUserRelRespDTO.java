

package com.datamaster.module.taxonomy.api.space.dto;

import lombok.Data;

/**
 * 空间与用户关联关系响应 DTO 对象 TAX_SPACE_USER_REL
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
public class TaxonomySpaceUserRelRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 空间 ID */
    private Long spaceId;

    /** 用户 ID */
    private Long userId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
