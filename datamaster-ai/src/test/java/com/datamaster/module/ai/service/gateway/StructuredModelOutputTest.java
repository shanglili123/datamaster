package com.datamaster.module.ai.service.gateway;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.service.skill.dto.*;
import com.datamaster.module.ontology.api.dto.*;
import com.datamaster.module.collector.api.etl.dto.CollectorOpsAnalysisOutput;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StructuredModelOutputTest {
    @Test
    public void parsesAllSixTaskReturnTypesWithoutProviderCalls() {
        ChatLanguageModel reportModel = model("{\"dataSchema\":{\"fields\":[{\"name\":\"orders\",\"type\":\"array\"}]},\"layout\":{\"sections\":[{\"type\":\"table\",\"bind\":\"orders\",\"columns\":[{\"prop\":\"id\",\"label\":\"编号\"}]}]},\"style\":{\"theme\":{}},\"prompt\":{\"instruction\":\"查询订单\"}}");
        ReportTemplateOutput report = AiServices.builder(StructuredModelService.ReportAssistant.class).chatLanguageModel(reportModel).build().generate("system", "user");
        assertEquals("orders", report.getDataSchema().getFields().get(0).getName());
        assertTrue(JSON.toJSONString(report.getLayout().getSections().get(0).getColumns()).contains("编号"));

        SkillEnhancementOutput skill = AiServices.builder(StructuredModelService.SkillAssistant.class)
                .chatLanguageModel(model("{\"businessSummary\":\"订单\",\"fieldUsages\":[{\"field\":\"amount\",\"usage\":\"金额\"}],\"sqlRules\":[\"只读\"]}"))
                .build().generate("system", "user");
        assertEquals("amount", skill.getFieldUsages().get(0).getField());

        OntologyModelOutput ontology = AiServices.builder(StructuredModelService.OntologyAssistant.class)
                .chatLanguageModel(model("{\"ontologyName\":\"订单\",\"concepts\":[{\"code\":\"order\",\"tableName\":\"orders\",\"properties\":[{\"code\":\"id\",\"isPrimary\":true}]}],\"relations\":[]}"))
                .build().generate("system", "user");
        assertEquals(Boolean.TRUE, ontology.getConcepts().get(0).getProperties().get(0).getIsPrimary());
        assertTrue(JSON.toJSONString(ontology).contains("\"isPrimary\":true"));

        ActionGenerationOutput actions = AiServices.builder(StructuredModelService.ActionsAssistant.class)
                .chatLanguageModel(model("{\"actions\":[{\"actionName\":\"修改\",\"actionType\":\"COMPOSITE\",\"executionSteps\":[{\"conceptCode\":\"order\",\"actionType\":\"UPDATE\",\"paramConfig\":[{\"propertyCode\":\"status\",\"valueMode\":\"placeholder\",\"valueTemplate\":\"${status}\"}]}]}]}"))
                .build().generate("system", "user");
        assertEquals("${status}", actions.getActions().get(0).getExecutionSteps().get(0).getParamConfig().get(0).getValueTemplate());

        ActionDecisionOutput decision = AiServices.builder(StructuredModelService.DecisionAssistant.class)
                .chatLanguageModel(model("{\"decision\":\"ASK_HUMAN\",\"actionId\":\"9007199254740993\",\"inputParams\":{\"quantity\":\"2\"},\"missingFields\":[\"objectKey\"]}"))
                .build().generate("system", "user");
        assertEquals("9007199254740993", decision.getActionId());
        assertEquals(ActionDecisionOutput.Decision.ASK_HUMAN, decision.getDecision());
        assertEquals("2", decision.getInputParams().get("quantity"));

        CollectorOpsAnalysisOutput ops = AiServices.builder(StructuredModelService.OpsAssistant.class)
                .chatLanguageModel(model("{\"failureType\":\"PERMISSION\",\"riskLevel\":\"HIGH\",\"recoverable\":false}"))
                .build().generate("system", "user");
        assertEquals(CollectorOpsAnalysisOutput.RiskLevel.HIGH, ops.getRiskLevel());
        assertEquals(Boolean.FALSE, ops.getRecoverable());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsUnknownDecisionEnum() {
        AiServices.builder(StructuredModelService.DecisionAssistant.class)
                .chatLanguageModel(model("{\"decision\":\"EXECUTE_NOW\"}"))
                .build().generate("system", "user");
    }

    @Test(expected = RuntimeException.class)
    public void rejectsNonJsonInsteadOfManuallyExtractingBraces() {
        AiServices.builder(StructuredModelService.SkillAssistant.class)
                .chatLanguageModel(model("not a structured response"))
                .build().generate("system", "user");
    }

    private ChatLanguageModel model(final String text) {
        return new ChatLanguageModel() {
            @Override public Response<AiMessage> generate(List<ChatMessage> messages) {
                assertTrue(messages.size() >= 2);
                return Response.from(AiMessage.from(text));
            }
        };
    }
}
