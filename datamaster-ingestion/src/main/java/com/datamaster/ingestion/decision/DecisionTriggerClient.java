package com.datamaster.ingestion.decision;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.datamaster.ingestion.config.IngestionProperties;
import com.datamaster.module.ontology.api.IActionTriggerApiService;
import com.datamaster.module.ontology.api.dto.ActionDataArrivalTriggerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 本体动作「数据到达触发」回调客户端
 *
 * <p>接收数据写入数仓后，按配置回调本体动作接口（/ont/action/trigger/data-arrival），
 * 触发启用且 triggerRef 匹配的数据到达型动作自动提交执行（替代原 /ont/decision/trigger/data-arrival）。
 * 回调失败不影响接收主流程。</p>
 */
@Slf4j
@Service
public class DecisionTriggerClient {

    @Resource
    private IngestionProperties properties;

    /** 聚合部署时存在；独立接收进程没有该实现，自动回退 HTTP。 */
    @Autowired(required = false)
    private IActionTriggerApiService actionTriggerApiService;

    /**
     * 触发数据到达型动作。
     *
     * @param topic 来源 topic（缺省作为 triggerRef）
     * @param inputParams 数据事件载荷（可空，原样透传）
     * @param objectKey   对象稳定主键（联合主键为 JSON）
     * @param eventId     来源事件唯一编号
     * @param spaceId     来源空间 ID（可空）
     * @param spaceCode   来源空间编码（可空）
     */
    public void trigger(String topic, String inputParams, String objectKey, String eventId,
                        Long spaceId, String spaceCode) {
        IngestionProperties.DecisionTrigger trigger = properties.getDecisionTrigger();
        if (trigger == null || !trigger.isEnabled()) {
            return;
        }
        String triggerRef = (trigger.getTriggerRef() == null || trigger.getTriggerRef().trim().isEmpty())
                ? topic : trigger.getTriggerRef();
        ActionDataArrivalTriggerDTO request = new ActionDataArrivalTriggerDTO();
        request.setTriggerRef(triggerRef);
        request.setInputParams(inputParams);
        request.setObjectKey(objectKey);
        request.setEventId(eventId);
        request.setSpaceId(spaceId);
        request.setSpaceCode(spaceCode);

        if (actionTriggerApiService != null) {
            try {
                actionTriggerApiService.triggerDataArrival(request);
                log.info("数据到达动作触发成功（进程内） triggerRef={} eventId={}", triggerRef, eventId);
            } catch (Exception e) {
                log.warn("数据到达动作触发失败（进程内） triggerRef={} eventId={}", triggerRef, eventId, e);
            }
            return;
        }

        String baseUrl = trigger.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.warn("动作数据到达触发已启用但未配置 base-url，跳过触发 topic={}", topic);
            return;
        }
        if (trigger.getSecret() == null || trigger.getSecret().trim().isEmpty()) {
            log.warn("独立部署 HTTP 动作触发未配置 secret，拒绝发送 triggerRef={}", triggerRef);
            return;
        }
        String url = baseUrl.trim();
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        String path = trigger.getPath() == null ? "/ont/action/trigger/data-arrival" : trigger.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        url = url + path;

        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("X-DataMaster-Trigger-Secret", trigger.getSecret())
                    .contentType("application/json")
                    .body(JSON.toJSONString(request))
                    .timeout(5000)
                    .execute();
            if (!response.isOk()) {
                log.warn("动作数据到达触发回调状态异常 triggerRef={} eventId={} status={}",
                        triggerRef, eventId, response.getStatus());
            } else {
                log.info("动作数据到达触发回调成功 triggerRef={} eventId={} status={}",
                        triggerRef, eventId, response.getStatus());
            }
        } catch (Exception e) {
            log.warn("动作数据到达触发回调失败 triggerRef={} eventId={}", triggerRef, eventId, e);
        }
    }
}
