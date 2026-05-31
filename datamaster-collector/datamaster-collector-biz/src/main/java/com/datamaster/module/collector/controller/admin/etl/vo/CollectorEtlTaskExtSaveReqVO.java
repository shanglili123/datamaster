

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


}
