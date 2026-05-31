

package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据集成节点实例-日志 Request VO 对象 COL_ETL_NODE_INSTANCE_LOG
 *
 * @author DATAMASTER
 * @date 2025-08-05
 */
@Schema(description = "数据集成节点实例-日志 Request VO")
@Data
public class CollectorEtlNodeInstanceLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;


    @Schema(description = "任务类型", example = "")
    private String taskType;

    @Schema(description = "节点id", example = "")
    private Long nodeId;

    @Schema(description = "节点编码", example = "")
    private String nodeCode;

    @Schema(description = "任务实例id", example = "")
    private Long taskInstanceId;

    @Schema(description = "日志内容", example = "")
    private String logContent;




}
