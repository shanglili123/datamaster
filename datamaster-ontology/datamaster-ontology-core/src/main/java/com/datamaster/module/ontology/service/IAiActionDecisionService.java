package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.action.vo.AiActionDecisionReqVO;
import com.datamaster.module.ontology.controller.admin.action.vo.AiActionDecisionRespVO;

/** AI 本体动作决策服务。 */
public interface IAiActionDecisionService {

    /**
     * 根据自然语言意图在已配置动作中做结构化决策，并可选择提交到现有执行队列。
     */
    AiActionDecisionRespVO decide(AiActionDecisionReqVO reqVO);
}
