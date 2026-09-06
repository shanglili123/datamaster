package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;

/**
 * 动作执行 Service
 */
public interface IActionExecutionService {

    /** 提交执行（生成SQL + dry-run） */
    ExecutionRespVO submitExecution(ExecutionSubmitReqVO submitReqVO);

    /** 审批通过 */
    void approveExecution(ApprovalReqVO approvalReqVO);

    /** 审批拒绝 */
    void rejectExecution(ApprovalReqVO approvalReqVO);

    /** 执行已批准的动作 */
    default ExecutionRespVO executeExecution(Long executionId) {
        return executeExecution(executionId, true);
    }

    /**
     * 执行已批准的动作
     *
     * @param executionId    执行记录ID
     * @param triggerWebhook 是否触发动作 Webhook 回调（对象管理行操作的「回调开关」关闭时=false）
     */
    ExecutionRespVO executeExecution(Long executionId, boolean triggerWebhook);

    /** 回退已执行的记录（按 before/after 快照构建还原 SQL） */
    ExecutionRespVO rollbackExecution(Long executionId);

    /** 获取执行详情 */
    ExecutionRespVO getExecutionById(Long id);

    /** 分页查询执行记录 */
    PageResult<ExecutionRespVO> getExecutionPage(ExecutionPageReqVO pageReqVO);

    /** 查询待审批列表 */
    java.util.List<ExecutionRespVO> getPendingApprovals(Long ontologyId);

    /**
     * 数据到达触发：匹配 triggerRef 的已启用动作（triggerRef 非空）逐条自动提交（对象绑定决策载体）。
     * 由接收工程在数据写入后回调（替代原 /ont/decision/trigger/data-arrival）。
     */
    java.util.List<ExecutionRespVO> submitByTrigger(String triggerRef, String inputParams, String objectKey,
                                                    String eventId, Long spaceId, String spaceCode);
}
