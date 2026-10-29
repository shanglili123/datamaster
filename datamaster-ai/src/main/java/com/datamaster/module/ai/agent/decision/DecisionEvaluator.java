package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.decision.DecisionModels.Candidate;
import com.datamaster.module.ai.agent.decision.DecisionModels.Evidence;

import java.util.List;

/** Domain implementations must compute from authorized server evidence, not model-supplied outcomes. */
public interface DecisionEvaluator {
    boolean supports(Candidate candidate);
    void evaluate(Candidate candidate, List<Evidence> evidence);
}
