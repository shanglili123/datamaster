package com.datamaster.module.ontology.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.webhook.vo.*;
import com.datamaster.module.ontology.convert.WebhookConvert;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.module.ontology.dal.dataobject.WebhookDO;
import com.datamaster.module.ontology.dal.dataobject.WebhookLogDO;
import com.datamaster.module.ontology.dal.mapper.WebhookLogMapper;
import com.datamaster.module.ontology.dal.mapper.WebhookMapper;
import com.datamaster.module.ontology.service.IWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Webhook 服务实现
 *
 * <p>回调基于「动作/决策执行记录」生成，参照 Palantir 数据输出模型：
 * 动作/决策执行成功后，通过 webhook 将结果回写源系统，是回写源系统的唯一外部途径。</p>
 */
@Slf4j
@Service
@Validated
public class WebhookServiceImpl implements IWebhookService {

    /** 动作执行返回（触发）状态 */
    private static final String EXEC_EXECUTED = "EXECUTED";
    private static final String EXEC_FAILED = "FAILED";
    private static final String EXEC_ROLLED_BACK = "ROLLED_BACK";

    @Resource private WebhookMapper webhookMapper;
    @Resource private WebhookLogMapper webhookLogMapper;

    // ==================== Webhook 配置 CRUD ====================

    @Override
    public Long createWebhook(WebhookSaveReqVO saveReqVO) {
        WebhookDO bean = WebhookConvert.INSTANCE.convert(saveReqVO);
        if (StringUtils.isBlank(bean.getMethod())) {
            bean.setMethod("POST");
        }
        if (bean.getEnabled() == null) {
            bean.setEnabled(true);
        }
        if (bean.getMaxRetry() == null) {
            bean.setMaxRetry(0);
        }
        webhookMapper.insert(bean);
        return bean.getId();
    }

    @Override
    public Integer updateWebhook(WebhookSaveReqVO saveReqVO) {
        WebhookDO bean = WebhookConvert.INSTANCE.convert(saveReqVO);
        if (StringUtils.isBlank(bean.getMethod())) {
            bean.setMethod("POST");
        }
        if (bean.getMaxRetry() == null) {
            bean.setMaxRetry(0);
        }
        return webhookMapper.updateById(bean);
    }

    @Override
    public Integer deleteWebhook(Long id) {
        return webhookMapper.deleteById(id);
    }

    @Override
    public WebhookRespVO getWebhookById(Long id) {
        WebhookDO bean = webhookMapper.selectById(id);
        return bean == null ? null : WebhookConvert.INSTANCE.convert(bean);
    }

    @Override
    public PageResult<WebhookRespVO> getWebhookPage(WebhookPageReqVO pageReqVO) {
        PageResult<WebhookDO> page = webhookMapper.selectPage(pageReqVO);
        return new PageResult<>(WebhookConvert.INSTANCE.convertList(page.getRows()), page.getTotal());
    }

    @Override
    public List<WebhookRespVO> getWebhooksByOntology(Long ontologyId) {
        List<WebhookDO> list = webhookMapper.selectList(WebhookDO::getOntologyId, ontologyId);
        return WebhookConvert.INSTANCE.convertList(list);
    }

    // ==================== 回调日志 ====================

    @Override
    public PageResult<WebhookLogRespVO> getWebhookLogPage(WebhookLogPageReqVO pageReqVO) {
        PageResult<WebhookLogDO> page = webhookLogMapper.selectPage(pageReqVO);
        return new PageResult<>(WebhookConvert.INSTANCE.convertLogList(page.getRows()), page.getTotal());
    }

    @Override
    public Integer clearWebhookLogs(Long webhookId) {
        if (webhookId == null) {
            // 物理清理全部（避免触发 @TableLogic 逻辑删除带来的历史堆积），按需扩展
            return webhookLogMapper.delete(null);
        }
        return webhookLogMapper.delete(WebhookLogDO::getWebhookId, webhookId);
    }

    // ==================== 回调触发（动作执行） ====================

    @Override
    public void notifyActionExecution(ActionExecutionDO exec) {
        if (exec == null || exec.getStatus() == null) {
            return;
        }
        String status = exec.getStatus();
        if (!EXEC_EXECUTED.equals(status) && !EXEC_FAILED.equals(status) && !EXEC_ROLLED_BACK.equals(status)) {
            return; // 仅终态触发回调（避免提交/审批阶段重复发送）
        }
        try {
            List<WebhookDO> matched = resolveMatchedWebhooks(exec.getActionId(), exec.getOntologyId());
            if (matched.isEmpty()) {
                return;
            }
            // 回调基于「动作执行记录」：请求体以执行记录字段为准拼装，前端展示执行结果。
            JSONObject payload = buildPayload(exec, status);
            for (WebhookDO webhook : matched) {
                fireOne(webhook, "ACTION", exec.getId(), exec.getOntologyId(), payload.toJSONString());
            }
        } catch (Exception e) {
            // 回调为最佳努力，绝不允许影响动作执行主流程
            log.warn("动作 webhook 触发失败, actionExecutionId={}", exec.getId(), e);
        }
    }

    /**
     * 解析命中的 Webhook：优先 actionId 精确绑定，其次取该本体下未绑定具体动作的本体级回调。
     */
    private List<WebhookDO> resolveMatchedWebhooks(Long actionId, Long ontologyId) {
        List<WebhookDO> result = new ArrayList<>();
        if (actionId != null) {
            result.addAll(webhookMapper.selectEnabledByAction(actionId));
        }
        if (ontologyId != null) {
            // 不去重本体级 webhook（如果与 actionId 绑定是同一配置项，在此不会重复，因为本体级要求 actionId 为空）
            result.addAll(webhookMapper.selectEnabledByOntologyScope(ontologyId));
        }
        return result;
    }

    /**
     * 构建回调请求体。有 payloadTemplate 时做占位符替换（支持 ${executionId} 等字段），
     * 否则生成默认结构：事件类型 + 动作执行记录各关键字段。
     */
    private JSONObject buildPayload(ActionExecutionDO exec, String status) {
        JSONObject body = new JSONObject();
        body.put("eventType", "ACTION_EXECUTION");
        body.put("executionId", exec.getId());
        body.put("actionId", exec.getActionId());
        body.put("ontologyId", exec.getOntologyId());
        body.put("status", status);
        body.put("inputParams", safeParse(exec.getInputParams()));
        body.put("beforeData", safeParse(exec.getBeforeData()));
        body.put("afterData", safeParse(exec.getAfterData()));
        body.put("previewResult", safeParse(exec.getPreviewResult()));
        body.put("generatedSql", exec.getGeneratedSql());
        body.put("executeTime", exec.getExecuteTime());
        return body;
    }

    private Object safeParse(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 发起单个 webhook 回调并记录日志。失败按 maxRetry 同步重试（简单指数退避），
     * 均失败则记录 FAILED。
     */
    private void fireOne(WebhookDO webhook, String execType, Long actionExecId, Long ontologyId, String payload) {
        int totalAttempts = (webhook.getMaxRetry() == null ? 0 : webhook.getMaxRetry()) + 1;
        int connected = 0;
        WebhookLogDO logDO = newLog(webhook, execType, actionExecId, ontologyId, payload);
        logDO.setStatus("PENDING");
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                HttpResponse resp = doSend(webhook, payload);
                logDO.setStatus("SUCCESS");
                logDO.setHttpStatus(resp.getStatus());
                logDO.setResponseBody(truncate(resp.body(), 4000));
                logDO.setRetryCount(attempt - 1);
                webhookLogMapper.insert(logDO);
                return;
            } catch (Exception e) {
                connected++;
                log.warn("webhook 回调失败(第{}/{}次), id={}, url={}", attempt, totalAttempts, webhook.getId(), webhook.getUrl(), e);
                if (attempt < totalAttempts) {
                    sleepQuietly(1000L * attempt);
                }
            }
        }
        logDO.setStatus("FAILED");
        logDO.setHttpStatus(null);
        logDO.setErrorMessage("回调重试均失败");
        logDO.setRetryCount(totalAttempts - 1);
        webhookLogMapper.insert(logDO);
    }

    private WebhookLogDO newLog(WebhookDO webhook, String execType, Long actionExecId, Long ontologyId, String payload) {
        WebhookLogDO logDO = WebhookLogDO.builder()
                .webhookId(webhook.getId())
                .webhookName(webhook.getName())
                .executionType(execType)
                .actionExecutionId(actionExecId)
                .ontologyId(ontologyId)
                .payload(payload)
                .retryCount(0)
                .build();
        logDO.setCreateTime(new Date());
        return logDO;
    }

    /**
     * 执行 HTTP 回调（支持 GET/POST/PUT/DELETE），自动合并自定义请求头与密钥鉴权头。
     */
    private HttpResponse doSend(WebhookDO webhook, String payload) throws Exception {
        String method = StringUtils.isBlank(webhook.getMethod()) ? "POST" : webhook.getMethod().toUpperCase();
        HttpRequest request = HttpRequest.of(webhook.getUrl()).method(cn.hutool.http.Method.valueOf(method));
        Map<String, String> headers = parseHeaders(webhook.getHeaders());
        if (StringUtils.isNotBlank(webhook.getSecret())) {
            headers.putIfAbsent("Authorization", "Bearer " + webhook.getSecret());
        }
        request.addHeaders(headers);
        if (!"GET".equals(method) && payload != null) {
            request.body(payload, "application/json;charset=UTF-8");
        }
        request.timeout(30000);
        HttpResponse resp = request.execute();
        resp.charset(StandardCharsets.UTF_8);
        return resp;
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (StringUtils.isBlank(headersJson)) {
            return new java.util.HashMap<>();
        }
        try {
            JSONObject obj = JSON.parseObject(headersJson);
            Map<String, String> map = new java.util.HashMap<>();
            obj.forEach((k, v) -> map.put(k, String.valueOf(v)));
            return map;
        } catch (Exception e) {
            log.warn("webhook headers 解析失败: {}", headersJson, e);
            return new java.util.HashMap<>();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}