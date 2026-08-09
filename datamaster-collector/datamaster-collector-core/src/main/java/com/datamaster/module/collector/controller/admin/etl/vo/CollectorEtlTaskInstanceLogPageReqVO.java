

package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据集成任务实例-日志 Request VO 对象 COL_ETL_TASK_INSTANCE_LOG
 *
 * @author DATAMASTER
 * @date 2025-08-05
 */
@Schema(description = "数据集成任务实例-日志 Request VO")
@Data
public class CollectorEtlTaskInstanceLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;


    @Schema(description = "任务类型", example = "")
    private String taskType;

    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "任务编码", example = "")
    private String taskCode;

    @Schema(description = "日志内容", example = "")
    private String logContent;




}
