package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Schema(description = "执行提交 Request VO")
@Data
public class ExecutionSubmitReqVO {

    @Schema(description = "动作ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "动作ID不能为空")
    private Long actionId;

    @Schema(description = "输入参数JSON", example = "{\"name\":\"张三\",\"age\":30}")
    private String inputParams;

    @Schema(description = "触发对象主键；多目标动作的内部步骤共享这次触发上下文", example = "CUST001")
    private String objectKey;

    /** 幂等键：同动作下唯一。重复提交（同 actionId + idempotencyKey）直接返回首次执行记录，防重复执行 */
    @Schema(description = "幂等键（可选）：同动作下唯一，重复提交直接返回首次执行记录，防止数据到达触发等场景重复执行", example = "evt-20260905-0001")
    private String idempotencyKey;

    @Schema(description = "兼容字段；是否直接执行只由动作的人工确认配置决定，调用方不能绕过")
    private Boolean autoExecute;

    @Schema(description = "触发类型", example = "DATA_ARRIVAL")
    private String triggerType;

    @Schema(description = "触发器引用", example = "customer_stream")
    private String triggerRef;

    @Schema(description = "来源事件唯一编号", example = "customer_stream:0:1024")
    private String eventId;

    @Schema(description = "最大执行尝试次数；V1 写动作默认 1，不自动重放")
    private Integer maxAttempts;

    @Schema(description = "最早可执行时间；为空表示立即进入自动执行队列")
    private Date nextRunTime;

    /** 当前空间ID：由前端拦截器自动注入，用于资产字段权限校验 */
    @Schema(description = "当前空间ID（前端自动注入）", example = "1")
    private Long spaceId;

    /** 当前空间编码：由前端拦截器自动注入 */
    @Schema(description = "当前空间编码（前端自动注入）", example = "space_demo")
    private String spaceCode;
}
