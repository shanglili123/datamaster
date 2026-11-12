package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import org.junit.Test;

import java.lang.reflect.*;
import static org.junit.Assert.*;

public class AskInputContinuationTest {
    @Test public void combinesOnlyStoredGoalAndSupplementWithoutConfirming() {
        String text = AskInputContinuation.question("分析支付金额趋势", "2026年10月，Asia/Shanghai");
        assertTrue(text.startsWith("分析支付金额趋势")); assertTrue(text.contains("2026年10月"));
        assertTrue(text.contains("不代表执行确认"));
    }

    @Test public void capsAccumulatedInputAndRejectsEmptySupplements() {
        reject(() -> AskInputContinuation.question("原目标", " "));
        reject(() -> AskInputContinuation.question("x".repeat(7990), "y".repeat(30)));
    }

    @Test public void metricSupplementRestoresGoalButNoSaveConfirmationOrResults() throws Exception {
        AskUnifiedReqVO request = request(); AiAgentOperationDO prior = prior();
        store(prior).restoreInput(request, 7L);
        assertTrue(request.getQuestion().startsWith("分析支付金额趋势"));
        assertTrue(request.getQuestion().contains("2026-10-01至2026-11-01"));
        assertNull(request.getOperationId()); assertNull(request.getContinuationOperationId());
        assertFalse(Boolean.TRUE.equals(request.getConfirmed()));
    }

    @Test public void rejectsOtherOwnerSpaceSessionOntologyOrNonPendingOperation() throws Exception {
        for (String field : new String[]{"owner", "space", "code", "session", "ontology", "status", "deleted", "datasource"}) {
            AskUnifiedReqVO request = request(); AiAgentOperationDO prior = prior();
            if ("owner".equals(field)) prior.setCreatorId(99L);
            if ("space".equals(field)) prior.setSpaceId(99L);
            if ("code".equals(field)) prior.setSpaceCode("other");
            if ("ontology".equals(field)) prior.setOntologyId(99L);
            if ("session".equals(field)) request.setSessionId(99L);
            if ("status".equals(field)) prior.setStatus(AgentToolResult.NEED_CONFIRMATION);
            if ("deleted".equals(field)) prior.setDelFlag(true);
            if ("datasource".equals(field)) request.setDatasourceId(99L);
            AskMetricOperationStore store = store(prior);
            reject(() -> store.restoreInput(request, 7L)); assertNull(request.getOperationId());
        }
    }

    @Test public void supplementCannotBeCombinedWithConfirmation() throws Exception {
        AskUnifiedReqVO request = request(); request.setConfirmed(true); AskMetricOperationStore store = store(prior());
        reject(() -> store.restoreInput(request, 7L));
    }

    @Test public void decisionSupplementKeepsObjectAndRejectsChangedContext() {
        AskUnifiedReqVO request = request(); request.setResponseMode("decision"); request.setObjectKey("ORD-1");
        AiAgentOperationDO prior = prior(); prior.setCurrentStep("决策分析");
        AskUnifiedReqVO stored = request(); stored.setResponseMode("decision"); stored.setObjectKey("ORD-1");
        prior.setRequestJson(JSON.toJSONString(stored));
        com.datamaster.module.ai.agent.tool.AgentToolContext context = new com.datamaster.module.ai.agent.tool.AgentToolContext(
                null, 7L, "owner", 10L, "space", java.util.Collections.emptyMap());
        AskInputContinuation.restoreDecision(prior, request, context);
        assertTrue(request.getQuestion().startsWith(prior.getGoal())); assertNull(request.getContinuationOperationId());
        request.setObjectKey("ORD-2");
        reject(() -> AskInputContinuation.restoreDecision(prior, request, context));
        request.setObjectKey("ORD-1"); prior.setStatus("PROPOSED");
        reject(() -> AskInputContinuation.restoreDecision(prior, request, context));
    }

    @Test public void completedFormalQueryCanSupplyGoalForNewDecisionButNotExecution() throws Exception {
        AskUnifiedReqVO request = request(); request.setContinuationOperationId(null);
        request.setAnalysisOperationId("ASK_METRIC_00000000-0000-0000-0000-000000000001");
        AiAgentOperationDO prior = prior(); prior.setStatus(AgentToolResult.SUCCEEDED);
        AgentRunResponse result = new AgentRunResponse();
        AgentToolResult query = AgentToolResult.of(AgentToolResult.SUCCEEDED, null, "真实查询", prior.getId()); query.setToolName("execute_metric_analysis");
        result.setToolResults(java.util.Collections.singletonList(query)); prior.setResultJson(JSON.toJSONString(result));
        store(prior).restoreDecisionGoal(request, 7L);
        assertTrue(request.getQuestion().startsWith(prior.getGoal())); assertNull(request.getAnalysisOperationId()); assertNull(request.getOperationId());
    }

    private static AskUnifiedReqVO request() {
        AskUnifiedReqVO req = new AskUnifiedReqVO(); req.setContinuationOperationId("ASK_METRIC_00000000-0000-0000-0000-000000000001");
        req.setQuestion("2026-10-01至2026-11-01，Asia/Shanghai"); req.setSessionId(8L); req.setConversationId("8");
        req.setOntologyId(9L); req.setSpaceId(10L); req.setSpaceCode("space"); return req;
    }
    private static AiAgentOperationDO prior() {
        AskUnifiedReqVO stored = request(); stored.setQuestion("分析支付金额趋势"); stored.setContinuationOperationId(null);
        AiAgentOperationDO prior = new AiAgentOperationDO(); prior.setId(request().getContinuationOperationId());
        prior.setCurrentStep(AskMetricOperationStore.STEP); prior.setStatus(AgentToolResult.NEED_INPUT); prior.setCreatorId(7L);
        prior.setSpaceId(10L); prior.setSpaceCode("space"); prior.setOntologyId(9L); prior.setGoal(stored.getQuestion());
        prior.setRequestJson(JSON.toJSONString(stored)); prior.setDelFlag(false); return prior;
    }
    private static AskMetricOperationStore store(AiAgentOperationDO prior) throws Exception {
        AskMetricOperationStore store = new AskMetricOperationStore();
        Object service = Proxy.newProxyInstance(IAiAgentOperationService.class.getClassLoader(), new Class<?>[]{IAiAgentOperationService.class},
                (proxy, method, args) -> { assertEquals("get", method.getName()); return prior; });
        Field field = AskMetricOperationStore.class.getDeclaredField("operationService"); field.setAccessible(true); field.set(store, service); return store;
    }
    private static void reject(Runnable action) {
        try { action.run(); fail("Must reject invalid continuation"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
}
