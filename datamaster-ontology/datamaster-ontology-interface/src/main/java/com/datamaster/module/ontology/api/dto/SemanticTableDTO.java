package com.datamaster.module.ontology.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 本体概念表语义 DTO — AI 问数/Skill 生成专用
 *
 * 一个概念表绑定（ONT_CONCEPT_TABLE）对应一条记录，
 * 包含概念元信息和该概念在该物理表上的属性语义映射。
 */
@Data
public class SemanticTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 概念ID（ONT_CONCEPT.id） */
    private Long conceptId;

    /** 概念业务名称（ONT_CONCEPT.name） */
    private String conceptName;

    /** 概念编码（ONT_CONCEPT.code） */
    private String conceptCode;

    /** 概念描述（ONT_CONCEPT.description） */
    private String conceptDescription;

    /** 数据源ID（ONT_CONCEPT_TABLE.datasource_id） */
    private Long datasourceId;

    /** 物理表名（ONT_CONCEPT_TABLE.table_name） */
    private String tableName;

    /** 数据库名（ONT_CONCEPT_TABLE.database_name） */
    private String databaseName;

    /** Schema名（ONT_CONCEPT_TABLE.schema_name） */
    private String schemaName;

    /** 该概念在此物理表上的属性语义列表（含物理列映射） */
    private List<SemanticPropertyDTO> properties;
}
