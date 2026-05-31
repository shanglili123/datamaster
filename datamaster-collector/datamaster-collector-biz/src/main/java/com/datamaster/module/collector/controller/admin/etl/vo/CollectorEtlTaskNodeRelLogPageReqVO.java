

package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据集成任务节点关系-日志 Request VO 对象 COL_ETL_TASK_NODE_REL_LOG
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Schema(description = "数据集成任务节点关系-日志 Request VO")
@Data
public class CollectorEtlTaskNodeRelLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "项目id", example = "")
    private Long projectId;

    @Schema(description = "项目编码", example = "")
    private String projectCode;

    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "任务编码", example = "")
    private String taskCode;

    @Schema(description = "任务版本", example = "")
    private Integer taskVersion;

    @Schema(description = "前节点id", example = "")
    private Long preNodeId;

    @Schema(description = "前节点编码", example = "")
    private String preNodeCode;

    @Schema(description = "前节点版本", example = "")
    private Integer preNodeVersion;

    @Schema(description = "后节点id", example = "")
    private Long postNodeId;

    @Schema(description = "后节点编码", example = "")
    private String postNodeCode;

    @Schema(description = "后节点版本", example = "")
    private Long postNodeVersion;




}
