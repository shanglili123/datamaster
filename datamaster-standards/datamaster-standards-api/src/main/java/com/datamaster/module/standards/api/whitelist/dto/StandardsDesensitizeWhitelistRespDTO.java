

package com.datamaster.module.standards.api.whitelist.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

/**
 * 脱敏白名单 DTO 对象 STD_DESENSITIZE_WHITELIST
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Data
public class StandardsDesensitizeWhitelistRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 白名单名称 */
    private String name;

    /** 数据分类 */
    private Long dataCategoryId;

    /** 生效分类;1：用户 2：角色 3：部门 */
    private String effectiveCategory;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
