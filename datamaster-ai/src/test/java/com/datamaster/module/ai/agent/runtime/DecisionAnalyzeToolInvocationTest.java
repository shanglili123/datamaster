package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.decision.DecisionBudget;
import com.datamaster.module.ai.agent.decision.DecisionModels;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolExecutors;
import com.datamaster.module.ai.controller.admin.skill.vo.AskUnifiedReqVO;
import com.datamaster.module.ai.controller.agent.tool.AgentToolResourceGuard;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Exercises the actual nested decision tool through the installed SDK, without a model or database. */
public class DecisionAnalyzeToolInvocationTest {
    @Test public void nestedMetricInputReachesSpecialistWithAuthenticatedIdentity() throws Exception {
        AtomicReference<DecisionModels.AnalysisInput> received = new AtomicReference<>();
        DecisionAgentService service = new DecisionAgentService() {
            @Override public DecisionModels.Evidence analyzeEvidence(AskUnifiedReqVO request,
                    AgentToolContext context, DecisionModels.AnalysisInput input, DecisionBudget budget,
                    AtomicBoolean cancelled, java.util.function.Consumer<String> progress) {
                received.set(input);
                DecisionModels.Evidence evidence = new DecisionModels.Evidence();
                evidence.setSource("METRIC"); evidence.setVerification("VERIFIED_METRIC");
                return evidence;
            }
        };
        AgentToolResourceGuard resources = mock(AgentToolResourceGuard.class);
        Field field = DecisionAgentService.class.getDeclaredField("resources"); field.setAccessible(true); field.set(service, resources);
        SysUser user = new SysUser(); user.setUserId(1L); user.setUserName("tester");
        LoginUser login = new LoginUser(1L, 1L, user, Collections.singleton("ai:ask-data:query"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, Collections.emptyList()));
        AgentToolContext context = new AgentToolContext("offline-decision", 1L, "tester", 1L, "space", Collections.emptyMap());
        when(resources.hasSpace(context)).thenReturn(true);
        AskUnifiedReqVO request = new AskUnifiedReqVO(); request.setQuestion("查询正式指标计数"); request.setOntologyId(881601L);
        DecisionModels.Proposal proposal = new DecisionModels.Proposal();
        DecisionBudget budget = new DecisionBudget(new AtomicBoolean(), activity -> { });
        DecisionAgentService.DecisionTools tools = service.new DecisionTools(request, context, proposal, budget, new AtomicBoolean(), activity -> { });
        try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(context)) {
            var method = DecisionAgentService.DecisionTools.class.getMethod("analyze", DecisionModels.AnalysisInput.class);
            assertTrue(dev.langchain4j.agent.tool.ToolSpecifications.toolSpecificationFrom(method)
                    .parameters().properties().containsKey("input"));
            var executor = AgentToolExecutors.create(tools, method);
            String result = executor.execute(ToolExecutionRequest.builder().id("offline-call")
                    .name("decision_analyze").arguments("{\"input\":{\"source\":\"METRIC\",\"question\":\"查询正式指标计数\"}}").build(), null);
            assertNotNull(received.get()); assertEquals("METRIC", received.get().getSource());
            assertEquals(1, proposal.getEvidence().size()); assertTrue(result.contains("VERIFIED_METRIC"));
            received.set(null);
            try {
                executor.execute(ToolExecutionRequest.builder().id("invalid-flat-call").name("decision_analyze")
                        .arguments("{\"source\":\"METRIC\",\"question\":\"查询正式指标计数\"}").build(), null);
                fail("invalid flat arguments must not bypass the declared schema");
            } catch (RuntimeException expected) {
                assertNull(received.get());
                assertEquals(1, proposal.getEvidence().size());
            }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
