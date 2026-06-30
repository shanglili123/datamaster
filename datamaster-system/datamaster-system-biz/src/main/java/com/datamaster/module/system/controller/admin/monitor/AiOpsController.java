package com.datamaster.module.system.controller.admin.monitor;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.system.service.monitor.IAiOpsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * AI ops monitor.
 */
@RestController
@RequestMapping("/monitor/ai-ops")
public class AiOpsController {

    @Resource
    private IAiOpsService aiOpsService;

    @PreAuthorize("@ss.hasPermi('monitor:aiops:list')")
    @GetMapping("/diagnose")
    public AjaxResult diagnose() throws Exception {
        return AjaxResult.success(aiOpsService.diagnose());
    }
}
