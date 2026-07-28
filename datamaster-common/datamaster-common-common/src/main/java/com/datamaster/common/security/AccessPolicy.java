package com.datamaster.common.security;

import com.datamaster.common.core.domain.entity.SysMenu;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.utils.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * Central role and permission policy for platform and project access.
 */
public class AccessPolicy
{
    public static final Long SYSTEM_PROJECT_ID = 0L;
    public static final Long SYSTEM_ADMIN_ROLE_ID = 2L;
    public static final Long NORMAL_USER_ROLE_ID = 7L;

    private AccessPolicy()
    {
    }

    public static boolean isPlatformAdmin(Long userId, List<SysRole> roles)
    {
        return SysUser.isAdmin(userId) || hasRoleId(roles, SYSTEM_ADMIN_ROLE_ID);
    }

    public static boolean isSuperAdmin(Long userId)
    {
        return SysUser.isAdmin(userId);
    }

    public static boolean isSystemUserRole(SysRole role)
    {
        return role != null && SYSTEM_PROJECT_ID.equals(role.getProjectId());
    }

    public static boolean isAssignableSystemUserRole(SysRole role, Long operatorUserId)
    {
        if (!isSystemUserRole(role) || role.isAdmin())
        {
            return false;
        }
        if (SYSTEM_ADMIN_ROLE_ID.equals(role.getRoleId()))
        {
            return isSuperAdmin(operatorUserId);
        }
        return NORMAL_USER_ROLE_ID.equals(role.getRoleId());
    }

    public static boolean isAllowedSystemUserRole(SysRole role)
    {
        return isSystemUserRole(role)
                && (SYSTEM_ADMIN_ROLE_ID.equals(role.getRoleId()) || NORMAL_USER_ROLE_ID.equals(role.getRoleId()));
    }

    public static boolean hasProjectAdminRole(List<SysRole> roles)
    {
        for (SysRole role : safeRoles(roles))
        {
            String roleKey = lower(role.getRoleKey());
            String roleName = role.getRoleName();
            if ("gly".equals(roleKey)
                    || (roleKey != null && roleKey.endsWith("-admin"))
                    || (roleName != null && roleName.contains("空间管理员")))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOpsRole(List<SysRole> roles)
    {
        for (SysRole role : safeRoles(roles))
        {
            String roleKey = lower(role.getRoleKey());
            String roleName = role.getRoleName();
            if ("yw".equals(roleKey) || (roleName != null && roleName.contains("运维")))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean canAccessMenu(SysMenu menu, Long userId, List<SysRole> roles, boolean projectMode)
    {
        if (isPlatformAdmin(userId, roles))
        {
            return true;
        }
        if (menu == null)
        {
            return false;
        }
        if (isSystemMenu(menu))
        {
            return false;
        }
        if (isLogMenu(menu))
        {
            return hasOpsRole(roles) || hasProjectAdminRole(roles);
        }
        if (isProjectBaseMenu(menu))
        {
            return hasProjectAdminRole(roles);
        }
        return true;
    }

    public static boolean canUsePermission(String permission, Long userId, List<SysRole> roles)
    {
        if (isPlatformAdmin(userId, roles))
        {
            return true;
        }
        if (StringUtils.isEmpty(permission))
        {
            return false;
        }
        if (isSystemPermission(permission))
        {
            return false;
        }
        if (isPlatformOnlyPermission(permission))
        {
            return false;
        }
        if (isLogPermission(permission))
        {
            return hasOpsRole(roles) || hasProjectAdminRole(roles);
        }
        if (isProjectBasePermission(permission))
        {
            return hasProjectAdminRole(roles);
        }
        return true;
    }

    public static boolean isProjectAdminPermission(String permission)
    {
        return !isSystemPermission(permission) && !isPlatformOnlyPermission(permission);
    }

    public static boolean isOpsPermission(String permission)
    {
        return isLogPermission(permission);
    }

    private static boolean isSystemMenu(SysMenu menu)
    {
        String path = lower(menu.getPath());
        String perms = lower(menu.getPerms());
        String name = menu.getMenuName();
        return "系统管理".equals(name)
                || "sys".equals(path)
                || "system".equals(path)
                || (perms != null && perms.startsWith("system:"));
    }

    private static boolean isProjectBaseMenu(SysMenu menu)
    {
        String path = lower(menu.getPath());
        String perms = lower(menu.getPerms());
        String name = menu.getMenuName();
        return "空间基础管理".equals(name)
                || "成员角色管理".equals(name)
                || "setting".equals(path)
                || "projectuserrel".equals(path)
                || "taskcat".equals(path)
                || "datadevcat".equals(path)
                || isProjectBasePermission(perms);
    }

    private static boolean isLogMenu(SysMenu menu)
    {
        String path = lower(menu.getPath());
        String perms = lower(menu.getPerms());
        String name = menu.getMenuName();
        return (name != null && name.contains("日志"))
                || (path != null && path.contains("log"))
                || isLogPermission(perms);
    }

    private static boolean isSystemPermission(String permission)
    {
        String value = lower(permission);
        return value != null && value.startsWith("system:");
    }

    private static boolean isPlatformOnlyPermission(String permission)
    {
        String value = lower(permission);
        return "tax:project:add".equals(value);
    }

    private static boolean isLogPermission(String permission)
    {
        String value = lower(permission);
        return value != null && (value.startsWith("monitor:") || value.contains("log"));
    }

    private static boolean isProjectBasePermission(String permission)
    {
        String value = lower(permission);
        return value != null && (value.startsWith("col:projectuserrel:")
                || value.startsWith("col:project:role:")
                || value.startsWith("col:taskcat:")
                || value.startsWith("col:datadevcat:"));
    }

    private static boolean hasRoleId(List<SysRole> roles, Long roleId)
    {
        for (SysRole role : safeRoles(roles))
        {
            if (role != null && roleId.equals(role.getRoleId()))
            {
                return true;
            }
        }
        return false;
    }

    private static Collection<SysRole> safeRoles(List<SysRole> roles)
    {
        return roles == null ? java.util.Collections.emptyList() : roles;
    }

    private static String lower(String value)
    {
        return value == null ? null : value.toLowerCase();
    }
}
