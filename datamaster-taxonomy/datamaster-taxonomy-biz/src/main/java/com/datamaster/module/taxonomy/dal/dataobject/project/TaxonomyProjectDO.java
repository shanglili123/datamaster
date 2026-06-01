package com.datamaster.module.taxonomy.dal.dataobject.project;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 项目 DO 对象 TAX_PROJECT
 *
 * @author shu
 * @date 2025-01-20
 */
@Data
@TableName(value = "TAX_PROJECT")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_PROJECT_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomyProjectDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 项目名称 */
    private String name;

    /** 项目编码 */
    private String code;

    /** DS 专属工作组 ID */
    private Integer workerGroupId;

    /** DS 专属工作组 */
    private String workerGroup;

    /** 项目管理员id */
    private Long managerId;
    /** 项目管理员 */
    @TableField(exist = false)
    private String nickName;
    /** 项目管理员手机号 */
    @TableField(exist = false)
    private String managerPhone;

    /** 项目描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
