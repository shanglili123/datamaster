package com.datamaster.module.ontology.api;

import com.datamaster.module.ontology.api.dto.OntologyDecisionContextDTO;

/** 本体决策上下文只读接口，供 AI Skill 生成使用。 */
public interface IOntologyDecisionApiService {

    OntologyDecisionContextDTO getDecisionContext(Long ontologyId);
}
