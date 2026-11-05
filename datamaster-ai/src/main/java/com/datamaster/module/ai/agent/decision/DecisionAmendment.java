package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;

/** Human amendment data only. No model invocation or execution permission is encoded here. */
final class DecisionAmendment {
    private DecisionAmendment() { }

    static Binding binding(AmendmentRequest request) {
        if (request == null || !("ACTION".equals(request.getType()) || "WORKFLOW".equals(request.getType())))
            throw new ServiceException("请选择 ACTION 或 WORKFLOW");
        DecisionBindingResolver.positiveId(request.getTargetId());
        if (request.getObjectKey() != null && request.getObjectKey().length() > 2000)
            throw new ServiceException("业务对象主键过长");
        if (request.getInputParams() != null && request.getInputParams().length() > 20000)
            throw new ServiceException("执行参数过长");
        Binding binding = new Binding();
        binding.setType(request.getType()); binding.setTargetId(request.getTargetId());
        binding.setObjectKey(request.getObjectKey() == null ? null : request.getObjectKey().trim());
        binding.setInputParams(DecisionBindingResolver.canonical(DecisionBindingResolver.parseInput(request.getInputParams())));
        return binding;
    }

    static Proposal apply(Proposal original, String candidateId, Binding validated) {
        if (!java.util.Arrays.asList("PROPOSED", "PREVIEW_READY").contains(original.getStatus())
                || original.getExecutionId() != null || original.getRunId() != null || original.getExecutionStatus() != null)
            throw new ServiceException("已提交或未完成分析的提案不能补充绑定");
        Proposal next = JSON.parseObject(JSON.toJSONString(original), Proposal.class);
        Candidate selected = next.getCandidates().stream().filter(c -> c != null && candidateId != null
                && candidateId.equals(c.getId()) && !c.isBaseline()).findFirst()
                .orElseThrow(() -> new ServiceException("请选择当前提案的处置候选"));
        selected.setSuggestedBinding(JSON.parseObject(JSON.toJSONString(validated), Binding.class));
        // Changing an execution target/quantity invalidates the comparison as a whole, not only the old preview.
        for (Candidate candidate : next.getCandidates()) {
            candidate.setCalculation(null); candidate.setCalculationSpec(null);
        }
        next.setCalculationContext(null); next.setComparison(null);
        next.setBinding(null); next.setSelectedCandidateId(null); next.setPreviewDigest(null); next.setDefinitionDigest(null);
        next.setExecutionDetails(null); next.setResumeAllowed(false); next.setResumeToken(null); next.setResumeMessage(null);
        next.setRevision(original.getRevision() + 1); next.setStatus("PROPOSED");
        next.setMessage("执行绑定已人工补充并通过只读校验；旧预览和测算已失效，请重新测算、准备预览并明确确认；尚未提交执行");
        return next;
    }
}
