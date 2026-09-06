package com.datamaster.module.ontology.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 对象实例层 API DTO — 供 AI 问数、管理端对象浏览器等上层模块复用
 *
 * 对象（Object）= 绑定到物理表的概念（偏 Palantir Object Type 思路）；
 * 对象集（Object Set）= 一个概念 + 它命中的物理表（一个概念可绑定多张表）。
 * 仅承载只读语义信息，不含任何权限或 SQL 执行逻辑。
 */
@Data
public class ObjectInstanceApiDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 概念ID（ONT_CONCEPT.id） */
    private Long conceptId;

    /** 概念业务名称（ONT_CONCEPT.name） */
    private String conceptName;

    /** 概念编码（ONT_CONCEPT.code） */
    private String conceptCode;

    /** 概念描述（ONT_CONCEPT.description） */
    private String conceptDescription;

    /** 所属本体ID（ONT_ONTOLOGY.id） */
    private Long ontologyId;

    /** 物理表绑定ID（ONT_CONCEPT_TABLE.id） */
    private Long tableBindingId;

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
