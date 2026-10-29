package com.datamaster.module.ontology.api;

/** Model completion capability supplied by the AI module at runtime. */
public interface IOntologyModelGateway {

    String complete(String systemPrompt, String userPrompt);

    boolean available();

    default com.datamaster.module.ontology.api.dto.OntologyModelOutput generateOntology(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("当前模型网关不支持类型化本体输出");
    }

    default com.datamaster.module.ontology.api.dto.ActionGenerationOutput generateActions(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("当前模型网关不支持类型化动作输出");
    }

    default com.datamaster.module.ontology.api.dto.ActionDecisionOutput decideAction(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("当前模型网关不支持类型化动作决策");
    }
}
