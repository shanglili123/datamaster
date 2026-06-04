package com.datamaster.module.modeling.dal.dataobject.dm;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 主题域管理 DO 对象 MDL_THEME_DOMAIN
 *
 * @author FXB
 * @date 2026-03-24
 */
@Data
@TableName(value = "MDL_THEME_DOMAIN")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MDL_THEME_DOMAIN_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelingThemeDomainDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**
     * 层级编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 英文缩写
     */
    private String engName;

    /**
     * 关联上级ID
     */
    private Long parentId;

    /**
     * 负责人ID
     */
    private Long ownerUserId;

    /**
     * 数仓分层ID
     */
    private Long dataLayerId;

    /**
     * 描述
     */
    private String description;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目编码
     */
    private String projectCode;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    /**
     * 删除标志
     */
    @TableLogic
    private Boolean delFlag;


    /**
     * 负责人名称
     */
    @TableField(exist = false)
    private String ownerUserName;

    /**
     * 数仓分层名称
     */
    @TableField(exist = false)
    private String dataLayerName;

    /**
     * 负责人联系方式
     */
    @TableField(exist = false)
    private String ownerUserPhoneNumber;

    /**
     * 统计数量
     */
    @TableField(exist = false)
    private Long num;
}
