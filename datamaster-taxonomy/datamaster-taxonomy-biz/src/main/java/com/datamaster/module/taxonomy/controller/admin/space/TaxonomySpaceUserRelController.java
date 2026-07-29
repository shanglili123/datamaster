

package com.datamaster.module.taxonomy.controller.admin.space;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.domain.entity.SysDept;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.page.TableDataInfo;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelSaveReqVO;
import com.datamaster.module.taxonomy.convert.space.TaxonomySpaceUserRelConvert;
import com.datamaster.module.taxonomy.dal.dataobject.space.TaxonomySpaceUserRelDO;
import com.datamaster.module.taxonomy.service.space.ITaxonomySpaceUserRelService;
import com.datamaster.module.system.domain.SysUserRole;
import com.datamaster.module.system.service.ISysDeptService;
import com.datamaster.module.system.service.ISysRoleService;
import com.datamaster.module.system.service.ISysUserService;
import com.datamaster.security.web.service.SysPermissionService;
import com.datamaster.security.web.service.TokenService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 空间成员关系 Controller
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Tag(name = "空间成员关系")
@RestController
@RequestMapping("/tax/spaceUserRel")
@Validated
public class TaxonomySpaceUserRelController extends BaseController {
    @Resource
    private ITaxonomySpaceUserRelService taxonomySpaceUserRelService;

    @Resource
    private ISysRoleService roleService;

    @Resource
    private TokenService tokenService;

    @Resource
    private SysPermissionService permissionService;

    @Resource
    private ISysUserService userService;

    @Resource
    private ISysDeptService deptService;

    @Operation(summary = "查询空间成员关系列表")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomySpaceUserRelRespVO>> list(TaxonomySpaceUserRelPageReqVO TaxonomySpaceUserRel) {
        PageResult<TaxonomySpaceUserRelDO> page = taxonomySpaceUserRelService.getSpaceUserRelPage(TaxonomySpaceUserRel);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomySpaceUserRelRespVO.class));
    }

    @Operation(summary = "导出空间成员关系列表")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:export')")
    @Log(title = "空间成员关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomySpaceUserRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomySpaceUserRelDO> list = (List<TaxonomySpaceUserRelDO>) taxonomySpaceUserRelService.getSpaceUserRelPage(exportReqVO).getRows();
        ExcelUtil<TaxonomySpaceUserRelRespVO> util = new ExcelUtil<>(TaxonomySpaceUserRelRespVO.class);
        util.exportExcel(response, TaxonomySpaceUserRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入空间成员关系列表")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:import')")
    @Log(title = "空间成员关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomySpaceUserRelRespVO> util = new ExcelUtil<>(TaxonomySpaceUserRelRespVO.class);
        List<TaxonomySpaceUserRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = taxonomySpaceUserRelService.importSpaceUserRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取空间成员关系详细信息")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomySpaceUserRelRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomySpaceUserRelDO TaxonomySpaceUserRelDO = taxonomySpaceUserRelService.getSpaceUserRelById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomySpaceUserRelDO, TaxonomySpaceUserRelRespVO.class));
    }

    @Operation(summary = "获取空间成员详情，包括角色信息")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:query')")
    @GetMapping(value = "/roleUser/{id}")
    public CommonResult<TaxonomySpaceUserRelRespVO> getSpaceUserRoleDetail(@PathVariable("id") Long id) {
        TaxonomySpaceUserRelRespVO respVO = taxonomySpaceUserRelService.getSpaceUserRoleDetail(id);
        return CommonResult.success(respVO);
    }

    @Operation(summary = "新增空间成员关系")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:add')")
    @Log(title = "空间成员关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomySpaceUserRelSaveReqVO TaxonomySpaceUserRel) {
        TaxonomySpaceUserRel.setCreatorId(getUserId());
        TaxonomySpaceUserRel.setCreateBy(getNickName());
        TaxonomySpaceUserRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(taxonomySpaceUserRelService.createSpaceUserRel(TaxonomySpaceUserRel));
    }

    @Operation(summary = "批量新增空间成员与角色关系")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:add')")
    @Log(title = "空间成员关系", businessType = BusinessType.INSERT)
    @PostMapping("/addUserListAndRoleList")
    public CommonResult<Boolean> addUserListAndRoleList(@Valid @RequestBody TaxonomySpaceUserRelSaveReqVO TaxonomySpaceUserRel) {
        TaxonomySpaceUserRel.setCreatorId(getUserId());
        TaxonomySpaceUserRel.setCreateBy(getNickName());
        TaxonomySpaceUserRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(taxonomySpaceUserRelService.createSpaceUsersAndRoles(TaxonomySpaceUserRel));
    }

    @Operation(summary = "修改空间成员关系")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:edit')")
    @Log(title = "空间成员关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomySpaceUserRelSaveReqVO TaxonomySpaceUserRel) {
        TaxonomySpaceUserRel.setUpdatorId(getUserId());
        TaxonomySpaceUserRel.setUpdateBy(getNickName());
        TaxonomySpaceUserRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(taxonomySpaceUserRelService.updateSpaceUserRel(TaxonomySpaceUserRel));
    }

    @Operation(summary = "修改空间成员与角色关系")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:edit')")
    @Log(title = "空间成员关系", businessType = BusinessType.UPDATE)
    @PutMapping("/editUserListAndRoleList")
    public CommonResult<Integer> editUserListAndRoleList(@Valid @RequestBody TaxonomySpaceUserRelSaveReqVO TaxonomySpaceUserRel) {
        TaxonomySpaceUserRel.setUpdatorId(getUserId());
        TaxonomySpaceUserRel.setUpdateBy(getNickName());
        TaxonomySpaceUserRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(taxonomySpaceUserRelService.updateSpaceUsersAndRoles(TaxonomySpaceUserRel));
    }

    @Operation(summary = "删除空间成员关系")
    @PreAuthorize("@ss.hasPermi('col:spaceUserRel:remove')")
    @Log(title = "空间成员关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(taxonomySpaceUserRelService.removeSpaceUserRel(Arrays.asList(ids)));
    }

    @PreAuthorize("@ss.hasPermi('col:space:role:list')")
    @GetMapping("/role/list")
    public TableDataInfo list(SysRole role) {
        startPage();
        List<SysRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('col:space:role:export')")
    @PostMapping("/role/export")
    public void export(HttpServletResponse response, SysRole role) {
        List<SysRole> list = roleService.selectRoleList(role);
        ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
        util.exportExcel(response, list, "角色数据");
    }

    /**
     * 根据角色编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:query')")
    @GetMapping(value = "/role/{roleId}")
    public AjaxResult getRoleInfo(@PathVariable Long roleId) {
        roleService.checkRoleDataScope(roleId);
        return success(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:add')")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping("/role")
    public AjaxResult add(@Validated @RequestBody SysRole role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        } else if (!roleService.checkRoleKeyUnique(role)) {
            return error("新增角色'" + role.getRoleName() + "'失败，权限字符已存在");
        }
        role.setCreateBy(getUsername());
        return toAjax(roleService.insertRole(role));

    }

    /**
     * 修改保存角色
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/role")
    public AjaxResult edit(@Validated @RequestBody SysRole role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        if (!roleService.checkRoleNameUnique(role)) {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        } else if (!roleService.checkRoleKeyUnique(role)) {
            return error("修改角色'" + role.getRoleName() + "'失败，权限字符已存在");
        }
        role.setUpdateBy(getUsername());

        if (roleService.updateRole(role) > 0) {
            // 更新缓存用户权限
            LoginUser loginUser = getLoginUser();
            if (StringUtils.isNotNull(loginUser.getUser()) && !loginUser.getUser().isAdmin()) {
                loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
                loginUser.setUser(userService.selectUserByUserName(loginUser.getUser().getUserName()));
                tokenService.setLoginUser(loginUser);
            }
            return success();
        }
        return error("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
    }

    /**
     * 修改保存数据权限
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/role/dataScope")
    public AjaxResult dataScope(@RequestBody SysRole role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/role/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysRole role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        role.setUpdateBy(getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 删除角色
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/role/{roleIds}")
    public AjaxResult removeRole(@PathVariable Long[] roleIds) {
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    /**
     * 获取角色选择框列表
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:query')")
    @GetMapping("/role/optionselect")
    public AjaxResult optionselect() {
        return success(roleService.selectRoleAll());
    }

    /**
     * 查询已分配用户角色列表
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:list')")
    @GetMapping("/role/authUser/allocatedList")
    public TableDataInfo allocatedList(SysUser user) {
        startPage();
        List<SysUser> list = userService.selectAllocatedList(user);
        return getDataTable(list);
    }

    /**
     * 查询未分配用户角色列表
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:list')")
    @GetMapping("/role/authUser/unallocatedList")
    public TableDataInfo unallocatedList(SysUser user) {
        startPage();
        List<SysUser> list = userService.selectUnallocatedList(user);
        return getDataTable(list);
    }

    /**
     * 取消授权用户
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/role/authUser/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole) {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    /**
     * 批量取消授权用户
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/role/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(Long roleId, Long[] userIds) {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    /**
     * 批量选择用户授权
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/role/authUser/selectAll")
    public AjaxResult selectAuthUserAll(Long roleId, Long[] userIds) {
        roleService.checkRoleDataScope(roleId);
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }

    /**
     * 获取对应角色部门树列表
     */
    @PreAuthorize("@ss.hasPermi('col:space:role:query')")
    @GetMapping(value = "/role/deptTree/{roleId}")
    public AjaxResult deptTree(@PathVariable("roleId") Long roleId) {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.selectDeptTreeList(new SysDept()));
        return ajax;
    }

}
