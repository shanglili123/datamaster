package com.datamaster.module.ai.controller.admin.profile;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.exception.ServiceException;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Authenticated-thread scope and platform identity checks, independent of URL/token validation. */
public class AiModelProfileControllerTest {
    @Test public void spaceAdminCannotManageEvenWithModelPermission() {
        AiModelProfileController controller = controller(true);
        LoginUser member = user(100L, role(11L, 10L, "space-admin", "空间管理员"));
        member.setPermissions(Collections.singleton("ai:model-config:edit"));
        rejected(() -> controller.scope(member, 10L, "space10", request(null, null), true));
        assertEquals(Long.valueOf(10), controller.scope(member, 10L, "space10", request(null, null), false).id);
    }

    @Test public void onlyPlatformAdministratorMayManageWithoutMembership() {
        AiModelProfileController controller = controller(false);
        LoginUser admin = user(100L, role(2L, 0L, "admin", "系统管理员"));
        assertEquals(Long.valueOf(10), controller.scope(admin, 10L, "space10", request(null, null), true).id);
        rejected(() -> controller.scope(admin, 10L, "space10", request(null, null), false));
        LoginUser platformOps = user(101L, role(12L, 0L, "yw", "平台运维"));
        rejected(() -> controller.scope(platformOps, 10L, "space10", request(null, null), true));
        assertEquals(Long.valueOf(10), controller(true).scope(platformOps, 10L, "space10", request(null, null), true).id);
        LoginUser spaceOps = user(102L, role(13L, 10L, "yw", "空间运维"));
        rejected(() -> controller(true).scope(spaceOps, 10L, "space10", request(null, null), true));
    }

    @Test public void canonicalScopeAndHeaderQueryMustAgree() {
        AiModelProfileController controller = controller(true);
        LoginUser member = user(100L, role(11L, 10L, "member", "成员"));
        rejected(() -> controller.scope(member, 10L, "different", request(null, null), false));
        rejected(() -> controller.scope(member, 10L, "space10", request("11", null), false));
        rejected(() -> controller.scope(member, 10L, "space10", request(null, "wrong"), false));
        assertEquals(Long.valueOf(10), controller.scope(member, null, "space10", request("10", "space10"), false).id);
        rejected(() -> controller.scope(member, null, null, request("10", "space10"), false));
    }

    private static AiModelProfileController controller(boolean member) {
        ISpaceApi spaces = (ISpaceApi) Proxy.newProxyInstance(ISpaceApi.class.getClassLoader(), new Class<?>[]{ISpaceApi.class}, (proxy, method, args) -> {
            if ("getSpaceIdBySpaceCode".equals(method.getName())) return "space10".equals(args[0]) ? 10L : null;
            if ("getSpaceCodeBySpaceId".equals(method.getName())) return Long.valueOf(10).equals(args[0]) ? "space10" : null;
            if ("hasUserSpace".equals(method.getName())) return member;
            return null;
        });
        return new AiModelProfileController(null, spaces);
    }
    private static HttpServletRequest request(String headerId, String queryCode) {
        Map<String, String> headers = new HashMap<>(); headers.put("X-Space-Id", headerId);
        Map<String, String> parameters = new HashMap<>(); parameters.put("spaceCode", queryCode);
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
            if ("getHeader".equals(method.getName())) return headers.get(args[0]);
            if ("getParameter".equals(method.getName())) return parameters.get(args[0]);
            return null;
        });
    }
    private static LoginUser user(Long id, SysRole role) {
        SysUser user = new SysUser(); user.setUserId(id); user.setRoles(Collections.singletonList(role));
        return new LoginUser(id, null, user, Collections.emptySet());
    }
    private static SysRole role(Long id, Long spaceId, String key, String name) {
        SysRole role = new SysRole(); role.setRoleId(id); role.setSpaceId(spaceId); role.setRoleKey(key); role.setRoleName(name);
        return role;
    }
    private static void rejected(Runnable action) {
        try { action.run(); fail("Expected safe rejection"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
}
