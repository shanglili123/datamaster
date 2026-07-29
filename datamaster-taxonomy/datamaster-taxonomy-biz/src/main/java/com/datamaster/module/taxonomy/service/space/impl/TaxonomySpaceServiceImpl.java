

package com.datamaster.module.taxonomy.service.space.impl;

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
import com.datamaster.api.ds.api.project.DsWorkerGroupRespDTO;
import com.datamaster.api.ds.api.base.DsResultDTO;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
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
import com.datamaster.module.taxonomy.api.space.ITaxonomySpaceApi;
import com.datamaster.module.taxonomy.api.space.dto.TaxonomySpaceReqDTO;
import com.datamaster.module.taxonomy.api.space.dto.TaxonomySpaceRespDTO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpacePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceRespVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceSaveReqVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySysUserReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.space.TaxonomySpaceDO;
import com.datamaster.module.taxonomy.dal.dataobject.space.TaxonomySpaceUserRelDO;
import com.datamaster.module.taxonomy.dal.mapper.space.TaxonomySpaceMapper;
import com.datamaster.module.taxonomy.dal.mapper.space.TaxonomySpaceUserRelMapper;
import com.datamaster.module.taxonomy.service.space.ITaxonomySpaceService;
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
 * 空间Service业务层处理
 *
 * @author shu
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomySpaceServiceImpl extends ServiceImpl<TaxonomySpaceMapper, TaxonomySpaceDO> implements ITaxonomySpaceService, ITaxonomySpaceApi {
    @Resource
    private TaxonomySpaceMapper taxonomySpaceMapper;
    @Resource
    private TaxonomySpaceUserRelMapper taxonomySpaceUserRelMapper;
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
    public PageResult<TaxonomySpaceDO> getSpacePage(TaxonomySpacePageReqVO pageReqVO) {
        Page<TaxonomySpaceDO> TaxonomySpaceDOPage = taxonomySpaceMapper
                .selectSpaceListByPage(new Page(pageReqVO.getPageNum(), pageReqVO.getPageSize()), pageReqVO);
        PageResult pageResult = new PageResult();
        pageResult.setTotal(TaxonomySpaceDOPage.getTotal());
        pageResult.setRows(TaxonomySpaceDOPage.getRecords());
        return pageResult;
    }

    @Override
    public PageResult<TaxonomySpaceRespDTO> getSpacePage(TaxonomySpaceReqDTO pageReqVO) {
        TaxonomySpacePageReqVO TaxonomySpacePageReqVO = BeanUtils.toBean(pageReqVO, TaxonomySpacePageReqVO.class);
        PageResult<TaxonomySpaceDO> TaxonomySpacePage = this.getSpacePage(TaxonomySpacePageReqVO);
        return BeanUtils.toBean(TaxonomySpacePage, TaxonomySpaceRespDTO.class);
    }

    @Override
    public Long createSpace(TaxonomySpaceSaveReqVO createReqVO) {
        DsProjectCreateReqDTO dsProjectCreateReqDTO = new DsProjectCreateReqDTO();
        dsProjectCreateReqDTO.setProjectName(createReqVO.getName());
        dsProjectCreateReqDTO.setDescription(createReqVO.getDescription());
        DsProjectRespDTO dsProjectRespDTO = dsProjectService.saveProject(dsProjectCreateReqDTO);
        if (dsProjectRespDTO.getCode() != 0) {
            return -1L;
        }
        Long spaceCode = dsProjectRespDTO.getData().getCode();
        String workerGroup = buildWorkerGroupName(spaceCode);
        Integer workerGroupId = null;
        try {
            DsWorkerGroupRespDTO workerGroupRespDTO = dsProjectService.saveWorkerGroup(workerGroup);
            if (!isSuccess(workerGroupRespDTO) || workerGroupRespDTO.getData() == null) {
                cleanupDsResources(spaceCode, null);
                return -1L;
            }
            workerGroupId = workerGroupRespDTO.getData().getId();
            DsStatusRespDTO assignRespDTO = dsProjectService.assignWorkerGroup(spaceCode, workerGroup);
            if (!isSuccess(assignRespDTO)) {
                cleanupDsResources(spaceCode, workerGroupId);
                return -1L;
            }
        } catch (Exception e) {
            cleanupDsResources(spaceCode, workerGroupId);
            log.error("Failed to create or assign DS worker group, spaceCode={}", spaceCode, e);
            return -1L;
        }
        TaxonomySpaceDO dictType = BeanUtils.toBean(createReqVO, TaxonomySpaceDO.class);
        dictType.setCode(spaceCode.toString());
        dictType.setWorkerGroupId(workerGroupId);
        dictType.setWorkerGroup(workerGroup);
        try {
            // 新增空间管理数据
            taxonomySpaceMapper.insert(dictType);
            if (dictType.getManagerId() != null) {
                // 新增空间与用户关联数据
                TaxonomySpaceUserRelDO TaxonomySpaceUserRelDO = new TaxonomySpaceUserRelDO();
                TaxonomySpaceUserRelDO.setSpaceId(dictType.getId());
                TaxonomySpaceUserRelDO.setUserId(dictType.getManagerId());
                taxonomySpaceUserRelMapper.insert(TaxonomySpaceUserRelDO);
                // 查询内置角色表
                SysRole sysRole = new SysRole();
                sysRole.setSpaceId(-1L);
                List<SysRole> roleList = sysRoleMapper.selectRoleList(sysRole);
                if (!roleList.isEmpty()) {
                    List<SysUserRole> userRoleList = new ArrayList<>();
                    List<SysRole> sysRoleList = new ArrayList<>();
                    for (SysRole role : roleList) {
                        SysRole sRole = new SysRole();
                        sRole.setOldRoleId(role.getRoleId());
                        sRole.setSpaceId(dictType.getId());
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
                            rMenusList.add(roleMenu);
                        }
                    }
                    if (!rMenusList.isEmpty()) {
                        sysRoleMenuMapper.batchRoleMenu(rMenusList);
                    }
                }
            }
        }catch (Exception e){
            // 如果发送报错就删除ds里面的数据
            cleanupDsResources(spaceCode, workerGroupId);
            e.printStackTrace();
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return -2L;
        }

        return dictType.getId();
    }

    @Override
    public int updateSpace(TaxonomySpaceSaveReqVO updateReqVO) {
        // 相关校验
        DsProjectUpdateReqDTO dsProjectUpdateReqDTO = new DsProjectUpdateReqDTO();
        dsProjectUpdateReqDTO.setProjectName(updateReqVO.getName());
        dsProjectUpdateReqDTO.setProjectCode(Long.valueOf(updateReqVO.getCode()));
        dsProjectUpdateReqDTO.setDescription(updateReqVO.getDescription());
        DsProjectRespDTO dsProjectRespDTO = dsProjectService.updateProject(dsProjectUpdateReqDTO);
        if (dsProjectRespDTO.getCode() != 0) {
            return -1;
        }
        // 更新空间
        TaxonomySpaceDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomySpaceDO.class);
        int i = -1;
        try {
            i = taxonomySpaceMapper.updateById(updateObj);
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
    public int removeSpace(Collection<Long> idList) {
        QueryWrapper<TaxonomySpaceUserRelDO> spaceWrapper = new QueryWrapper<>();
        spaceWrapper.in(!CollectionUtils.isEmpty(idList), "space_id", idList);
        List<TaxonomySpaceUserRelDO> spaceUserRelDOList = taxonomySpaceUserRelMapper.selectList(spaceWrapper);
        if (spaceUserRelDOList.size() > 0) {
            return -1;
        }
        List<TaxonomySpaceDO> spaceDOList = taxonomySpaceMapper.selectList(new QueryWrapper<TaxonomySpaceDO>().in(!CollectionUtils.isEmpty(idList), "id", idList));
        int i = taxonomySpaceMapper.deleteBatchIds(idList);
        for (TaxonomySpaceDO spaceDO : spaceDOList) {
            DsProjectDeleteRespDTO dsProjectDeleteRespDTO = dsProjectService.deleteProject(Long.valueOf(spaceDO.getCode()));
            if (dsProjectDeleteRespDTO.getCode() != 0) {
                // 手动回滚事务
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return -2;
            }
            if (spaceDO.getWorkerGroupId() != null) {
                DsStatusRespDTO deleteWorkerGroupRespDTO = dsProjectService.deleteWorkerGroup(spaceDO.getWorkerGroupId());
                if (!isSuccess(deleteWorkerGroupRespDTO)) {
                    log.warn("Failed to delete DS worker group, spaceCode={}, workerGroupId={}",
                            spaceDO.getCode(), spaceDO.getWorkerGroupId());
                }
            }
        }
        // 批量删除空间
        return i;
    }

    @Override
    public TaxonomySpaceDO getSpaceById(Long id) {
        TaxonomySpaceDO spaceDO = taxonomySpaceMapper.selectById(id);

        return spaceDO;
    }

    @Override
    public List<TaxonomySpaceDO> getSpaceList() {
        return taxonomySpaceMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomySpaceDO> getSpaceMap() {
        List<TaxonomySpaceDO> spaceList = taxonomySpaceMapper.selectList();
        return spaceList.stream()
                .collect(Collectors.toMap(
                        TaxonomySpaceDO::getId,
                        spaceDO -> spaceDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }

    /**
     * 导入空间数据
     *
     * @param importExcelList 空间数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importSpace(List<TaxonomySpaceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomySpaceRespVO respVO : importExcelList) {
            try {
                TaxonomySpaceDO spaceDO = BeanUtils.toBean(respVO, TaxonomySpaceDO.class);
                Long spaceId = respVO.getId();
                if (isUpdateSupport) {
                    if (spaceId != null) {
                        TaxonomySpaceDO existingSpace = taxonomySpaceMapper.selectById(spaceId);
                        if (existingSpace != null) {
                            taxonomySpaceMapper.updateById(spaceDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + spaceId + " 的空间记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + spaceId + " 的空间记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomySpaceDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", spaceId);
                    TaxonomySpaceDO existingSpace = taxonomySpaceMapper.selectOne(queryWrapper);
                    if (existingSpace == null) {
                        taxonomySpaceMapper.insert(spaceDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + spaceId + " 的空间记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + spaceId + " 的空间记录已存在。");
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
     * 判断当前用户是否可维护空间成员
     *
     * @param userId 用户 ID
     * @return 权限判断结果
     */
    @Override
    public JSONObject checkUserSpaceManagePermission(Long userId, Long id) {
        JSONObject jsonObject = new JSONObject();
        QueryWrapper<TaxonomySpaceDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ID", id).eq("MANAGER_ID", userId);
        TaxonomySpaceDO spaceDO = taxonomySpaceMapper.selectOne(queryWrapper);
        jsonObject.set("isManagerId", spaceDO != null);
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
     * 查询当前用户所属的空间列表
     *
     * @param userId 用户id
     * @return 空间列表
     */
    @Override
    public List<TaxonomySpaceDO> getCurrentUserSpaceList(Long userId) {
        if (userId == 1) {
            return taxonomySpaceMapper.selectList();
        }
        QueryWrapper<TaxonomySpaceUserRelDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<TaxonomySpaceUserRelDO> userRelDOList = taxonomySpaceUserRelMapper.selectList(queryWrapper);
        List<Long> spaceIds = userRelDOList.stream()
                .map(TaxonomySpaceUserRelDO::getSpaceId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(spaceIds)) {
            return new ArrayList<>();
        }
        QueryWrapper<TaxonomySpaceDO> spaceWrapper = new QueryWrapper<>();
        spaceWrapper.in(!CollectionUtils.isEmpty(spaceIds), "id", spaceIds).eq("valid_flag", "1");
        return taxonomySpaceMapper.selectList(spaceWrapper);
    }

    /**
     * 查询尚未加入当前空间的用户列表
     */
    @Override
    public List<SysUser> selectUsersNotInSpace(TaxonomySysUserReqVO user) {
        QueryWrapper<TaxonomySpaceUserRelDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("SPACE_ID", user.getSpaceId());
        List<TaxonomySpaceUserRelDO> spaceUserRelDOList = taxonomySpaceUserRelMapper.selectList(queryWrapper);
        List<Long> userIdList = spaceUserRelDOList.stream()
                .map(TaxonomySpaceUserRelDO::getUserId)
                .collect(Collectors.toList());
        TaxonomySpaceDO spaceDO = taxonomySpaceMapper.selectById(user.getSpaceId());
        userIdList.add(spaceDO.getManagerId());
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
    public Boolean updateSpaceStatus(Long id, Long status) {
        return this.update(Wrappers.lambdaUpdate(TaxonomySpaceDO.class)
                .eq(TaxonomySpaceDO::getId, id)
                .set(TaxonomySpaceDO::getValidFlag, status));
    }

    @Override
    public Long getSpaceIdBySpaceCode(String spaceCode) {
        TaxonomySpaceDO spaceDO = baseMapper.selectOne(Wrappers.lambdaQuery(TaxonomySpaceDO.class)
                .eq(TaxonomySpaceDO::getCode, spaceCode));
        if (spaceDO != null) {
            return spaceDO.getId();
        }
        return null;
    }

    @Override
    public String getSpaceCodeBySpaceId(Long spaceId) {
        TaxonomySpaceDO spaceDO = baseMapper.selectById(spaceId);
        return spaceDO == null ? null : spaceDO.getCode();
    }

    @Override
    public String getWorkerGroupBySpaceCode(String spaceCode) {
        TaxonomySpaceDO spaceDO = baseMapper.selectOne(Wrappers.lambdaQuery(TaxonomySpaceDO.class)
                .eq(TaxonomySpaceDO::getCode, spaceCode));
        return spaceDO == null ? null : spaceDO.getWorkerGroup();
    }

    private String buildWorkerGroupName(Long spaceCode) {
        return "space_" + spaceCode;
    }

    private boolean isSuccess(DsResultDTO result) {
        return result != null && Integer.valueOf(0).equals(result.getCode());
    }

    private void cleanupDsResources(Long spaceCode, Integer workerGroupId) {
        try {
            dsProjectService.deleteProject(spaceCode);
        } catch (Exception e) {
            log.warn("Failed to clean up DS space, spaceCode={}", spaceCode, e);
        }
        if (workerGroupId == null) {
            return;
        }
        try {
            dsProjectService.deleteWorkerGroup(workerGroupId);
        } catch (Exception e) {
            log.warn("Failed to clean up DS worker group, workerGroupId={}", workerGroupId, e);
        }
    }
}
