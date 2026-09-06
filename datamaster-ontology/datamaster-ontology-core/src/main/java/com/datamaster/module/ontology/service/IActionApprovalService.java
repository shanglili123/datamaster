package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.action.vo.ApprovalChainRespVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;

/**
 * 对象绑定动作人工确认服务
 *
 * <p>前置条件负责机器判定；本服务只提供可选的一次人工确认及永久审计。</p>
 */
public interface IActionApprovalService {

    /**
     * ① 提交时判定：对「提交参数 + 目标对象主键 + 当前用户/时间」求值 Action.submissionCriteria 条件。
     *
     * @return 判定 JSON 字符串：{"decision":"PASS|REJECT","passed":boolean,"detail":[{field,op,value,actual,match}]}；
     *         无条件（criteria 为空）时返回 passed=true / decision=PASS。
     */
    String evaluateCriteria(ActionDO action, String inputParams, String objectKey);

    /**
     * 初始化唯一人工确认任务。
     * 仅当 execution 状态为 PENDING_APPROVAL 时调用。
     */
    void initChain(ActionExecutionDO exec, ActionDO action);

    /**
     * 记录当前登录确认人的一次结论（永久审计，意见可选）。
     *
     * @return 确认结果："APPROVED" / "REJECTED"；
     *         null 表示该执行记录没有审批链（旧单步路径，由调用方按旧逻辑直接设置状态）。
     */
    String submitReviewerDecision(Long executionId, boolean approve, String reason);

    /**
     * 查询对象执行记录的完整审批链（请求 + 关卡任务 + 审阅人决策审计）；无审批链返回 null。
     */
    ApprovalChainRespVO getChain(Long executionId);

    /**
     * 当前登录用户是否为该执行记录指定确认人（未绑定确认人=任意登录用户可确认）。
     * 用于执行列表「通过/拒绝」按钮可见性与待办过滤。
     */
    boolean canApprove(Long executionId);
}
