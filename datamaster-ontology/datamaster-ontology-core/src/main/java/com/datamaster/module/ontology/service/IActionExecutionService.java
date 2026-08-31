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
    ExecutionRespVO executeExecution(Long executionId);

    /** 回退已执行的记录（按 before/after 快照构建还原 SQL） */
    ExecutionRespVO rollbackExecution(Long executionId);

    /** 获取执行详情 */
    ExecutionRespVO getExecutionById(Long id);

    /** 分页查询执行记录 */
    PageResult<ExecutionRespVO> getExecutionPage(ExecutionPageReqVO pageReqVO);

    /** 查询待审批列表 */
    java.util.List<ExecutionRespVO> getPendingApprovals(Long ontologyId);
}
