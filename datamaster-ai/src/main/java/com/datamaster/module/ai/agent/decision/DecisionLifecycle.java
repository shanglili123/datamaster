package com.datamaster.module.ai.agent.decision;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import java.util.*;

/** Human task dispositions never create or cancel an execution. */
final class DecisionLifecycle {
    private DecisionLifecycle() { }
    static Proposal apply(Proposal source, DispositionRequest request, Date now) {
        if (source.getExecutionId() != null || source.getRunId() != null || source.getExecutionStatus() != null)
            throw new ServiceException("已提交任务请在原执行流程处理，不能改成未提交决策");
        String reason = request.getReason();
        if (reason == null || reason.trim().isEmpty() || reason.length() > 1000) throw new ServiceException("请填写处置原因（最多1000字符）");
        String action = request.getDisposition();
        boolean reopen = "REOPEN".equals(action);
        if (reopen ? !Arrays.asList("DEFERRED", "ABANDONED", "ACCEPTED_NO_ACTION").contains(source.getStatus())
                : !Arrays.asList("PROPOSED", "PREVIEW_READY", "NEED_INPUT", "DEFERRED").contains(source.getStatus()))
            throw new ServiceException("当前任务状态不支持此处置");
        if (("DEFER".equals(action) || "ACCEPT_NO_ACTION".equals(action)) && request.getReviewAfter() != null && !request.getReviewAfter().after(now))
            throw new ServiceException("约定复查时间必须在未来");
        Proposal next = JSON.parseObject(JSON.toJSONString(source), Proposal.class);
        if ("ACCEPT_NO_ACTION".equals(action)) {
            Candidate baseline = next.getCandidates().stream().filter(Candidate::isBaseline).findFirst()
                    .orElseThrow(() -> new ServiceException("当前任务没有不干预基线"));
            next.setSelectedCandidateId(baseline.getId()); next.setStatus("ACCEPTED_NO_ACTION");
        } else if ("DEFER".equals(action)) {
            if (request.getReviewAfter() == null || !request.getReviewAfter().after(now)) throw new ServiceException("暂缓需要未来的复查时间");
            next.setStatus("DEFERRED"); next.setSelectedCandidateId(null);
        } else if ("ABANDON".equals(action)) { next.setStatus("ABANDONED"); next.setSelectedCandidateId(null); }
        else if (reopen) { next.setStatus(next.getCandidates().isEmpty() ? "NEED_INPUT" : "PROPOSED"); next.setSelectedCandidateId(null); }
        else throw new ServiceException("不支持的决策处置");
        next.setRevision(source.getRevision() + 1); next.setDispositionReason(reason.trim()); next.setDecidedAt(now);
        next.setReviewAfter("DEFER".equals(action) || "ACCEPT_NO_ACTION".equals(action) ? request.getReviewAfter() : null);
        next.setBinding(null); next.setPreviewDigest(null); next.setDefinitionDigest(null); next.setPreviewExpiresAt(null); next.setImpactPreview(null);
        next.setMessage(reopen ? "已重新打开任务；旧预览已失效，过时证据须重新分析" : "已记录人工处置；未提交、取消或执行任何业务动作");
        return next;
    }
}
