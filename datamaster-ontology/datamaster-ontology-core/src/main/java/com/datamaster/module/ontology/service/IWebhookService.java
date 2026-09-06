package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookLogPageReqVO;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookLogRespVO;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookPageReqVO;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookRespVO;
import com.datamaster.module.ontology.controller.admin.webhook.vo.WebhookSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;

import java.util.List;

/**
 * Webhook 服务：执行动作/决策后回写源系统（回调基于执行记录生成）。
 */
public interface IWebhookService {

    /** 新增 Webhook 配置 */
    Long createWebhook(WebhookSaveReqVO saveReqVO);

    /** 修改 Webhook 配置 */
    Integer updateWebhook(WebhookSaveReqVO saveReqVO);

    /** 删除 Webhook 配置 */
    Integer deleteWebhook(Long id);

    /** 获取 Webhook 详情 */
    WebhookRespVO getWebhookById(Long id);

    /** 分页查询 Webhook 配置 */
    PageResult<WebhookRespVO> getWebhookPage(WebhookPageReqVO pageReqVO);

    /** 查询某本体下可用（绑定该本体、可被某动作触发的）Webhook 列表 */
    List<WebhookRespVO> getWebhooksByOntology(Long ontologyId);

    /** 分页查询回调日志 */
    PageResult<WebhookLogRespVO> getWebhookLogPage(WebhookLogPageReqVO pageReqVO);

    /** 清除回调日志（按 webhookId 可选，空则全部） */
    Integer clearWebhookLogs(Long webhookId);

    /**
     * 动作执行记录产生后（EXECUTED/FAILED/ROLLED_BACK），触发绑定该动作/本体的全部 webhook 回调。
     * <p>静默最佳努力：回调失败仅记录日志，绝不影响动作执行主流程。</p>
     */
    void notifyActionExecution(ActionExecutionDO exec);
}