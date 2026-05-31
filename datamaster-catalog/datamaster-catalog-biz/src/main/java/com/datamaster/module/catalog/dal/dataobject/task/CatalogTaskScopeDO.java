package com.datamaster.module.catalog.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 采集范围 DO 对象 Catalog_TASK_SCOPE
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Data
@TableName(value = "CAT_TASK_SCOPE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("Catalog_TASK_SCOPE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogTaskScopeDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 任务id */
    private Long taskId;

    /** 数据库名称 */
    private String dbName;

    /** 模式名 */
    private String schemaName;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

    /** 描述 */
    private String description;

}
