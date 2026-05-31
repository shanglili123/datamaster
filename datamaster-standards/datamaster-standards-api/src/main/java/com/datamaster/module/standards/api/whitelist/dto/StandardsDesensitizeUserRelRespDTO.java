

package com.datamaster.module.standards.api.whitelist.dto;

import lombok.*;

/**
 * 脱敏白名单与用户关联关系 DTO 对象 STD_DESENSITIZE_USER_REL
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Data
public class StandardsDesensitizeUserRelRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 脱敏白名单ID */
    private Long desensitizeId;

    /** 用户ID */
    private Long userId;

    /** 白名单名称 */
    private String desensitizeName;

    /** 用户名称 */
    private String userName;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;

    /** 生效分类;1：用户 2：角色 3：部门 */
    private String effectiveCategory;


}
