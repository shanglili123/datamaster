

package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 数据集成任务-扩展数据 创建/修改 Request VO COL_ETL_TASK_EXT
 *
 * @author DATAMASTER
 * @date 2025-04-16
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据集成任务-扩展数据 Response VO")
@Data
public class CollectorEtlTaskExtSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "数据汇聚任务id", example = "")
    private Long taskId;

    @Schema(description = "数据汇聚任务编码", example = "")
    private String etlTaskCode;

    @Schema(description = "数据汇聚任务版本", example = "")
    private Integer etlTaskVersion;

    @Schema(description = "数据汇聚节点id", example = "")
    private Long etlNodeId;

    @Schema(description = "数据汇聚节点名称", example = "")
    @Size(max = 256, message = "数据汇聚节点名称长度不能超过256个字符")
    private String etlNodeName;

    @Schema(description = "数据汇聚节点编码", example = "")
    @Size(max = 256, message = "数据汇聚节点编码长度不能超过256个字符")
    private String etlNodeCode;

    @Schema(description = "数据汇聚节点版本", example = "")
    private Integer etlNodeVersion;

    @Schema(description = "数据汇聚节点关系id", example = "")
    private Long etlRelationId;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;

    @Schema(description = "FlinkX任务JSON配置", example = "")
    private String flinkxJobJson;

    @Schema(description = "FlinkX基础任务JSON模板", example = "")
    private String flinkxJobTemplateJson;

    @Schema(description = "增量类型：ID、TIME", example = "ID")
    private String incrementalType;

    @Schema(description = "源数据源ID", example = "")
    private Long sourceDatasourceId;

    @Schema(description = "目标数据源ID", example = "")
    private Long targetDatasourceId;

    @Schema(description = "源表名称", example = "")
    private String sourceTableName;

    @Schema(description = "目标表名称", example = "")
    private String targetTableName;

    @Schema(description = "源表增量字段", example = "")
    private String sourceIncrementColumn;

    @Schema(description = "目标表增量字段", example = "")
    private String targetIncrementColumn;

    @Schema(description = "首次增量同步初始游标", example = "")
    private String incrementalInitialValue;

    @Schema(description = "时间增量边界格式", example = "yyyy-MM-dd HH:mm:ss.SSSSSS")
    private String incrementalTimeFormat;

    @Schema(description = "本次增量同步起始值", example = "")
    private String incrementalStartValue;

    @Schema(description = "本次增量同步结束值", example = "")
    private String incrementalEndValue;

    @Schema(description = "增量准备HTTP节点ID", example = "")
    private Long prepareNodeId;

    @Schema(description = "增量准备HTTP节点名称", example = "")
    private String prepareNodeName;

    @Schema(description = "增量准备HTTP节点编码", example = "")
    private String prepareNodeCode;

    @Schema(description = "增量准备HTTP节点版本", example = "")
    private Integer prepareNodeVersion;

    @Schema(description = "根节点到增量准备HTTP节点的关系ID", example = "")
    private Long prepareRelationId;

    @Schema(description = "增量完成HTTP节点ID", example = "")
    private Long completeNodeId;

    @Schema(description = "增量完成HTTP节点名称", example = "")
    private String completeNodeName;

    @Schema(description = "增量完成HTTP节点编码", example = "")
    private String completeNodeCode;

    @Schema(description = "增量完成HTTP节点版本", example = "")
    private Integer completeNodeVersion;

    @Schema(description = "CHUNJUN节点到增量完成HTTP节点的关系ID", example = "")
    private Long completeRelationId;

}
