package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Schema(description = "执行记录 Response VO")
@Data
public class ExecutionRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号", example = "1")
    @Excel(name = "编号")
    private Long id;

    @Schema(description = "动作ID", example = "1")
    @Excel(name = "动作ID")
    private Long actionId;

    @Schema(description = "本体ID", example = "1")
    @Excel(name = "本体ID")
    private Long ontologyId;

    @Schema(description = "输入参数", example = "{\"name\":\"张三\"}")
    @Excel(name = "输入参数")
    private String inputParams;

    @Schema(description = "生成的SQL", example = "INSERT INTO t_customer (name) VALUES ('张三')")
    @Excel(name = "生成SQL")
    private String generatedSql;

    @Schema(description = "预览结果")
    @Excel(name = "预览结果")
    private String previewResult;

    @Schema(description = "执行前数据")
    @Excel(name = "执行前数据")
    private String beforeData;

    @Schema(description = "状态", example = "DRAFT")
    @Excel(name = "状态", readConverterExp = "DRAFT=草稿,PENDING_APPROVAL=待审批,APPROVED=已批准,REJECTED=已拒绝,EXECUTED=已执行,FAILED=失败")
    private String status;

    @Schema(description = "审批意见", example = "同意")
    @Excel(name = "审批意见")
    private String approvalReason;

    @Schema(description = "审批者", example = "admin")
    @Excel(name = "审批者")
    private String approverId;

    @Schema(description = "审批时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    @Schema(description = "执行时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date executeTime;

    @Schema(description = "错误信息")
    @Excel(name = "错误信息")
    private String errorMessage;

    @Schema(description = "创建者", example = "admin")
    @Excel(name = "创建者")
    private String createBy;

    @Schema(description = "创建时间", example = "2026-08-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
