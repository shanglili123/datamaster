package com.datamaster.common.database.core;

import lombok.Data;

/**
 * 表元数据信息（采集用）
 * 用于元数据采集时获取表行数、索引、分区、存储等结构信息。
 * 字段语义与元数据采集需求保持一致。
 *
 * @author DATAMASTER
 */
@Data
public class DbTableMetadata {

    /**
     * 表行数
     */
    private Long rowCount;

    /**
     * 表索引信息（逗号分隔，不含主键索引）
     */
    private String indexes;

    /**
     * 表分区字段信息（逗号分隔）
     */
    private String partitionFields;

    /**
     * 表存储大小
     */
    private Integer tableSize;

    /**
     * 存储引擎
     */
    private String storageEngine;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 主键字段（逗号分隔）
     */
    private String primaryKey;

    /**
     * 创建时间（yyyy-MM-dd HH:mm:ss）
     */
    private String createTime;

    /**
     * 修改时间（yyyy-MM-dd HH:mm:ss）
     */
    private String updateTime;
}
