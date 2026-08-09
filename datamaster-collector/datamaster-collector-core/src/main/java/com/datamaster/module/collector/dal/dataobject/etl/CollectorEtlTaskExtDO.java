package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据集成任务-扩展数据 DO 对象 COL_ETL_TASK_EXT
 *
 * @author DATAMASTER
 * @date 2025-04-16
 */
@Data
@TableName(value = "COL_ETL_TASK_EXT")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("COL_ETL_TASK_EXT_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectorEtlTaskExtDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 数据汇聚任务id */
    private Long taskId;

    /**
     * 数据汇聚任务编码
     */
    private String etlTaskCode;
    /**
     * 数据汇聚任务版本
     */
    private Integer etlTaskVersion;

    /** 数据汇聚节点id */
    private Long etlNodeId;

    /** 数据汇聚节点名称 */
    private String etlNodeName;

    /** 数据汇聚节点编码 */
    private String etlNodeCode;

    /** 数据汇聚节点版本 */
    private Integer etlNodeVersion;

    /** 数据汇聚节点关系id */
    private Long etlRelationId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

    /** FlinkX 任务JSON（当执行引擎为FlinkX时使用） */
    private String flinkxJobJson;

    /** FlinkX 基础任务JSON（每次增量执行前基于该模板重建where条件） */
    private String flinkxJobTemplateJson;

    /** 增量类型：ID、TIME */
    private String incrementalType;

    /** 源数据源ID */
    private Long sourceDatasourceId;

    /** 目标数据源ID */
    private Long targetDatasourceId;

    /** 源表名称 */
    private String sourceTableName;

    /** 目标表名称 */
    private String targetTableName;

    /** 源表增量字段 */
    private String sourceIncrementColumn;

    /** 目标表增量字段 */
    private String targetIncrementColumn;

    /** 首次增量同步初始游标 */
    private String incrementalInitialValue;

    /** 时间增量边界格式 */
    private String incrementalTimeFormat;

    /** 本次增量同步起始值 */
    private String incrementalStartValue;

    /** 本次增量同步结束值 */
    private String incrementalEndValue;

    /** 增量准备HTTP节点ID */
    private Long prepareNodeId;

    /** 增量准备HTTP节点名称 */
    private String prepareNodeName;

    /** 增量准备HTTP节点编码 */
    private String prepareNodeCode;

    /** 增量准备HTTP节点版本 */
    private Integer prepareNodeVersion;

    /** 根节点到增量准备HTTP节点的关系ID */
    private Long prepareRelationId;

    /** 增量完成HTTP节点ID */
    private Long completeNodeId;

    /** 增量完成HTTP节点名称 */
    private String completeNodeName;

    /** 增量完成HTTP节点编码 */
    private String completeNodeCode;

    /** 增量完成HTTP节点版本 */
    private Integer completeNodeVersion;

    /** CHUNJUN节点到增量完成HTTP节点的关系ID */
    private Long completeRelationId;

}
