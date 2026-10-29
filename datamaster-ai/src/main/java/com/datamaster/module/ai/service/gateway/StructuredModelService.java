package com.datamaster.module.ai.service.gateway;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.service.skill.dto.ReportTemplateOutput;
import com.datamaster.module.ai.service.skill.dto.SkillEnhancementOutput;
import com.datamaster.module.ontology.api.dto.*;
import com.datamaster.module.collector.api.etl.ICollectorOpsModelGateway;
import com.datamaster.module.collector.api.etl.dto.CollectorOpsAnalysisOutput;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** Each task has a concrete return type; no raw completion JSON extraction fallback. */
@Service
public class StructuredModelService implements ICollectorOpsModelGateway {
    @Resource private GovernedChatModelFactory models;

    public ReportTemplateOutput report(String system, String user) {
        return AiServices.builder(ReportAssistant.class).chatLanguageModel(model("REPORT_TEMPLATE")).build().generate(system, user);
    }
    public SkillEnhancementOutput skill(String system, String user) {
        return AiServices.builder(SkillAssistant.class).chatLanguageModel(model("SKILL_ENHANCEMENT")).build().generate(system, user);
    }
    public OntologyModelOutput ontology(String system, String user) {
        return AiServices.builder(OntologyAssistant.class).chatLanguageModel(model("ONTOLOGY_GENERATE")).build().generate(system, user);
    }
    public ActionGenerationOutput actions(String system, String user) {
        return AiServices.builder(ActionsAssistant.class).chatLanguageModel(model("ACTION_GENERATE")).build().generate(system, user);
    }
    public ActionDecisionOutput decision(String system, String user) {
        return AiServices.builder(DecisionAssistant.class).chatLanguageModel(model("ACTION_DECISION")).build().generate(system, user);
    }
    @Override
    public CollectorOpsAnalysisOutput analyzeFailure(String system, String user, Long spaceId, String spaceCode) {
        AiInvocationGateway.Context current = AiInvocationGateway.snapshotContext();
        // These scope identifiers come from the collector's persisted task, never from model output.
        try (AiInvocationContext.Scope ignored = AiInvocationContext.scope(new AiInvocationGateway.Context(
                current.getUserId(), spaceId, spaceCode, current.getOperationId()))) {
            if (!models.available()) return null;
            return AiServices.builder(OpsAssistant.class).chatLanguageModel(model("ETL_OPS_ANALYSIS")).build().generate(system, user);
        }
    }
    private ChatLanguageModel model(String scene) {
        ChatLanguageModel model = models.create(scene);
        if (model == null) throw new ServiceException("AI模型未启用或未配置API地址");
        return model;
    }

    public interface ReportAssistant {
        @SystemMessage("{{system}}") ReportTemplateOutput generate(@V("system") String system, @UserMessage String user);
    }
    public interface SkillAssistant {
        @SystemMessage("{{system}}") SkillEnhancementOutput generate(@V("system") String system, @UserMessage String user);
    }
    public interface OntologyAssistant {
        @SystemMessage("{{system}}") OntologyModelOutput generate(@V("system") String system, @UserMessage String user);
    }
    public interface ActionsAssistant {
        @SystemMessage("{{system}}") ActionGenerationOutput generate(@V("system") String system, @UserMessage String user);
    }
    public interface DecisionAssistant {
        @SystemMessage("{{system}}") ActionDecisionOutput generate(@V("system") String system, @UserMessage String user);
    }
    public interface OpsAssistant {
        @SystemMessage("{{system}}") CollectorOpsAnalysisOutput generate(@V("system") String system, @UserMessage String user);
    }
}
