

package com.datamaster.security.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.constant.Constants;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.security.AccessPolicy;
import com.datamaster.module.system.service.ISysMenuService;
import com.datamaster.module.system.service.ISysRoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户权限处理
 *
 * @author DATAMASTER
 */
@Component
public class SysPermissionService
{
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取角色数据权限
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user)
    {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (AccessPolicy.isPlatformAdmin(user.getUserId(), user.getRoles()))
        {
            roles.add(Constants.SUPER_ADMIN);
        }
        else
        {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     *
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user)
    {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (AccessPolicy.isPlatformAdmin(user.getUserId(), user.getRoles()))
        {
            perms.add(Constants.ALL_PERMISSION);
        }
        else
        {
            List<SysRole> roles = user.getRoles();
            if (!CollectionUtils.isEmpty(roles))
            {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (SysRole role : roles)
                {
                    Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                    role.setPermissions(rolePerms);
                    perms.addAll(rolePerms);
                }
            }
            else
            {
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
            if (AccessPolicy.hasSpaceAdminRole(roles))
            {
                for (String perm : menuService.selectMenuPerms())
                {
                    if (AccessPolicy.isSpaceAdminPermission(perm))
                    {
                        perms.add(perm);
                    }
                }
            }
            else if (AccessPolicy.hasOpsRole(roles))
            {
                for (String perm : menuService.selectMenuPerms())
                {
                    if (AccessPolicy.isOpsPermission(perm))
                    {
                        perms.add(perm);
                    }
                }
            }
            perms.removeIf(permission -> !AccessPolicy.canUsePermission(permission, user.getUserId(), roles));
        }
        return perms;
    }
}
