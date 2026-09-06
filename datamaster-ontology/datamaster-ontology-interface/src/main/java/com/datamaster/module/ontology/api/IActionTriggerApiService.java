package com.datamaster.module.ontology.api;

import com.datamaster.module.ontology.api.dto.ActionDataArrivalTriggerDTO;

import java.util.List;

/**
 * 本体动作触发跨模块接口。
 *
 * <p>聚合部署时由数据接收模块直接调用，避免对本机 HTTP 回调产生额外故障点。</p>
 */
public interface IActionTriggerApiService {

    /**
     * 提交匹配的数据到达动作。
     *
     * @return 创建或幂等命中的动作执行记录 ID
     */
    List<Long> triggerDataArrival(ActionDataArrivalTriggerDTO request);
}
