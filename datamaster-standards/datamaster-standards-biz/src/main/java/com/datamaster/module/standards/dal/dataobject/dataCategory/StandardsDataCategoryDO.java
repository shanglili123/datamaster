package com.datamaster.module.standards.dal.dataobject.dataCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据分类 DO 对象 STD_DATA_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Data
@TableName(value = "STD_DATA_CATEGORY")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DATA_CATEGORY_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDataCategoryDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**
     * 类目id
     */
    private Long catId;

    /**
     * 类目编码
     */
    private String catCode;

    /**
     * 分类名称
     */
    private String name;

    /** 分类名称缩写名 */
    private String shortName;

    /**
     * 数据分级
     */
    private Long dataLevelId;

    /**
     * 任务优先级;HIGHEST,HIGH,MEDIUM,LOW,LOWEST
     */
    private String priority;

    /**
     * 描述
     */
    private String description;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /**
     * 是否有效;0：无效，1：有效
     */
    private Boolean validFlag;

    /**
     * 删除标志;1：已删除，0：未删除
     */
    @TableLogic
    private Boolean delFlag;

    /**
     * 类目名称
     */
    @TableField(exist = false)
    private String catName;

    /**
     * 数据分级缩写
     */
    @TableField(exist = false)
    private String dataLevelShortName;

    /**
     * 脱敏配置（0:否 1:是）
     */
    @TableField(exist = false)
    private String desensitizationRulesFlag;

    @TableField(exist = false)
    @Schema(description = "脱敏规则id")
    private Long desensitizationRulesId;

}
