package com.datamaster.module.ai.agent.decision;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Model-owned draft fields only; persistence, calculation and approval fields are intentionally absent. */
public final class DecisionPlanningOutput {
    private DecisionPlanningOutput() { }

    @Data
    public static class Result {
        @Description("最多三个处置候选；信息不足时为空，不生成不干预基线")
        private List<CandidateDraft> candidates = new ArrayList<>();
        @Description("形成方案仍缺少的业务条件，信息不足时明确列出")
        private List<String> missingInformation = new ArrayList<>();
    }

    @Data
    public static class CandidateDraft {
        private String name;
        private String rationale;
        private List<String> evidenceIds = new ArrayList<>();
        private List<String> risks = new ArrayList<>();
        private List<ConstraintDraft> constraints = new ArrayList<>();
        private List<ImpactDraft> impacts = new ArrayList<>();
        private BindingDraft suggestedBinding;
    }

    @Data
    public static class ConstraintDraft {
        private String name;
        @Description("未核验或已发现不满足；模型不得证明业务约束通过")
        private ConstraintStatus status;
        private String reason;
    }

    public enum ConstraintStatus { UNKNOWN, FAIL }

    @Data
    public static class ImpactDraft {
        private String name;
        private String unit;
    }

    @Data
    public static class BindingDraft {
        private BindingType type;
        @Description("本轮planning_capabilities工具返回的已有目标ID，完整字符串，不猜测")
        private String targetId;
        @Description("参数JSON对象的字符串；不能包含SQL、脚本或执行指令")
        private String inputParams;
    }

    public enum BindingType { ACTION, WORKFLOW }

    /** Explicit mapping prevents draft output from writing server-owned result or approval fields. */
    public static DecisionModels.Candidate toCandidate(CandidateDraft draft) {
        DecisionModels.Candidate candidate = new DecisionModels.Candidate();
        candidate.setName(draft.getName()); candidate.setRationale(draft.getRationale());
        if (draft.getEvidenceIds() != null) candidate.setEvidenceIds(new ArrayList<>(draft.getEvidenceIds()));
        if (draft.getRisks() != null) candidate.setRisks(new ArrayList<>(draft.getRisks()));
        if (draft.getConstraints() != null) for (ConstraintDraft source : draft.getConstraints()) {
            if (source == null) continue;
            DecisionModels.Constraint constraint = new DecisionModels.Constraint();
            constraint.setName(source.getName()); constraint.setReason(source.getReason());
            constraint.setStatus(source.getStatus() == ConstraintStatus.FAIL ? "FAIL" : "UNKNOWN");
            candidate.getConstraints().add(constraint);
        }
        if (draft.getImpacts() != null) for (ImpactDraft source : draft.getImpacts()) {
            if (source == null) continue;
            DecisionModels.Impact impact = new DecisionModels.Impact();
            impact.setName(source.getName()); impact.setUnit(source.getUnit());
            candidate.getImpacts().add(impact);
        }
        BindingDraft source = draft.getSuggestedBinding();
        if (source != null && source.getType() != null) {
            DecisionModels.Binding binding = new DecisionModels.Binding();
            binding.setType(source.getType().name()); binding.setTargetId(source.getTargetId()); binding.setInputParams(source.getInputParams());
            candidate.setSuggestedBinding(binding);
        }
        return candidate;
    }
}
