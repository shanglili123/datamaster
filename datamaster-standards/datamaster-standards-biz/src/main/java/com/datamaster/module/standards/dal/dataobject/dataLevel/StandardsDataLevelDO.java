package com.datamaster.module.standards.dal.dataobject.dataLevel;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据分级 DO 对象 STD_DATA_LEVEL
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Data
@TableName(value = "STD_DATA_LEVEL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DATA_LEVEL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDataLevelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 分级名称 */
    private String name;

    /** 分级缩写名 */
    private String shortName;

    /** 敏感等级 */
    private Long sensitiveLevel;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 颜色值 */
    private String colors;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
