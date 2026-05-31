package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据集成节点 DO 对象 COL_ETL_NODE
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Data
@TableName(value = "COL_ETL_NODE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("COL_ETL_NODE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlNodeDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 任务类型;1：离线任务 2：实时任务 3：数据开发任务 4：作业任务 */
    private String taskType;

    /** 节点类型 */
    private String type;

    /** 节点名称 */
    private String name;

    /** 节点编码 */
    private String code;

    /** 节点版本 */
    private Long version;

    /** 项目id */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 节点参数 */
    private String parameters;

    /** 任务优先级 */
    private String priority;

    /** 失败重试次数 */
    private Long failRetryTimes;

    /** 失败重试间隔（分钟） */
    private Long failRetryInterval;

    /** 超时时间 */
    private Long timeout;

    /** 延迟执行时间（分钟） */
    private Long delayTime;

    /** CPU配额 */
    private Long cpuQuota;

    /** 最大内存 */
    private Long memoryMax;

    /** 描述 */
    private String description;

    /** 组件类型 */
    private String componentType;

    /** DolphinScheduler的id */
    private Long dsId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
