

package com.datamaster.module.taxonomy.api.space.dto;

import lombok.Data;

/**
 * 空间请求 DTO 对象 TAX_SPACE
 *
 * @author shu
 * @date 2025-01-20
 */
@Data
public class TaxonomySpaceReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 空间名称 */
    private String name;

    /** 空间编码 */
    private String code;

    /** 用于判断空间是否已绑定的数据源 ID */
    private Long assignedDatasourceId;

    /** 空间管理员 ID */
    private Long managerId;

    /** 空间描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
