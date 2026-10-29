package com.datamaster.module.ai.controller.agent;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ai.agent.decision.DecisionModels.Proposal;
import com.datamaster.module.ai.agent.decision.DecisionModels.Request;
import com.datamaster.module.ai.agent.decision.DecisionProposalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/** Human-facing decision boundary; confirmation is never inferred from chat memory. */
@RestController
@RequestMapping("/ai/decision")
public class DecisionProposalController {
    @Resource private DecisionProposalService proposals;

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    public CommonResult<Proposal> get(@PathVariable String id) {
        return CommonResult.success(proposals.get(id));
    }

    @PostMapping("/calculate")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    public CommonResult<Proposal> calculate(@RequestBody com.datamaster.module.ai.agent.decision.DecisionModels.CalculationRequest request) {
        return CommonResult.success(proposals.calculate(request));
    }

    @PostMapping("/prepare")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query') and @ss.hasPermi('ont:action:edit')")
    public CommonResult<Proposal> prepare(@RequestBody Request request) {
        return CommonResult.success(proposals.prepare(request));
    }

    @PostMapping("/confirm")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query') and @ss.hasPermi('ont:action:edit')")
    public CommonResult<Proposal> confirm(@RequestBody Request request) {
        return CommonResult.success(proposals.confirm(request));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    public CommonResult<Proposal> status(@PathVariable String id) {
        return CommonResult.success(proposals.refresh(id));
    }
}
