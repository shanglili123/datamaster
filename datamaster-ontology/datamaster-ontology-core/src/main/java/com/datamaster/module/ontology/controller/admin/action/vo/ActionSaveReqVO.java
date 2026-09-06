package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "动作保存 Request VO")
@Data
public class ActionSaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;

    @Schema(description = "动作名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "创建客户")
    @NotBlank(message = "动作名称不能为空")
    private String name;

    @Schema(description = "动作类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CREATE")
    @NotBlank(message = "动作类型不能为空")
    private String actionType;

    @Schema(description = "触发对象类型ID（FUNCTION 类型动作不需要）", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Long conceptId;

    @Schema(description = "多目标动作的有序执行步骤 JSON 数组")
    private String executionSteps;

    @Schema(description = "绑定共享函数ID（actionType=FUNCTION 时必填，来自 ONT_FUNCTION 表）", example = "1")
    private Long functionId;

    @Schema(description = "动作绑定函数的读取来源概念ID（FUNCTION 类型动作选填，绑定后注入 input.source.rows）", example = "1")
    private Long sourceConceptId;

    @Schema(description = "可选关联关系ID JSON数组（FUNCTION 类型动作选填，注入 input.source.relations）")
    private String sourceRelationIds;

    @Schema(description = "输出目标概念ID（FUNCTION 类型动作选填，脚本 JSON 结果按主键 UPSERT 到该概念物理表）", example = "1")
    private Long outputConceptId;

    @Schema(description = "数据来源读取行数上限（默认 5000）", example = "5000")
    private Integer readLimit;

    @Schema(description = "是否需要一次人工确认；false=提交后直接由 Worker 执行", example = "true")
    private Boolean needsApproval;

    @Schema(description = "执行前置检查 JSONB：提交时和真正执行前均求值，决定 PASS/REJECT；是否人工确认由执行模式独立决定", example = "{\"logic\":\"AND\",\"conditions\":[{\"field\":\"param.amount\",\"op\":\"gt\",\"value\":1000}]}")
    private String submissionCriteria;

    @Schema(description = "人工确认次数兼容字段：0=直接执行，1=一次人工确认；服务端不接受多级审批", example = "1")
    private Integer approvalLevels;

    @Schema(description = "唯一人工确认人兼容 JSON 数组：[{\"stage\":1,\"userId\":1001,\"userName\":\"张三\"}]；空=任意登录用户可确认", example = "[{\"stage\":1,\"userId\":1001,\"userName\":\"张三\"}]")
    private String approvalReviewers;

    @Schema(description = "历史触发标识兼容字段；新动作的触发来源由运行时传入")
    private String triggerRef;

    @Schema(description = "执行参数配置(JSON数组)：属性选择+目标值配置，valueMode=direct|placeholder|relative|expression；relative 表示当前值加减运算")
    private String paramConfig;

    @Schema(description = "描述", example = "创建新客户记录")
    private String description;
}
