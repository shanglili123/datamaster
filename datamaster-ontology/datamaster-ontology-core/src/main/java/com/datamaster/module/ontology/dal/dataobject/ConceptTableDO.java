package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 概念表绑定 DO
 *
 * 对应表 ONT_CONCEPT_TABLE
 */
@Data
@TableName("ONT_CONCEPT_TABLE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConceptTableDO extends BaseEntity {

    /** Maintained by database trigger, including identity changes followed by restoration. */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long sourceAuthRevision;

    /** 所属概念ID */
    private Long conceptId;

    /** 数据源ID */
    private Long datasourceId;

    /** 数据库名 */
    private String databaseName;

    /** 表名 */
    private String tableName;

    /** Schema名 */
    private String schemaName;

    @TableLogic
    private Integer delFlag;
}
