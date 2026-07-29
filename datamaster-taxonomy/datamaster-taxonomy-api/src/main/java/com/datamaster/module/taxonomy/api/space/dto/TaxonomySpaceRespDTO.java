

package com.datamaster.module.taxonomy.api.space.dto;

import lombok.Data;

/**
 * 空间响应 DTO 对象 TAX_SPACE
 *
 * @author shu
 * @date 2025-01-20
 */
@Data
public class TaxonomySpaceRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 空间名称 */
    private String name;

    /** 空间管理员 ID */
    private Long managerId;

    /** 空间描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;

    /** 是否分配到数据研发 */
    private Boolean dppAssigned;

    /** 空间编码 */
    private String code;

    /** DS 专属工作组 ID */
    private Integer workerGroupId;

    /** DS 专属工作组 */
    private String workerGroup;

    /** 空间管理员昵称 */
    private String nickName;

    /** 空间管理员手机号 */
    private String managerPhone;


}
