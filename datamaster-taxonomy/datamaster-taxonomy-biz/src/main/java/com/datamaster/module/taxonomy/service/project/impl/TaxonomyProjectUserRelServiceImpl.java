

package com.datamaster.module.taxonomy.service.project.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.core.domain.entity.SysRole;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectUserRelDO;
import com.datamaster.module.taxonomy.dal.mapper.project.TaxonomyProjectUserRelMapper;
import com.datamaster.module.taxonomy.service.project.ITaxonomyProjectUserRelService;
import com.datamaster.module.system.domain.SysUserRole;
import com.datamaster.module.system.mapper.SysRoleMapper;
import com.datamaster.module.system.mapper.SysUserMapper;
import com.datamaster.module.system.mapper.SysUserRoleMapper;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目与用户关联关系Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyProjectUserRelServiceImpl extends ServiceImpl<TaxonomyProjectUserRelMapper, TaxonomyProjectUserRelDO> implements ITaxonomyProjectUserRelService {
    @Resource
    private TaxonomyProjectUserRelMapper TaxonomyProjectUserRelMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public PageResult<TaxonomyProjectUserRelDO> getAttProjectUserRelPage(TaxonomyProjectUserRelPageReqVO pageReqVO) {
        return TaxonomyProjectUserRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttProjectUserRel(TaxonomyProjectUserRelSaveReqVO createReqVO) {
        TaxonomyProjectUserRelDO dictType = BeanUtils.toBean(createReqVO, TaxonomyProjectUserRelDO.class);
        TaxonomyProjectUserRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttProjectUserRel(TaxonomyProjectUserRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新项目与用户关联关系
        TaxonomyProjectUserRelDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyProjectUserRelDO.class);
        return TaxonomyProjectUserRelMapper.updateById(updateObj);
    }

    @Override
    public int updateUserListAndRoleList(TaxonomyProjectUserRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新项目与用户关联关系
        SysRole sysRole = new SysRole();
        sysRole.setProjectId(updateReqVO.getProjectId());
        List<SysRole> sysRoleList = sysRoleMapper.selectRoleList(sysRole);

        List<SysUserRole> sysUserRoleList = new ArrayList<>();
        for (SysRole role : sysRoleList) {
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(updateReqVO.getUserId());
            sysUserRole.setRoleId(role.getRoleId());
            sysUserRoleList.add(sysUserRole);
        }
        sysUserRoleMapper.deleteUserRoleList(sysUserRoleList);

        List<SysUserRole> userRoleList = new ArrayList<>();
        for (Long roleId : updateReqVO.getRoleIdList()) {
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(updateReqVO.getUserId());
            sysUserRole.setRoleId(roleId);
            userRoleList.add(sysUserRole);
        }
        if (!userRoleList.isEmpty()){
            sysUserRoleMapper.batchUserRole(userRoleList);
        }

        TaxonomyProjectUserRelDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyProjectUserRelDO.class);
        return TaxonomyProjectUserRelMapper.updateById(updateObj);
    }

    @Override
    public int removeAttProjectUserRel(Collection<Long> idList) {
        QueryWrapper<TaxonomyProjectUserRelDO> projectWrapper = new QueryWrapper<>();
        projectWrapper.in(!CollectionUtils.isEmpty(idList), "id", idList);
        List<TaxonomyProjectUserRelDO> TaxonomyProjectUserRelDOList = TaxonomyProjectUserRelMapper.selectList(projectWrapper);
        List<Long> userId = TaxonomyProjectUserRelDOList.stream().map(TaxonomyProjectUserRelDO::getUserId).collect(Collectors.toList());
        List<SysUserRole> byUserIdList = sysUserRoleMapper.getByUserIdList(userId);
        SysRole sysRole = new SysRole();
        Long projectId = TaxonomyProjectUserRelDOList.get(0) != null ? TaxonomyProjectUserRelDOList.get(0).getProjectId() : -999;
        sysRole.setProjectId(projectId);
        List<SysRole> sysRoleList = sysRoleMapper.selectRoleList(sysRole);
        List<Long> roleIdList = sysRoleList.stream().map(SysRole::getRoleId).collect(Collectors.toList());
        List<SysUserRole> userRoleList = new ArrayList<>();
        for (SysUserRole sysUserRole : byUserIdList) {
            if (roleIdList.contains(sysUserRole.getRoleId())) {
                userRoleList.add(sysUserRole);
            }
        }
        if (!userRoleList.isEmpty()){
            sysUserRoleMapper.deleteUserRoleList(userRoleList);
        }
        // 批量删除项目与用户关联关系
        return TaxonomyProjectUserRelMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyProjectUserRelDO getAttProjectUserRelById(Long id) {
        return TaxonomyProjectUserRelMapper.selectById(id);
    }

    @Override
    public List<TaxonomyProjectUserRelDO> getAttProjectUserRelList() {
        return TaxonomyProjectUserRelMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyProjectUserRelDO> getAttProjectUserRelMap() {
        List<TaxonomyProjectUserRelDO> TaxonomyProjectUserRelList = TaxonomyProjectUserRelMapper.selectList();
        return TaxonomyProjectUserRelList.stream()
                .collect(Collectors.toMap(
                        TaxonomyProjectUserRelDO::getId,
                        TaxonomyProjectUserRelDO -> TaxonomyProjectUserRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入项目与用户关联关系数据
     *
     * @param importExcelList 项目与用户关联关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttProjectUserRel(List<TaxonomyProjectUserRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyProjectUserRelRespVO respVO : importExcelList) {
            try {
                TaxonomyProjectUserRelDO TaxonomyProjectUserRelDO = BeanUtils.toBean(respVO, TaxonomyProjectUserRelDO.class);
                Long TaxonomyProjectUserRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyProjectUserRelId != null) {
                        TaxonomyProjectUserRelDO existingAttProjectUserRel = TaxonomyProjectUserRelMapper.selectById(TaxonomyProjectUserRelId);
                        if (existingAttProjectUserRel != null) {
                            TaxonomyProjectUserRelMapper.updateById(TaxonomyProjectUserRelDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyProjectUserRelId + " 的项目与用户关联关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyProjectUserRelId + " 的项目与用户关联关系记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyProjectUserRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyProjectUserRelId);
                    TaxonomyProjectUserRelDO existingAttProjectUserRel = TaxonomyProjectUserRelMapper.selectOne(queryWrapper);
                    if (existingAttProjectUserRel == null) {
                        TaxonomyProjectUserRelMapper.insert(TaxonomyProjectUserRelDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyProjectUserRelId + " 的项目与用户关联关系记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyProjectUserRelId + " 的项目与用户关联关系记录已存在。");
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
     * 创建项目前端传用户集合和角色集合
     *
     * @param TaxonomyProject 项目信息
     * @return 项目编号
     */
    @Override
    public Boolean createUserListAndRoleList(TaxonomyProjectUserRelSaveReqVO TaxonomyProject) {
        List<TaxonomyProjectUserRelDO> TaxonomyProjectUserRelDOList = new ArrayList<>();
        List<SysUserRole> sysUserRoleList = new ArrayList<>();
        for (Long userId : TaxonomyProject.getUserIdList()) {
            TaxonomyProjectUserRelDO TaxonomyProjectUserRelDO = new TaxonomyProjectUserRelDO();
            TaxonomyProjectUserRelDO.setUserId(userId);
            TaxonomyProjectUserRelDO.setProjectId(TaxonomyProject.getProjectId());
            TaxonomyProjectUserRelDOList.add(TaxonomyProjectUserRelDO);
            for (Long roleId : TaxonomyProject.getRoleIdList()) {
                SysUserRole sysUserRole = new SysUserRole();
                sysUserRole.setRoleId(roleId);
                sysUserRole.setUserId(userId);
                sysUserRoleList.add(sysUserRole);
            }
        }
        Boolean aBoolean = TaxonomyProjectUserRelMapper.insertBatch(TaxonomyProjectUserRelDOList);
        int i = sysUserRoleMapper.batchUserRole(sysUserRoleList);
        return aBoolean && i != -1;
    }

    /**
     * 获取项目与用户关联关系详细信息包括角色信息
     *
     * @param id
     * @return
     */
    @Override
    public TaxonomyProjectUserRelRespVO getRoleUser(Long id) {
        TaxonomyProjectUserRelDO TaxonomyProjectUserRelDO = TaxonomyProjectUserRelMapper.selectById(id);
        SysUser sysUser = sysUserMapper.selectUserById(TaxonomyProjectUserRelDO.getUserId());
        TaxonomyProjectUserRelDO.setUserName(sysUser.getUserName());
        TaxonomyProjectUserRelDO.setNickName(sysUser.getNickName());
        TaxonomyProjectUserRelDO.setPhoneNumber(sysUser.getPhonenumber());
        List<SysUserRole> userRoleList = sysUserRoleMapper.getUserRoleByRoleId(TaxonomyProjectUserRelDO.getUserId());
        List<Long> roleIdList = userRoleList.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        SysRole sysRole = new SysRole();
        sysRole.setProjectId(TaxonomyProjectUserRelDO.getProjectId());
        List<SysRole> sysRoleList = sysRoleMapper.selectRoleList(sysRole);
        Set<Long> roleSet = new HashSet<>();
        for (SysRole role : sysRoleList) {
            if (roleIdList.contains(role.getRoleId())) {
                roleSet.add(role.getRoleId());
            }
        }
        TaxonomyProjectUserRelRespVO TaxonomyProjectUserRelRespVO = BeanUtils.toBean(TaxonomyProjectUserRelDO, TaxonomyProjectUserRelRespVO.class);
        TaxonomyProjectUserRelRespVO.setRoleIdList(roleSet.stream().collect(Collectors.toList()));
        return TaxonomyProjectUserRelRespVO;
    }
}
