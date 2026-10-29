package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.analysis.MetricSaveConfirmation;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/** Explicit namespace and immutable owner/space/session/ontology binding for metric saves. */
@Component
public class AskMetricOperationStore {
    public static final String PREFIX = "ASK_METRIC_";
    public static final String STEP = "指标问数";
    @Resource private IAiAgentOperationService operationService;

    public AiAgentOperationDO require(AskUnifiedReqVO request, Long userId) {
        String id = request.getOperationId();
        if (id == null || !id.matches("ASK_METRIC_[0-9a-fA-F-]{36}")) throw denied();
        AiAgentOperationDO operation = operationService.get(id);
        if (operation == null || Boolean.TRUE.equals(operation.getDelFlag())
                || !STEP.equals(operation.getCurrentStep())
                || !Objects.equals(userId, operation.getCreatorId())
                || !Objects.equals(request.getSpaceId(), operation.getSpaceId())
                || !Objects.equals(request.getSpaceCode(), operation.getSpaceCode())
                || !Objects.equals(request.getOntologyId(), operation.getOntologyId())) throw denied();
        AskUnifiedReqVO stored;
        try {
            stored = JSON.parseObject(operation.getRequestJson(), AskUnifiedReqVO.class);
        } catch (Exception ex) { throw denied(); }
        if (stored == null || !Objects.equals(request.getSessionId(), stored.getSessionId())
                || !Objects.equals(request.getOntologyId(), stored.getOntologyId())
                || !Objects.equals(request.getConversationId(), stored.getConversationId())) throw denied();
        return operation;
    }

    public AiAgentOperationDO create(AskUnifiedReqVO request, AgentToolContext context) {
        AiAgentOperationDO operation = new AiAgentOperationDO();
        operation.setId(PREFIX + UUID.randomUUID());
        request.setOperationId(operation.getId());
        operation.setGoal(request.getQuestion());
        operation.setCreatorId(context.getUserId()); operation.setCreateBy(context.getUserName());
        operation.setSpaceId(context.getSpaceId()); operation.setSpaceCode(context.getSpaceCode());
        operation.setOntologyId(request.getOntologyId());
        operation.setRequestJson(JSON.toJSONString(request));
        operation.setStatus(AgentToolResult.RUNNING); operation.setCurrentStep(STEP);
        operation.setCreateTime(new Date()); operation.setUpdateTime(new Date()); operation.setDelFlag(false);
        operationService.create(operation);
        return operation;
    }

    public AgentToolResult pending(AiAgentOperationDO operation) {
        return operation == null ? null : MetricSaveConfirmation.pending(operation.getStatus(), operation.getResultJson());
    }

    public AgentRunResponse saved(AiAgentOperationDO operation) {
        return JSON.parseObject(operation.getResultJson(), AgentRunResponse.class);
    }

    /** Never mark RUNNING before confirmation: SaveMetricAnalysisTool reads NEED_CONFIRMATION. */
    public void finish(AiAgentOperationDO operation, AgentRunResponse response, AgentToolContext context) {
        AiAgentOperationDO update = new AiAgentOperationDO();
        update.setId(operation.getId()); update.setStatus(response.getStatus());
        update.setResultJson(JSON.toJSONString(response)); update.setMessage(response.getMessage());
        update.setCurrentStep(STEP); update.setUpdaterId(context.getUserId());
        update.setUpdateBy(context.getUserName()); update.setUpdateTime(new Date());
        operationService.update(update);
    }

    private ServiceException denied() { return new ServiceException("指标问数操作不存在或与当前用户、空间、会话、本体不匹配"); }
}
