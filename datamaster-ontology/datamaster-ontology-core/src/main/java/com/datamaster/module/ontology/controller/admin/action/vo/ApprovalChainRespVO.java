package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 人工确认响应 VO：对象执行记录的一次人工确认及永久审计。
 */
@Schema(description = "审批链 Response VO")
@Data
public class ApprovalChainRespVO {

    @Schema(description = "审批请求ID")
    private Long requestId;

    @Schema(description = "动作执行记录ID")
    private Long actionExecutionId;

    @Schema(description = "目标对象主键")
    private String objectKey;

    @Schema(description = "总关卡数")
    private Integer totalStages;

    @Schema(description = "已推进到的关卡")
    private Integer currentStage;

    @Schema(description = "审批流状态：PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "提交人")
    private String requestByName;

    @Schema(description = "提交时间")
    private Date requestTime;

    @Schema(description = "关卡任务列表")
    private List<TaskVO> tasks = new ArrayList<>();

    @Schema(description = "人工确认任务（当前最多一条）")
    @Data
    public static class TaskVO {

        @Schema(description = "关卡任务ID")
        private Long taskId;

        @Schema(description = "关卡序号")
        private Integer stageNo;

        @Schema(description = "关卡状态：PENDING/APPROVED/REJECTED")
        private String status;

        @Schema(description = "本关卡需同意人数")
        private Integer requiredApprovals;

        @Schema(description = "本关卡已同意人数")
        private Integer approvedCount;

        @Schema(description = "本关卡指定审批人ID（空=任意登录用户可审）")
        private Long approverId;

        @Schema(description = "本关卡指定审批人姓名")
        private String approverName;

        @Schema(description = "本关卡审阅人决策审计（永久留痕）")
        private List<ReviewerVO> reviewers = new ArrayList<>();
    }

    @Schema(description = "审阅人决策审计")
    @Data
    public static class ReviewerVO {

        @Schema(description = "审阅人ID")
        private Long reviewerId;

        @Schema(description = "审阅人姓名")
        private String reviewerName;

        @Schema(description = "决策：APPROVE/REJECT")
        private String decision;

        @Schema(description = "审批意见（可选）")
        private String reason;

        @Schema(description = "决策时间")
        private Date decideTime;
    }
}
