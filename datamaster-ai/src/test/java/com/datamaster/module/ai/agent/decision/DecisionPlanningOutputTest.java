package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.runtime.DecisionAgentService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class DecisionPlanningOutputTest {
    @Test
    public void aiServiceReturnsTypedPlanningResultWithoutManualJsonExtraction() {
        DecisionAgentService.PlanningAssistant planner = AiServices.builder(DecisionAgentService.PlanningAssistant.class)
                .chatLanguageModel(model("{\"candidates\":[{\"name\":\"调拨\",\"rationale\":\"补齐缺口\","
                        + "\"evidenceIds\":[\"E1\"],\"constraints\":[{\"name\":\"库存\",\"status\":\"UNKNOWN\"}],"
                        + "\"suggestedBinding\":{\"type\":\"ACTION\",\"targetId\":\"9007199254740993\",\"inputParams\":\"{}\"}}],\"missingInformation\":[]}"))
                .build();
        DecisionPlanningOutput.Result output = planner.plan("测试方案");
        assertEquals("调拨", output.getCandidates().get(0).getName());
        DecisionModels.Candidate candidate = DecisionPlanningOutput.toCandidate(output.getCandidates().get(0));
        assertEquals("9007199254740993", candidate.getSuggestedBinding().getTargetId());
        assertEquals("UNKNOWN", candidate.getConstraints().get(0).getStatus());
        assertNull(candidate.getId()); assertNull(candidate.getCalculation());
        assertNull(candidate.getEvaluation()); assertNull(candidate.getSuggestedBinding().getVersion());
    }

    @Test
    public void missingInformationHasItsOwnTypedField() {
        DecisionAgentService.PlanningAssistant planner = AiServices.builder(DecisionAgentService.PlanningAssistant.class)
                .chatLanguageModel(model("{\"candidates\":[],\"missingInformation\":[\"请提供交期\"]}"))
                .build();
        assertEquals(Collections.singletonList("请提供交期"), planner.plan("测试").getMissingInformation());
    }

    @Test
    public void unspecifiedConstraintCannotBecomePassAndImpactHasNoComputedValue() {
        DecisionPlanningOutput.CandidateDraft draft = new DecisionPlanningOutput.CandidateDraft();
        DecisionPlanningOutput.ConstraintDraft constraint = new DecisionPlanningOutput.ConstraintDraft();
        constraint.setName("产能"); draft.getConstraints().add(constraint);
        DecisionPlanningOutput.ImpactDraft impact = new DecisionPlanningOutput.ImpactDraft();
        impact.setName("成本"); impact.setUnit("CNY"); draft.getImpacts().add(impact);
        DecisionModels.Candidate candidate = DecisionPlanningOutput.toCandidate(draft);
        assertEquals("UNKNOWN", candidate.getConstraints().get(0).getStatus());
        assertNull(candidate.getImpacts().get(0).getValue());
        assertNull(candidate.getImpacts().get(0).getMethod());
    }

    @Test(expected = RuntimeException.class)
    public void invalidOutputFailsRatherThanGuessingAnArraySubstring() {
        AiServices.builder(DecisionAgentService.PlanningAssistant.class)
                .chatLanguageModel(model("not valid structured output"))
                .build().plan("测试");
    }

    private ChatLanguageModel model(final String json) {
        return new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                return Response.from(AiMessage.from(json));
            }
        };
    }
}
