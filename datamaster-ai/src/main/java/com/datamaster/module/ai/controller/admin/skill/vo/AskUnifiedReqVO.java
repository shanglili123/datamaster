package com.datamaster.module.ai.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Unified ask-data request; identity and tool context are always resolved server-side. */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "统一问数请求")
public class AskUnifiedReqVO extends AiAskDataSqlReqVO {
    @Schema(description = "正式指标分析的当前本体ID；探索明细时可不传")
    private Long ontologyId;
    @Schema(description = "决策分析的业务对象主键，可选；不能替代对象权限校验")
    private String objectKey;
    @Schema(description = "普通ASK_DATA会话ID，不接受Data Agent会话")
    private Long sessionId;
    @Schema(description = "指标问数操作ID；只接受服务端返回的ASK_METRIC_命名空间")
    private String operationId;
    @Schema(description = "明确补充待输入任务的服务端操作ID；只恢复目标，不恢复确认或执行权限")
    private String continuationOperationId;
    @Schema(description = "从正式分析进入决策的服务端操作引用；只恢复目标，重新查询，不接收客户端数据行")
    private String analysisOperationId;
    @Schema(description = "明确确认服务端已展示的保存分析参数")
    private Boolean confirmed;
    @Schema(description = "探索报告模板ID，仅responseMode=report生效")
    private Long reportTemplateId;
    @Schema(description = "探索报告结构化参数")
    private java.util.Map<String, Object> reportParams;
}
