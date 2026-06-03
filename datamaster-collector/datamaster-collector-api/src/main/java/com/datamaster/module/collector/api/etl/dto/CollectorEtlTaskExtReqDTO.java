

package com.datamaster.module.collector.api.etl.dto;

import lombok.Data;

/**
 * 数据集成任务-扩展数据 DTO 对象 COL_ETL_TASK_EXT
 *
 * @author DATAMASTER
 * @date 2025-04-16
 */
@Data
public class CollectorEtlTaskExtReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 数据汇聚任务id */
    private Long taskId;

    /** 数据汇聚节点id */
    private Long etlNodeId;

    /** 数据汇聚节点名称 */
    private String etlNodeName;

    /** 数据汇聚节点编码 */
    private String etlNodeCode;

    /** 数据汇聚节点版本 */
    private Long etlNodeVersion;

    /** 数据汇聚节点关系id */
    private Long etlRelationId;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;

    /** FlinkX 任务JSON（当执行引擎为FlinkX时使用） */
    private String flinkxJobJson;

    /** FlinkX 基础任务JSON */
    private String flinkxJobTemplateJson;

    /** 增量类型：ID、TIME */
    private String incrementalType;

    /** 首次增量同步初始游标 */
    private String incrementalInitialValue;

    /** 时间增量边界格式 */
    private String incrementalTimeFormat;

    /** 本次增量同步起始值 */
    private String incrementalStartValue;

    /** 本次增量同步结束值 */
    private String incrementalEndValue;

    /** 增量完成HTTP节点编码 */
    private String completeNodeCode;

}
