package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.springframework.stereotype.Service;

import java.util.*;

/** Model suggestions never become computed outcomes merely by passing through another model. */
@Service
public class DecisionEvaluationService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<DecisionEvaluator> evaluators = Collections.emptyList();

    public void evaluate(Candidate candidate, List<Evidence> evidence) {
        Set<String> ids = new HashSet<>();
        for (Evidence item : evidence) ids.add(item.getId());
        candidate.getEvidenceIds().removeIf(id -> !ids.contains(id));
        Evaluation evaluation = new Evaluation();
        boolean failed = false;
        for (Constraint constraint : candidate.getConstraints()) {
            // A model may flag a concern, but cannot certify a hard constraint as satisfied.
            if ("FAIL".equalsIgnoreCase(constraint.getStatus())) failed = true;
            else constraint.setStatus("UNKNOWN");
            evaluation.getUnknowns().add(constraint.getName() + "：需由业务规则或领域评估器核验");
        }
        for (Impact impact : candidate.getImpacts()) {
            impact.setMethod("unverified");
            impact.setValue(null);
            if (!ids.contains(impact.getEvidenceId())) impact.setEvidenceId(null);
        }
        evaluation.setStatus(failed ? "FAIL" : "UNKNOWN");
        evaluation.setSummary(candidate.isBaseline() ? "不干预基线；未来业务影响尚未测算"
                : failed ? "存在未满足的约束，不可准备执行" : "仅定性方案；未配置领域收益/可行性评估器，不代表最优方案");
        if (!candidate.isBaseline()) evaluation.getUnknowns().add("成本、收益及资源约束未完成确定性测算，执行前需人工核实");
        candidate.setEvaluation(evaluation);
        if (!candidate.isBaseline()) for (DecisionEvaluator evaluator : evaluators) {
            if (evaluator.supports(candidate)) evaluator.evaluate(candidate, evidence);
        }
    }
}
