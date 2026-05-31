

package com.datamaster.module.standards.api.desensitizeRules.dto;

import lombok.*;

/**
 * 脱敏区间 DTO 对象 STD_DESENSITIZE_INTERVAL
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Data
public class StandardsDesensitizeIntervalReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 脱敏规则ID */
    private Long desensitizeRuleId;

    /** 区间号 */
    private Long intervalNo;

    /** 起始值 */
    private Long startNum;

    /** 末尾值 */
    private Long endNum;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
