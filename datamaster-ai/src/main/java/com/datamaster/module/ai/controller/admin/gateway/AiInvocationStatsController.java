package com.datamaster.module.ai.controller.admin.gateway;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.service.gateway.AiInvocationGateway;
import com.datamaster.module.ai.service.gateway.AiInvocationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Only authorized current-space persisted aggregates; no cross-space live/process counters. */
@RestController
@RequestMapping("/ai/model-gateway")
public class AiInvocationStatsController {
    private final ISpaceApi spaceApi;
    private final AiInvocationRepository repository;
    private final AiInvocationGateway gateway;

    public AiInvocationStatsController(ISpaceApi spaceApi, AiInvocationRepository repository,
                                       AiInvocationGateway gateway) {
        this.spaceApi = spaceApi;
        this.repository = repository;
        this.gateway = gateway;
    }

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    public CommonResult<Map<String, Object>> stats(@RequestParam("spaceId") Long spaceId,
                                                  @RequestParam("spaceCode") String spaceCode,
                                                  @RequestParam(value = "days", defaultValue = "7") int days) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) throw new ServiceException("请先登录");
        if (days < 1 || days > 30) throw new ServiceException("统计天数必须为1至30天");
        if (spaceId == null || spaceId <= 0 || spaceCode == null || spaceCode.trim().isEmpty()
                || spaceCode.length() > 128) {
            throw new ServiceException("请提供当前空间的ID和编码");
        }
        String normalizedCode = spaceCode.trim();
        // Check membership before resolving the canonical code to avoid cross-space probing.
        if (!spaceApi.hasUserSpace(userId, spaceId, normalizedCode)) {
            throw new ServiceException("当前用户无权访问该空间");
        }
        String canonicalCode = spaceApi.getSpaceCodeBySpaceId(spaceId);
        if (canonicalCode == null || !canonicalCode.equals(normalizedCode)) {
            throw new ServiceException("空间标识不一致");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("spaceId", spaceId);
        result.put("spaceCode", canonicalCode);
        result.put("days", days);
        result.put("groups", repository.aggregate(spaceId, canonicalCode, days));
        result.put("processCapacity", gateway.processCapacity());
        result.put("attemptDefinition", "One persisted logical invocation per valid begin, including retries inside its lease; capacity rejections included, active leases and failed writes excluded");
        result.put("usageDefinition", "tokensCount sums measured nonnegative input/output, including partial usage; usageKnownCount requires both values and a representable total; unknown usage is not zero usage");
        result.put("persistence", "Best-effort records only; failed writes are tracked by internal process persistenceFailures metrics, not included in these aggregates");
        return CommonResult.success(result);
    }
}
