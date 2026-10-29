package com.datamaster.module.ai.controller.admin.profile;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.security.AccessPolicy;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileAvailableRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileRespVO;
import com.datamaster.module.ai.controller.admin.profile.vo.AiModelProfileSaveReqVO;
import com.datamaster.module.ai.service.profile.AiModelProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Trusted platform configuration, separate from ordinary ask-data/member access. */
@RestController
@RequestMapping("/ai/model-profiles")
public class AiModelProfileController {
    private final AiModelProfileService profiles;
    private final ISpaceApi spaces;

    public AiModelProfileController(AiModelProfileService profiles, ISpaceApi spaces) {
        this.profiles = profiles;
        this.spaces = spaces;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('ai:model-config:list')")
    public CommonResult<List<AiModelProfileRespVO>> list(@RequestParam(value = "spaceId", required = false) Long id,
            @RequestParam(value = "spaceCode", required = false) String code, HttpServletRequest http) {
        Scope scope = scope(SecurityUtils.getLoginUser(), id, code, http, true);
        return CommonResult.success(profiles.list(scope.id));
    }

    @GetMapping("/available")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    public CommonResult<List<AiModelProfileAvailableRespVO>> available(@RequestParam(value = "spaceId", required = false) Long id,
            @RequestParam(value = "spaceCode", required = false) String code, HttpServletRequest http) {
        Scope scope = scope(SecurityUtils.getLoginUser(), id, code, http, false);
        return CommonResult.success(profiles.available(scope.id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('ai:model-config:add')")
    public CommonResult<AiModelProfileRespVO> create(@Valid @RequestBody AiModelProfileSaveReqVO body, HttpServletRequest http) {
        Scope scope = scope(SecurityUtils.getLoginUser(), body.getSpaceId(), body.getSpaceCode(), http, true);
        return CommonResult.success(profiles.create(scope.id, scope.code, body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ai:model-config:edit')")
    public CommonResult<AiModelProfileRespVO> update(@PathVariable("id") Long id, @Valid @RequestBody AiModelProfileSaveReqVO body, HttpServletRequest http) {
        Scope scope = scope(SecurityUtils.getLoginUser(), body.getSpaceId(), body.getSpaceCode(), http, true);
        return CommonResult.success(profiles.update(id, scope.id, scope.code, body));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@ss.hasPermi('ai:model-config:edit')")
    public CommonResult<AiModelProfileRespVO> activate(@PathVariable("id") Long id,
            @RequestParam(value = "spaceId", required = false) Long spaceId,
            @RequestParam(value = "spaceCode", required = false) String code, HttpServletRequest http) {
        Scope scope = scope(SecurityUtils.getLoginUser(), spaceId, code, http, true);
        return CommonResult.success(profiles.activate(id, scope.id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('ai:model-config:remove')")
    public CommonResult<Boolean> delete(@PathVariable("id") Long id,
            @RequestParam(value = "spaceId", required = false) Long spaceId,
            @RequestParam(value = "spaceCode", required = false) String code, HttpServletRequest http) {
        Scope scope = scope(SecurityUtils.getLoginUser(), spaceId, code, http, true);
        profiles.delete(id, scope.id);
        return CommonResult.success(true);
    }

    Scope scope(LoginUser login, Long requestedId, String requestedCode, HttpServletRequest http, boolean management) {
        if (login == null || login.getUserId() == null || login.getUser() == null) throw new ServiceException("请先登录");
        List<SysRole> roles = login.getUser().getRoles();
        boolean admin = AccessPolicy.isPlatformAdmin(login.getUserId(), roles);
        // A space role named '运维' is not a platform operator. Require a system-space role.
        List<SysRole> systemRoles = new ArrayList<>();
        for (SysRole role : roles == null ? Collections.<SysRole>emptyList() : roles)
            if (role != null && AccessPolicy.isSystemUserRole(role)) systemRoles.add(role);
        if (management && !admin && !AccessPolicy.hasOpsRole(systemRoles)) throw new ServiceException("仅平台管理员或平台运维可管理模型配置");
        String code = trim(requestedCode);
        if (code != null && code.length() > 128) throw new ServiceException("空间编码无效");
        Long id = requestedId;
        if (id == null && code != null) id = spaces.getSpaceIdBySpaceCode(code);
        if (id == null || id <= 0) throw new ServiceException("请先选择当前空间");
        String canonical = spaces.getSpaceCodeBySpaceId(id);
        if (canonical == null || code != null && !canonical.equals(code)) throw new ServiceException("空间标识不一致");
        // A platform administrator may manage an explicit space without membership; member reads never bypass it.
        if ((!management || !admin) && !spaces.hasUserSpace(login.getUserId(), id, canonical)) throw new ServiceException("当前用户无权访问该空间");
        checkId(http.getParameter("spaceId"), id);
        checkCode(http.getParameter("spaceCode"), canonical);
        checkId(http.getHeader("spaceId"), id);
        checkId(http.getHeader("X-Space-Id"), id);
        checkCode(http.getHeader("spaceCode"), canonical);
        checkCode(http.getHeader("X-Space-Code"), canonical);
        return new Scope(id, canonical);
    }

    private static void checkId(String value, Long id) {
        if (trim(value) == null) return;
        try { if (id.equals(Long.valueOf(value.trim()))) return; }
        catch (NumberFormatException ignored) { /* Generic mismatch message, never echo user inputs. */ }
        throw new ServiceException("请求空间标识不一致");
    }
    private static void checkCode(String value, String code) {
        if (trim(value) != null && !code.equals(value.trim())) throw new ServiceException("请求空间标识不一致");
    }
    private static String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    static final class Scope {
        final Long id;
        final String code;
        Scope(Long id, String code) { this.id = id; this.code = code; }
    }
}
