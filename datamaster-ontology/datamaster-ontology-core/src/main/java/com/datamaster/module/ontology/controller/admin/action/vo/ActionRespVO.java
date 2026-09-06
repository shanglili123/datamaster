package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "动作 Response VO")
@Data
public class ActionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "本体ID", example = "1")
    @Excel(name = "本体ID")
    private Long ontologyId;

    @Schema(description = "动作名称", example = "创建客户")
    @Excel(name = "动作名称")
    private String name;

    @Schema(description = "动作类型", example = "CREATE")
    @Excel(name = "动作类型")
    private String actionType;

    @Schema(description = "触发对象类型ID", example = "1")
    @Excel(name = "触发对象类型ID")
    private Long conceptId;

    @Schema(description = "多目标动作的有序执行步骤 JSON 数组")
    private String executionSteps;

    @Schema(description = "绑定共享函数ID（actionType=FUNCTION 时绑定的 ONT_FUNCTION 表ID）", example = "1")
    private Long functionId;

    @Schema(description = "动作绑定函数的读取来源概念ID（FUNCTION 类型动作）", example = "1")
    private Long sourceConceptId;

    @Schema(description = "可选关联关系ID JSON数组（FUNCTION 类型动作）")
    private String sourceRelationIds;

    @Schema(description = "输出目标概念ID（FUNCTION 类型动作）", example = "1")
    private Long outputConceptId;

    @Schema(description = "数据来源读取行数上限（默认 5000）", example = "5000")
    private Integer readLimit;

    @Schema(description = "是否需要一次人工确认；false=直接执行", example = "true")
    private Boolean needsApproval;

    @Schema(description = "执行前置检查 JSONB：决定 PASS/REJECT；与人工确认模式独立")
    private String submissionCriteria;

    @Schema(description = "人工确认次数兼容字段：0=直接执行，1=一次人工确认")
    private Integer approvalLevels;

    @Schema(description = "唯一人工确认人兼容 JSON 数组；空=任意登录用户可确认")
    private String approvalReviewers;

    @Schema(description = "历史触发标识兼容字段；触发来源以执行记录为准")
    private String triggerRef;

    @Schema(description = "执行参数配置(JSON数组)：属性选择+目标值配置")
    private String paramConfig;

    @Schema(description = "描述", example = "创建新客户记录")
    @Excel(name = "描述")
    private String description;

    @Schema(description = "动作定义版本号：提交执行时冻结，执行前复核", example = "1")
    private Integer version;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
