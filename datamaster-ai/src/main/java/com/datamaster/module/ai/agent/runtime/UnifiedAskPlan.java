package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;

import java.util.Map;
import java.util.Set;

/** A model-selected delivery contract, not a mapping from intent to tool names. */
public record UnifiedAskPlan(Set<Delivery> deliveries, boolean verifiedQuery) {
    public enum Delivery { ANSWER, REPORT, ADVICE, PROPOSAL }

    public UnifiedAskPlan {
        deliveries = Set.copyOf(deliveries);
        if (deliveries.isEmpty()) throw new IllegalArgumentException("Missing delivery");
        verifiedQuery |= deliveries.contains(Delivery.PROPOSAL);
    }

    boolean permitsNativeAnswer() {
        return !verifiedQuery && !deliveries.contains(Delivery.PROPOSAL) && !deliveries.contains(Delivery.REPORT);
    }

    boolean permitsNativeReport() {
        return !verifiedQuery && deliveries.equals(Set.of(Delivery.REPORT));
    }

    boolean acceptsNativeDelivery(java.util.List<AgentToolResult> results, com.datamaster.module.ai.agent.tool.AgentToolContext context) {
        if (!permitsNativeAnswer() && !permitsNativeReport()) return false;
        String kind = permitsNativeReport() ? "REPORT" : "NATIVE_REPLY";
        return results.stream().anyMatch(result -> UnifiedAskAgentService.isCurrent(result, context)
                && AgentToolResult.SUCCEEDED.equals(result.getStatus())
                && UnifiedAskAgentService.isUnverifiedExploration(result) && hasArtifact(result, kind));
    }

    boolean hasRequiredArtifacts(java.util.List<AgentToolResult> results, com.datamaster.module.ai.agent.tool.AgentToolContext context) {
        return missingArtifacts(results, context).isEmpty();
    }

    java.util.List<String> missingArtifacts(java.util.List<AgentToolResult> results,
                                             com.datamaster.module.ai.agent.tool.AgentToolContext context) {
        var current = results.stream().filter(result -> UnifiedAskAgentService.isCurrent(result, context)
                && AgentToolResult.SUCCEEDED.equals(result.getStatus())
                && !UnifiedAskAgentService.isUnverifiedExploration(result)).toList();
        var missing = new java.util.ArrayList<String>();
        if (verifiedQuery && current.stream().noneMatch(UnifiedAskAgentService::isMetricEvidence)) missing.add("已执行的受控查询");
        if (deliveries.contains(Delivery.REPORT) && current.stream().noneMatch(result -> hasArtifact(result, "REPORT"))) missing.add("报告产物或报告组织要求");
        if (deliveries.contains(Delivery.PROPOSAL) && current.stream().noneMatch(UnifiedAskPlan::hasUsableProposal)) missing.add("可用的候选提案");
        if (!UnifiedAskAgentService.hasAnswerEvidence(current, context)) missing.add("可回答原问题的证据");
        return missing;
    }

    private static boolean hasUsableProposal(AgentToolResult result) {
        return hasArtifact(result, "PROPOSAL") && result.getData() instanceof Map<?, ?> data
                && "PROPOSED".equals(data.get("status"))
                && data.get("candidates") instanceof java.util.List<?> candidates && !candidates.isEmpty();
    }

    private static boolean hasArtifact(AgentToolResult result, String kind) {
        return result.getData() instanceof Map<?, ?> data && kind.equals(data.get("artifactKind"));
    }
}
