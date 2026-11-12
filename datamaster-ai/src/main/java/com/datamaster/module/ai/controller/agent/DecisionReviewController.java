package com.datamaster.module.ai.controller.agent;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ai.agent.decision.DecisionModels.Proposal;
import com.datamaster.module.ai.agent.decision.DecisionProposalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/** Replays only frozen server-owned metric plans; no model or business writes. */
@RestController
@RequestMapping("/ai/decision")
public class DecisionReviewController {
    @Resource private DecisionProposalService proposals;

    @PostMapping("/{id}/review")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query') and @ss.hasPermi('ont:metric:query') and @ss.hasPermi('ont:metric:execute')")
    public CommonResult<Proposal> review(@PathVariable String id) {
        return CommonResult.success(proposals.reviewEffect(id));
    }
}
