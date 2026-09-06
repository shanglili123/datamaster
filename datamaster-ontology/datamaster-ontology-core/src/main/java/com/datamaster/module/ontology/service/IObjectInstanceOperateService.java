package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RowOperateReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RowOperateRespVO;

/**
 * 对象实例行操作 Service — 对象管理列表内直接对单条数据做新增/修改/删除
 *
 * <p>与普通动作走完全一致的执行链路（提交 → 审批 → 执行 → 快照/血缘/Webhook）：
 * <ul>
 *   <li>{@code preview}：按概念惰性创建/复用「内置」CREATE/UPDATE/DELETE 动作并提交执行（生成 SQL + dry-run + 按需建审批链）；</li>
 *   <li>{@code confirm}：审批通过（当前用户可审时，即「二次弹框确定=审批通过」）并真正执行。</li>
 * </ul>
 * 内置动作默认直接执行，可在动作面板调整为一次人工确认、绑定唯一确认人或挂 Webhook 回调。
 */
public interface IObjectInstanceOperateService {

    /**
     * 提交阶段（预览）：惰性创建内置动作 → submitExecution（dry-run，业务库不改动）。
     *
     * @param reqVO 行操作请求（data 以属性编码为 key）
     * @return 执行记录 + 状态（需审批时含 canApprove）+ SQL 预览
     */
    RowOperateRespVO preview(RowOperateReqVO reqVO);

    /**
     * 确认阶段：用户确认预览内容后执行；若动作配置了人工确认，则由独立确认中心处理。
     *
     * @param reqVO 行操作请求（executionId 来自 preview）
     * @return 执行结果（EXECUTED / FAILED 终态）
     */
    RowOperateRespVO confirm(RowOperateReqVO reqVO);
}
