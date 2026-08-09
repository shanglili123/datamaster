package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据集成SQL模版 DO 对象 COL_ETL_SQL_TEMP
 *
 * @author FXB
 * @date 2025-06-25
 */
@Data
@TableName(value = "COL_ETL_SQL_TEMP")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("COL_ETL_SQL_TEMP_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlSqlTempDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 名称 */
    private String name;

    /** 类型 */
    private String type;

    /** 内容 */
    private String content;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
