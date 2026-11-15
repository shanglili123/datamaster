package com.datamaster.module.ai.controller.admin.profile;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelRoutingSaveReqVO;
import com.datamaster.module.ai.service.profile.AiModelRoutingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/ai/model-routing")
public class AiModelRoutingController {
    private final AiModelRoutingService routing;
    private final AiModelProfileController profileAccess;
    public AiModelRoutingController(AiModelRoutingService routing, AiModelProfileController profileAccess) {
        this.routing = routing; this.profileAccess = profileAccess;
    }
    @GetMapping
    @PreAuthorize("@ss.hasPermi('ai:model-config:list')")
    public CommonResult<AiModelRoutingRespVO> get(@RequestParam Long spaceId,
            @RequestParam(required = false) String spaceCode, HttpServletRequest http) {
        AiModelProfileController.Scope scope = profileAccess.scope(SecurityUtils.getLoginUser(), spaceId, spaceCode, http, true);
        return CommonResult.success(routing.get(scope.id, scope.code));
    }
    @PutMapping
    @PreAuthorize("@ss.hasPermi('ai:model-config:edit')")
    public CommonResult<AiModelRoutingRespVO> save(@Valid @RequestBody AiModelRoutingSaveReqVO body, HttpServletRequest http) {
        AiModelProfileController.Scope scope = profileAccess.scope(SecurityUtils.getLoginUser(), body.getSpaceId(), body.getSpaceCode(), http, true);
        return CommonResult.success(routing.save(scope.id, scope.code, SecurityUtils.getUserId(), body));
    }
}
