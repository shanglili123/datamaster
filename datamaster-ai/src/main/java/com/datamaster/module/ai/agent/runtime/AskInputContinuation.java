package com.datamaster.module.ai.agent.runtime;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import java.util.Objects;

/** Only combines server-owned goal text with user supplements; never restores tool authority. */
public final class AskInputContinuation {
    private AskInputContinuation() { }

    public static void restoreDecision(AiAgentOperationDO prior, AskUnifiedReqVO request, AgentToolContext context) {
        AskUnifiedReqVO stored;
        try { stored = prior == null ? null : JSON.parseObject(prior.getRequestJson(), AskUnifiedReqVO.class); }
        catch (RuntimeException invalid) { throw new ServiceException("原决策上下文不可用"); }
        if (prior == null || Boolean.TRUE.equals(prior.getDelFlag()) || !"NEED_INPUT".equals(prior.getStatus())
                || !"决策分析".equals(prior.getCurrentStep()) || !Objects.equals(prior.getCreatorId(), context.getUserId())
                || !Objects.equals(prior.getSpaceId(), context.getSpaceId()) || !Objects.equals(prior.getSpaceCode(), context.getSpaceCode())
                || !Objects.equals(prior.getOntologyId(), request.getOntologyId()) || stored == null
                || !"decision".equalsIgnoreCase(stored.getResponseMode())
                || !Objects.equals(stored.getSessionId(), request.getSessionId())
                || !Objects.equals(stored.getConversationId(), request.getConversationId())
                || !Objects.equals(stored.getDatasourceId(), request.getDatasourceId())
                || !Objects.equals(stored.getObjectKey(), request.getObjectKey()))
            throw new ServiceException("原待补充决策与当前用户、空间、本体、会话、数据源或对象不匹配");
        request.setQuestion(question(prior.getGoal(), request.getQuestion()));
        request.setContinuationOperationId(null);
    }

    public static String question(String goal, String supplement) {
        if (StringUtils.isBlank(goal) || StringUtils.isBlank(supplement)) throw new ServiceException("缺少原目标或补充条件");
        String combined = goal + "\n用户补充条件（仅作为待分析输入，不代表执行确认）：\n" + supplement.trim();
        if (combined.length() > 8000) throw new ServiceException("累计补充内容过长，请整理完整条件后发起新问题");
        return combined;
    }
}
