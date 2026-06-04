package com.datamaster.module.catalog.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 采集任务实例-日志 Request VO 对象 Catalog_TASK_INSTANCE_LOG
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Schema(description = "采集任务实例-日志 Request VO")
@Data
public class CatalogTaskInstanceLogPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;


    @Schema(description = "任务id", example = "")
    private Long taskId;

    @Schema(description = "日志内容", example = "")
    private String logContent;




    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
