package com.datamaster.module.standards.dal.dataobject.dataElem;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据元数据资产关联信息 DO 对象 STD_DATA_ELEM_ASSET_REL
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
@TableName(value = "STD_DATA_ELEM_ASSET_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DATA_ELEM_ASSET_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDataElemAssetRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 数据元类型 */
    private String dataElemType;

    /** 数据元id */
    private String dataElemId;

    /** 资产id(数据表id) */
    private String assetId;

    /** 资产名称 */
    @TableField(exist = false)
    private String assetName;

    /** 表描述 */
    @TableField(exist = false)
    private String tableComment;

    /** 资产描述 */
    @TableField(exist = false)
    private String description;

    /** 数据表 */
    private String tableName;

    /** 关联字段id */
    private String columnId;

    /** 关联字段 */
    private String columnName;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
