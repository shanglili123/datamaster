package com.datamaster.module.taxonomy.dal.dataobject.project;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 项目与用户关联关系 DO 对象 TAX_PROJECT_USER_REL
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
@TableName(value = "TAX_PROJECT_USER_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_PROJECT_USER_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomyProjectUserRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 项目空间ID */
    private Long projectId;

    /** 用户ID */
    private Long userId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

    /** 用户名称 */
    @TableField(exist = false)
    private String userName;

    /** 手机号码 */
    @TableField(exist = false)
    private String phoneNumber;

    /** 部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 用户昵称 */
    @TableField(exist = false)
    private String nickName;

    /** 用户状态 */
    @TableField(exist = false)
    private String status;

    /** 用户角色多个一逗号拼接 */
    @TableField(exist = false)
    private String roleStr;
}
