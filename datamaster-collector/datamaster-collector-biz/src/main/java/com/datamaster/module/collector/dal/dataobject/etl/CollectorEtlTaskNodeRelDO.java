package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据集成任务节点关系 DO 对象 COL_ETL_TASK_NODE_REL
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Data
@TableName(value = "COL_ETL_TASK_NODE_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("COL_ETL_TASK_NODE_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlTaskNodeRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 项目id */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 任务id */
    private Long taskId;

    /** 任务编码 */
    private String taskCode;

    /** 任务版本 */
    private Long taskVersion;

    /** 前节点id */
    private Long preNodeId;

    /** 前节点编码 */
    private String preNodeCode;

    /** 前节点版本 */
    private Long preNodeVersion;

    /** 后节点id */
    private Long postNodeId;

    /** 后节点编码 */
    private String postNodeCode;

    /** 后节点版本 */
    private Long postNodeVersion;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
