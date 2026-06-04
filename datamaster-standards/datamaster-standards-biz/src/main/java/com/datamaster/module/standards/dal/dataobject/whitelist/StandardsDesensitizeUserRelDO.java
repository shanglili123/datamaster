package com.datamaster.module.standards.dal.dataobject.whitelist;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 脱敏白名单与用户关联关系 DO 对象 STD_DESENSITIZE_USER_REL
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Data
@TableName(value = "STD_DESENSITIZE_USER_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DESENSITIZE_USER_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDesensitizeUserRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 脱敏白名单ID */
    private Long desensitizeId;

    /** 用户ID */
    private Long userId;

    /** 白名单名称 */
    private String desensitizeName;

    /** 用户名称 */
    private String userName;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;

    /** 生效分类;1：用户 2：角色 3：部门 */
    private String effectiveCategory;


}
