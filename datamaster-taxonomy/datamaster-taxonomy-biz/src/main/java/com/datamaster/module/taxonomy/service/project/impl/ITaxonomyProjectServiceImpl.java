

package com.datamaster.module.taxonomy.service.project.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import com.datamaster.api.ds.api.project.DsProjectCreateReqDTO;
import com.datamaster.api.ds.api.project.DsProjectDeleteRespDTO;
import com.datamaster.api.ds.api.project.DsProjectRespDTO;
import com.datamaster.api.ds.api.project.DsProjectUpdateReqDTO;
import com.datamaster.api.ds.api.service.project.IDsProjectService;
import com.datamaster.common.constant.Constants;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.PageUtils;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.project.ITaxonomyProjectApi;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectReqDTO;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectRespDTO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectSaveReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomySysUserReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectDO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectUserRelDO;
import com.datamaster.module.taxonomy.dal.mapper.project.TaxonomyProjectMapper;
import com.datamaster.module.taxonomy.dal.mapper.project.TaxonomyProjectUserRelMapper;
import com.datamaster.module.taxonomy.service.project.ITaxonomyProjectService;
import com.datamaster.module.system.domain.SysRoleMenu;
import com.datamaster.module.system.domain.SysUserRole;
import com.datamaster.module.system.mapper.SysRoleMapper;
import com.datamaster.module.system.mapper.SysRoleMenuMapper;
import com.datamaster.module.system.mapper.SysUserMapper;
import com.datamaster.module.system.mapper.SysUserRoleMapper;
import com.datamaster.security.context.PermissionContextHolder;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目Service业务层处理
 *
 * @author shu
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ITaxonomyProjectServiceImpl extends ServiceImpl<TaxonomyProjectMapper, TaxonomyProjectDO> implements ITaxonomyProjectService, ITaxonomyProjectApi {
    @Resource
    private TaxonomyProjectMapper TaxonomyProjectMapper;
    @Resource
    private TaxonomyProjectUserRelMapper TaxonomyProjectUserRelMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Resource
    private IDsProjectService dsProjectService;

    @Override
    public PageResult<TaxonomyProjectDO> getAttProjectPage(TaxonomyProjectPageReqVO pageReqVO) {
        Page<TaxonomyProjectDO> TaxonomyProjectDOPage = TaxonomyProjectMapper
                .selectAttProjectListByPage(new Page(pageReqVO.getPageNum(), pageReqVO.getPageSize()), pageReqVO);
        PageResult pageResult = new PageResult();
        pageResult.setTotal(TaxonomyProjectDOPage.getTotal());
        pageResult.setRows(TaxonomyProjectDOPage.getRecords());
        return pageResult;
    }

    @Override
    public PageResult<TaxonomyProjectRespDTO> getAttProjectPage(TaxonomyProjectReqDTO pageReqVO) {
        TaxonomyProjectPageReqVO TaxonomyProjectPageReqVO = BeanUtils.toBean(pageReqVO, TaxonomyProjectPageReqVO.class);
        PageResult<TaxonomyProjectDO> TaxonomyProjectPage = this.getAttProjectPage(TaxonomyProjectPageReqVO);
        return BeanUtils.toBean(TaxonomyProjectPage, TaxonomyProjectRespDTO.class);
    }

    @Override
    public Long createAttProject(TaxonomyProjectSaveReqVO createReqVO) {
        DsProjectCreateReqDTO dsProjectCreateReqDTO = new DsProjectCreateReqDTO();
        dsProjectCreateReqDTO.setProjectName(createReqVO.getName());
        dsProjectCreateReqDTO.setDescription(createReqVO.getDescription());
        DsProjectRespDTO dsProjectRespDTO = dsProjectService.saveProject(dsProjectCreateReqDTO);
        if (dsProjectRespDTO.getCode() != 0) {
            return -1L;
        }
        TaxonomyProjectDO dictType = BeanUtils.toBean(createReqVO, TaxonomyProjectDO.class);
        dictType.setCode(dsProjectRespDTO.getData().getCode().toString());
        try {
            // 新增项目管理数据
            TaxonomyProjectMapper.insert(dictType);
            if (dictType.getManagerId() != null) {
                // 新增项目与用户关联数据
                TaxonomyProjectUserRelDO TaxonomyProjectUserRelDO = new TaxonomyProjectUserRelDO();
                TaxonomyProjectUserRelDO.setProjectId(dictType.getId());
                TaxonomyProjectUserRelDO.setUserId(dictType.getManagerId());
                TaxonomyProjectUserRelMapper.insert(TaxonomyProjectUserRelDO);
                // 查询内置角色表
                SysRole sysRole = new SysRole();
                sysRole.setProjectId(-1L);
                List<SysRole> roleList = sysRoleMapper.selectRoleList(sysRole);
                if (!roleList.isEmpty()) {
                    List<SysUserRole> userRoleList = new ArrayList<>();
                    List<SysRole> sysRoleList = new ArrayList<>();
                    for (SysRole role : roleList) {
                        SysRole sRole = new SysRole();
                        sRole.setOldRoleId(role.getRoleId());
                        sRole.setProjectId(dictType.getId());
                        sRole.setRoleName(role.getRoleName());
                        sRole.setRoleKey(role.getRoleKey());
                        sRole.setRoleSort(role.getRoleSort());
                        sRole.setDataScope(role.getDataScope());
                        sRole.setMenuCheckStrictly(role.isMenuCheckStrictly());
                        sRole.setDeptCheckStrictly(role.isDeptCheckStrictly());
                        sRole.setStatus(role.getStatus());
                        sysRoleList.add(sRole);
                    }
                    sysRoleMapper.insertRoleList(sysRoleList);
                    List<SysRole> sysRoleGlyList = sysRoleList.stream().filter(sysRole1 -> "gly".equals(sysRole1.getRoleKey())).collect(Collectors.toList());
                    for (SysRole role : sysRoleGlyList) {
                        SysUserRole sysUserRole = new SysUserRole();
                        sysUserRole.setUserId(dictType.getManagerId());
                        sysUserRole.setRoleId(role.getRoleId());
                        userRoleList.add(sysUserRole);
                    }
                    sysUserRoleMapper.batchUserRole(userRoleList);
                    List<Long> roleIdList = roleList.stream().map(SysRole::getRoleId).collect(Collectors.toList());
                    List<SysRoleMenu> roleMenuList = sysRoleMenuMapper.getByRoleIdList(roleIdList);
                    Map<Long, List<SysRoleMenu>> roleIdListMap = roleMenuList.stream().collect(Collectors.groupingBy(SysRoleMenu::getRoleId));
                    List<SysRoleMenu> rMenusList = new ArrayList<>();
                    for (SysRole role : sysRoleList) {
                        if (roleIdListMap.get(role.getOldRoleId()) == null || roleIdListMap.get(role.getOldRoleId()).size() == 0){
                            continue;
                        }
                        for (SysRoleMenu sysRoleMenu : roleIdListMap.get(role.getOldRoleId())) {
                            SysRoleMenu roleMenu = new SysRoleMenu();
                            roleMenu.setRoleId(role.getRoleId());
                            roleMenu.setMenuId(sysRoleMenu.getMenuId());
                            roleMenu.setProjectId(dictType.getId());
                            rMenusList.add(roleMenu);
                        }
                    }
                    sysRoleMenuMapper.batchRoleMenuProjectId(rMenusList);
                }
            }
        }catch (Exception e){
            // 如果发送报错就删除ds里面的数据
            dsProjectService.deleteProject(dsProjectRespDTO.getData().getCode());
            e.printStackTrace();
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return -2L;
        }

        return dictType.getId();
    }

    @Override
    public int updateAttProject(TaxonomyProjectSaveReqVO updateReqVO) {
        // 相关校验
        DsProjectUpdateReqDTO dsProjectUpdateReqDTO = new DsProjectUpdateReqDTO();
        dsProjectUpdateReqDTO.setProjectName(updateReqVO.getName());
        dsProjectUpdateReqDTO.setProjectCode(Long.valueOf(updateReqVO.getCode()));
        dsProjectUpdateReqDTO.setDescription(updateReqVO.getDescription());
        DsProjectRespDTO dsProjectRespDTO = dsProjectService.updateProject(dsProjectUpdateReqDTO);
        if (dsProjectRespDTO.getCode() != 0) {
            return -1;
        }
        // 更新项目
        TaxonomyProjectDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyProjectDO.class);
        int i = -1;
        try {
            i = TaxonomyProjectMapper.updateById(updateObj);
        }catch (Exception e){
            // 如果发送报错就删除ds里面的数据
            dsProjectService.deleteProject(dsProjectRespDTO.getData().getCode());
            e.printStackTrace();
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return i;
    }

    @Override
    public int removeAttProject(Collection<Long> idList) {
        QueryWrapper<TaxonomyProjectUserRelDO> projectWrapper = new QueryWrapper<>();
        projectWrapper.in(!CollectionUtils.isEmpty(idList), "project_id", idList);
        List<TaxonomyProjectUserRelDO> TaxonomyProjectUserRelDOList = TaxonomyProjectUserRelMapper.selectList(projectWrapper);
        if (TaxonomyProjectUserRelDOList.size() > 0) {
            return -1;
        }
        List<TaxonomyProjectDO> projectDOList = TaxonomyProjectMapper.selectList(new QueryWrapper<TaxonomyProjectDO>().in(!CollectionUtils.isEmpty(idList), "id", idList));
        int i = TaxonomyProjectMapper.deleteBatchIds(idList);
        for (TaxonomyProjectDO TaxonomyProjectDO : projectDOList) {
            DsProjectDeleteRespDTO dsProjectDeleteRespDTO = dsProjectService.deleteProject(Long.valueOf(TaxonomyProjectDO.getCode()));
            if (dsProjectDeleteRespDTO.getCode() != 0) {
                // 手动回滚事务
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return -2;
            }
        }
        // 批量删除项目
        return i;
    }

    @Override
    public TaxonomyProjectDO getAttProjectById(Long id) {
        TaxonomyProjectDO projectDO = TaxonomyProjectMapper.selectById(id);

        return projectDO;
    }

    @Override
    public List<TaxonomyProjectDO> getAttProjectList() {
        return TaxonomyProjectMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyProjectDO> getAttProjectMap() {
        List<TaxonomyProjectDO> TaxonomyProjectList = TaxonomyProjectMapper.selectList();
        return TaxonomyProjectList.stream()
                .collect(Collectors.toMap(
                        TaxonomyProjectDO::getId,
                        TaxonomyProjectDO -> TaxonomyProjectDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }

    /**
     * 导入项目数据
     *
     * @param importExcelList 项目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttProject(List<TaxonomyProjectRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyProjectRespVO respVO : importExcelList) {
            try {
                TaxonomyProjectDO TaxonomyProjectDO = BeanUtils.toBean(respVO, TaxonomyProjectDO.class);
                Long TaxonomyProjectId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyProjectId != null) {
                        TaxonomyProjectDO existingAttProject = TaxonomyProjectMapper.selectById(TaxonomyProjectId);
                        if (existingAttProject != null) {
                            TaxonomyProjectMapper.updateById(TaxonomyProjectDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyProjectId + " 的项目记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyProjectId + " 的项目记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyProjectDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyProjectId);
                    TaxonomyProjectDO existingAttProject = TaxonomyProjectMapper.selectOne(queryWrapper);
                    if (existingAttProject == null) {
                        TaxonomyProjectMapper.insert(TaxonomyProjectDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyProjectId + " 的项目记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyProjectId + " 的项目记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }

    /**
     * 获取当前用户是非具备用户添加和项目管理员
     *
     * @param userId 用户ID
     * @return
     */
    @Override
    public JSONObject addUserAndProjectIsOk(Long userId, Long id) {
        JSONObject jsonObject = new JSONObject();
        QueryWrapper<TaxonomyProjectDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ID", id).eq("MANAGER_ID", userId);
        TaxonomyProjectDO TaxonomyProjectDO = TaxonomyProjectMapper.selectOne(queryWrapper);
        jsonObject.set("isManagerId", TaxonomyProjectDO != null);
        if (StringUtils.isEmpty("system:user:add")) {
            jsonObject.set("isUserDaa", false);
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            jsonObject.set("isUserDaa", false);
        }
        PermissionContextHolder.setContext("system:user:add");
        Boolean isOk = loginUser.getPermissions().contains(Constants.ALL_PERMISSION)
                || loginUser.getPermissions().contains(StringUtils.trim("system:user:add"));
        jsonObject.set("isUserDaa", isOk);
        return jsonObject;
    }

    /**
     * 查询当前用户所属的项目列表
     *
     * @param userId 用户id
     * @return
     */
    @Override
    public List<TaxonomyProjectDO> getCurrentUserList(Long userId) {
        if (userId == 1) {
            return TaxonomyProjectMapper.selectList();
        }
        QueryWrapper<TaxonomyProjectUserRelDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<TaxonomyProjectUserRelDO> userRelDOList = TaxonomyProjectUserRelMapper.selectList(queryWrapper);
        List<Long> projectIds = userRelDOList.stream()
                .map(TaxonomyProjectUserRelDO::getProjectId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
            return new ArrayList<>();
        }
        QueryWrapper<TaxonomyProjectDO> projectWrapper = new QueryWrapper<>();
        projectWrapper.in(!CollectionUtils.isEmpty(projectIds), "id", projectIds).eq("valid_flag", "1");
        return TaxonomyProjectMapper.selectList(projectWrapper);
    }

    /**
     * 获取用户列表排除当前项目已经存在的用户
     */
    @Override
    public List<SysUser> selectNoProjectUserList(TaxonomySysUserReqVO user) {
        QueryWrapper<TaxonomyProjectUserRelDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PROJECT_ID", user.getProjectId());
        List<TaxonomyProjectUserRelDO> projectUserRelDOList = TaxonomyProjectUserRelMapper.selectList(queryWrapper);
        List<Long> userIdList = projectUserRelDOList.stream()
                .map(TaxonomyProjectUserRelDO::getUserId)
                .collect(Collectors.toList());
        TaxonomyProjectDO projectDO = TaxonomyProjectMapper.selectById(user.getProjectId());
        userIdList.add(projectDO.getManagerId());
        userIdList.add(1L);
        SysUser sysUser = new SysUser();
        sysUser.setUserIdList(userIdList);
        sysUser.setStatus("0");
        sysUser.setPhonenumber(user.getPhonenumber());
        sysUser.setUserName(user.getUserName());
        PageUtils.startPage();
        List<SysUser> sysUserList = sysUserMapper.selectNoProjectUserList(sysUser);
        return sysUserList;
    }

    @Override
    public Boolean editProjectStatus(Long id,Long status) {
        return this.update(Wrappers.lambdaUpdate(TaxonomyProjectDO.class)
                .eq(TaxonomyProjectDO::getId, id)
                .set(TaxonomyProjectDO::getValidFlag, status));
    }

    @Override
    public Long getProjectIdByProjectCode(String projectCode) {
        TaxonomyProjectDO projectDO = baseMapper.selectOne(Wrappers.lambdaQuery(TaxonomyProjectDO.class)
                .eq(TaxonomyProjectDO::getCode, projectCode));
        if (projectDO != null) {
            return projectDO.getId();
        }
        return null;
    }
}
