package com.datamaster.quality.dal.dataobject.qa;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据质量任务-稽查对象 DO 对象 COL_QUALITY_TASK_OBJ
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Data
@TableName(value = "COL_QUALITY_TASK_OBJ")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("COL_QUALITY_TASK_OBJ_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QualityTaskObjDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 数据质量任务ID */
    private Long taskId;

    /** 稽查对象名称 */
    private String name;

    /** 数据源id */
    private Long datasourceId;

    /** 表名称 */
    private String tableName;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
