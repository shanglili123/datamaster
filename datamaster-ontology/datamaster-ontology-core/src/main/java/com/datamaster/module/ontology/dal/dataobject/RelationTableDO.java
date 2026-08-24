package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 关系关联表绑定 DO
 *
 * 对应表 ONT_RELATION_TABLE
 */
@Data
@TableName("ONT_RELATION_TABLE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RelationTableDO extends BaseEntity {

    /** 所属关系ID */
    private Long relationId;

    /** 数据源ID */
    private Long datasourceId;

    /** 数据库名 */
    private String databaseName;

    /** 表名 */
    private String tableName;

    /** Schema名 */
    private String schemaName;

    /** 已选物理字段名, JSON数组 */
    private String columnNames;

    @TableLogic
    private Integer delFlag;
}
