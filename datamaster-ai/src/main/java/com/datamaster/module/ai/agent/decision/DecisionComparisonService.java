package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/** Ranking only compares a finite, explicitly scoped set of computed alternatives. */
@Service
public class DecisionComparisonService {
    public Comparison compare(Proposal proposal, CalculationContext context) {
        Comparison comparison = new Comparison(); comparison.setStrategy(context.getStrategy());
        List<Candidate> eligible = new ArrayList<>();
        for (Candidate candidate : proposal.getCandidates()) {
            CalculationResult result = candidate.getCalculation();
            boolean failed = candidate.getConstraints() != null && candidate.getConstraints().stream()
                    .anyMatch(item -> item != null && "FAIL".equals(item.getStatus()));
            failed |= candidate.getEvaluation() != null && "FAIL".equals(candidate.getEvaluation().getStatus());
            if (result != null) { result.setRank(null); result.setRecommended(false); }
            if (result == null || !"COMPUTED".equals(result.getStatus()) || failed
                    || !("PASS".equals(result.getBudgetStatus()) || "NOT_REQUIRED".equals(result.getBudgetStatus()))
                    || ("MEET_DEMAND_MIN_COST".equals(context.getStrategy()) && number(result.getRemainingGap()).signum() != 0)) {
                comparison.getExcludedIds().add(candidate.getId()); continue;
            }
            eligible.add(candidate);
        }
        Comparator<Candidate> order = Comparator.<Candidate, BigDecimal>comparing(c -> number(c.getCalculation().getTotalCost()));
        if ("MIN_GAP_THEN_COST".equals(context.getStrategy()))
            order = Comparator.<Candidate, BigDecimal>comparing(c -> number(c.getCalculation().getRemainingGap())).thenComparing(order);
        eligible.sort(order);
        int rank = 0;
        Candidate previous = null;
        for (int i = 0; i < eligible.size(); i++) {
            Candidate candidate = eligible.get(i);
            if (previous == null || order.compare(previous, candidate) != 0) rank = i + 1;
            candidate.getCalculation().setRank(rank);
            candidate.getCalculation().setRecommended(rank == 1);
            if (rank == 1) comparison.getRecommendedIds().add(candidate.getId());
            previous = candidate;
        }
        comparison.setStatus(eligible.isEmpty() ? "NO_FEASIBLE_CANDIDATE"
                : comparison.getExcludedIds().isEmpty() ? "COMPARABLE" : "PARTIAL");
        comparison.setSummary(eligible.isEmpty() ? "没有满足当前比较条件的已测算候选，不推荐方案"
                : "仅在当前已测算且满足预算/目标门槛的候选中进行条件性排序；相同结果并列，不代表全局最优");
        comparison.getLimitations().add("未测算或未满足门槛的候选不参与排名；业务约束未知不等于通过");
        comparison.getLimitations().add("MEET_DEMAND_MIN_COST按满足需求后的增量成本排序；MIN_GAP_THEN_COST先比较缺口再比较成本");
        return comparison;
    }

    private BigDecimal number(String value) { return new BigDecimal(value); }
}
