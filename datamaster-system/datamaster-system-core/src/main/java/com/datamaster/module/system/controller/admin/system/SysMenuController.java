

package com.datamaster.module.system.controller.admin.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.constant.UserConstants;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.entity.SysMenu;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.system.service.ISysMenuService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜单信息
 *
 * @author DATAMASTER
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController
{
    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取菜单列表
     */
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(SysMenu menu)
    {
        if (StringUtils.isNull(menu.getStatus())) {
            menu.setStatus("0");
        }
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(normalizeExistingMenus(menus));
    }

    /**
     * 根据菜单编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping(value = "/{menuId}")
    public AjaxResult getInfo(@PathVariable Long menuId)
    {
        return success(menuService.selectMenuById(menuId));
    }

    /**
     * 获取菜单下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysMenu menu)
    {
        menu.setStatus("0");
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeSelect(normalizeExistingMenus(menus)));
    }

    /**
     * 获取菜单下拉树列表(排除数据研发模块)
     */
    @GetMapping("/treeselectNoDpp")
    public AjaxResult treeselectNoDpp(SysMenu menu)
    {
        menu.setStatus("0");
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeNoSelectDpp(normalizeExistingMenus(menus)));
    }

    /**
     * 获取菜单下拉树列表(只限于数据研发模块)
     */
    @GetMapping("/treeselectDpp")
    public AjaxResult treeselectDpp(SysMenu menu)
    {
        menu.setStatus("0");
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeSelectDpp(normalizeExistingMenus(menus)));
    }

    /**
     * 加载对应角色菜单列表树
     */
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId)
    {
        SysMenu menu = new SysMenu();
        menu.setStatus("0");
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(normalizeExistingMenus(menus)));
        return ajax;
    }

    /**
     * 加载对应角色菜单列表树(排除数据研发模块)
     */
    @GetMapping(value = "/roleMenuTreeselectNoDpp/{roleId}")
    public AjaxResult roleMenuTreeselectNoDpp(@PathVariable("roleId") Long roleId)
    {
        SysMenu menu = new SysMenu();
        menu.setStatus("0");
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(normalizeExistingMenus(menus)));
        return ajax;
    }

    /**
     * 加载对应角色菜单列表树(只限于数据研发模块)
     */
    @GetMapping(value = "/roleMenuTreeselectDpp/{roleId}")
    public AjaxResult roleMenuTreeselectDpp(@PathVariable("roleId") Long roleId)
    {
        SysMenu sysMenu = new SysMenu();
        sysMenu.setPath("dpp");
        sysMenu.setStatus("0");
        List<SysMenu> menus = menuService.selectMenuList(sysMenu, getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelectDpp(normalizeExistingMenus(menus)));
        return ajax;
    }

    private List<SysMenu> normalizeExistingMenus(List<SysMenu> menus) {
        Set<Long> hiddenMenuIds = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysMenu menu : menus) {
                if (menu == null || menu.getMenuId() == null || hiddenMenuIds.contains(menu.getMenuId())) {
                    continue;
                }
                if (isHiddenSystemTool(menu) || hiddenMenuIds.contains(menu.getParentId())) {
                    hiddenMenuIds.add(menu.getMenuId());
                    changed = true;
                }
            }
        }

        List<SysMenu> result = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu == null || hiddenMenuIds.contains(menu.getMenuId())) {
                continue;
            }
            normalizeMenuName(menu);
            result.add(menu);
        }
        return result;
    }

    private boolean isHiddenSystemTool(SysMenu menu) {
        String path = menu.getPath();
        String menuName = menu.getMenuName();
        return "系统工具".equals(menuName)
                || "tool".equals(path)
                || "/tool".equals(path)
                || (StringUtils.isNotEmpty(path) && (path.startsWith("tool/") || path.startsWith("/tool")));
    }

    private void normalizeMenuName(SysMenu menu) {
        if ("系统监控".equals(menu.getMenuName())) {
            menu.setMenuName("日志管理");
        } else if ("数据治理".equals(menu.getMenuName())) {
            menu.setMenuName("元数据管理");
        } else if ("数据连接".equals(menu.getMenuName())) {
            menu.setMenuName("数据源管理");
        }
    }

    /**
     * 新增菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysMenu menu)
    {
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        menu.setCreateBy(getUsername());
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysMenu menu)
    {
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (menu.getMenuId().equals(menu.getParentId()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        menu.setUpdateBy(getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 删除菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        if (menuService.hasChildByMenuId(menuId))
        {
            return warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuId))
        {
            return warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }
}
