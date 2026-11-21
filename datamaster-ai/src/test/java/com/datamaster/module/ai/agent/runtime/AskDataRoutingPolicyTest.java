package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AskDataRoutingPolicyTest {
    @Test public void ordinaryQuestionCanExploreOnlyAfterCompleteEmptyCatalogue() {
        AskUnifiedReqVO request = new AskUnifiedReqVO(); request.setQuestion("销售记录有多少条");
        AgentRunResponse response = emptyCatalogue();
        assertTrue(AskDataRoutingPolicy.mayExplore(request,response));
        request.setQuestion("按正式指标口径统计销售记录");
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
        request.setQuestion("销售记录有多少条"); request.setConfirmed(true);
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
    }

    @Test public void failureDeniedQueryAndPartialSearchNeverFallback() {
        AskUnifiedReqVO request = new AskUnifiedReqVO(); request.setQuestion("销售记录有多少条");
        AgentRunResponse response = emptyCatalogue(); response.setStatus(AgentToolResult.FAILED);
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
        response = emptyCatalogue(); response.getToolResults().get(0).setStatus(AgentToolResult.FAILED);
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
        response = emptyCatalogue(); response.getToolResults().add(AgentToolResult.of(AgentToolResult.FAILED,null,"权限拒绝","op"));
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
        response = emptyCatalogue(); ((Map<String,Object>) response.getToolResults().get(0).getData()).put("searchComplete",false);
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
    }

    @Test public void nonemptyCatalogueCanRouteWhenNoCandidateFitsButNeverAfterExecution() {
        AskUnifiedReqVO request = new AskUnifiedReqVO(); request.setQuestion("统计设备故障情况");
        AgentRunResponse response = emptyCatalogue();
        ((Map<String,Object>)response.getToolResults().get(0).getData()).put("total",5);
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
        AgentToolResult choice=AgentToolResult.of(AgentToolResult.NEED_INPUT,null,"候选口径不适用","op");
        choice.setToolName("use_data_qa");choice.getMetadata().put("routingChoice","NO_APPLICABLE_METRIC");
        response.getToolResults().add(choice);
        assertTrue(AskDataRoutingPolicy.mayExplore(request,response));
        AgentToolResult query=AgentToolResult.of(AgentToolResult.SUCCEEDED,null,"已执行","op");query.setToolName("execute_metric_analysis");
        response.getToolResults().add(1,query);
        assertFalse(AskDataRoutingPolicy.mayExplore(request,response));
    }

    private static AgentRunResponse emptyCatalogue() {
        AgentRunResponse response = new AgentRunResponse(); response.setStatus(AgentToolResult.NEED_INPUT); response.setOperationId("op");
        Map<String,Object> data = new LinkedHashMap<>(); data.put("catalogSearch",true);data.put("searchComplete",true);data.put("total",0);
        AgentToolResult result = AgentToolResult.of(AgentToolResult.SUCCEEDED,data,"无指标","op"); result.setToolName("search_metrics");
        response.getToolResults().add(result); return response;
    }
}
