

package com.datamaster.module.taxonomy.service.space.impl;

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
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.space.vo.TaxonomySpaceUserRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.space.TaxonomySpaceUserRelDO;
import com.datamaster.module.taxonomy.dal.mapper.space.TaxonomySpaceUserRelMapper;
import com.datamaster.module.taxonomy.service.space.ITaxonomySpaceUserRelService;
import com.datamaster.module.system.domain.SysUserRole;
import com.datamaster.module.system.mapper.SysRoleMapper;
import com.datamaster.module.system.mapper.SysUserMapper;
import com.datamaster.module.system.mapper.SysUserRoleMapper;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 空间成员关系 Service 业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomySpaceUserRelServiceImpl extends ServiceImpl<TaxonomySpaceUserRelMapper, TaxonomySpaceUserRelDO> implements ITaxonomySpaceUserRelService {
    @Resource
    private TaxonomySpaceUserRelMapper taxonomySpaceUserRelMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public PageResult<TaxonomySpaceUserRelDO> getSpaceUserRelPage(TaxonomySpaceUserRelPageReqVO pageReqVO) {
        return taxonomySpaceUserRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createSpaceUserRel(TaxonomySpaceUserRelSaveReqVO createReqVO) {
        TaxonomySpaceUserRelDO dictType = BeanUtils.toBean(createReqVO, TaxonomySpaceUserRelDO.class);
        taxonomySpaceUserRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateSpaceUserRel(TaxonomySpaceUserRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新空间成员关系
        TaxonomySpaceUserRelDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomySpaceUserRelDO.class);
        return taxonomySpaceUserRelMapper.updateById(updateObj);
    }

    @Override
    public int updateSpaceUsersAndRoles(TaxonomySpaceUserRelSaveReqVO updateReqVO) {
        // 相关校验

        List<Long> spaceRoleIds = selectSpaceRoles(updateReqVO.getSpaceId()).stream()
                .map(SysRole::getRoleId)
                .collect(Collectors.toList());
        List<SysUserRole> spaceUserRoles = buildUserRoleList(Collections.singletonList(updateReqVO.getUserId()), spaceRoleIds);
        sysUserRoleMapper.deleteUserRoleList(spaceUserRoles);

        List<SysUserRole> userRoleList = buildUserRoleList(Collections.singletonList(updateReqVO.getUserId()), updateReqVO.getRoleIdList());
        if (!userRoleList.isEmpty()){
            sysUserRoleMapper.batchUserRole(userRoleList);
        }

        TaxonomySpaceUserRelDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomySpaceUserRelDO.class);
        return taxonomySpaceUserRelMapper.updateById(updateObj);
    }

    @Override
    public int removeSpaceUserRel(Collection<Long> idList) {
        QueryWrapper<TaxonomySpaceUserRelDO> spaceWrapper = new QueryWrapper<>();
        spaceWrapper.in(!CollectionUtils.isEmpty(idList), "id", idList);
        List<TaxonomySpaceUserRelDO> spaceUserRelList = taxonomySpaceUserRelMapper.selectList(spaceWrapper);
        List<Long> userIdList = spaceUserRelList.stream().map(TaxonomySpaceUserRelDO::getUserId).collect(Collectors.toList());
        List<SysUserRole> byUserIdList = sysUserRoleMapper.getByUserIdList(userIdList);
        Long spaceId = spaceUserRelList.get(0) != null ? spaceUserRelList.get(0).getSpaceId() : -999;
        List<Long> roleIdList = selectSpaceRoles(spaceId).stream().map(SysRole::getRoleId).collect(Collectors.toList());
        List<SysUserRole> userRoleList = new ArrayList<>();
        for (SysUserRole sysUserRole : byUserIdList) {
            if (roleIdList.contains(sysUserRole.getRoleId())) {
                userRoleList.add(sysUserRole);
            }
        }
        if (!userRoleList.isEmpty()){
            sysUserRoleMapper.deleteUserRoleList(userRoleList);
        }
        // 批量删除空间成员关系
        return taxonomySpaceUserRelMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomySpaceUserRelDO getSpaceUserRelById(Long id) {
        return taxonomySpaceUserRelMapper.selectById(id);
    }

    @Override
    public List<TaxonomySpaceUserRelDO> getSpaceUserRelList() {
        return taxonomySpaceUserRelMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomySpaceUserRelDO> getSpaceUserRelMap() {
        List<TaxonomySpaceUserRelDO> spaceUserRelList = taxonomySpaceUserRelMapper.selectList();
        return spaceUserRelList.stream()
                .collect(Collectors.toMap(
                        TaxonomySpaceUserRelDO::getId,
                        spaceUserRelDO -> spaceUserRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入空间成员关系数据
     *
     * @param importExcelList 空间成员关系数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importSpaceUserRel(List<TaxonomySpaceUserRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomySpaceUserRelRespVO respVO : importExcelList) {
            try {
                TaxonomySpaceUserRelDO spaceUserRelDO = BeanUtils.toBean(respVO, TaxonomySpaceUserRelDO.class);
                Long spaceUserRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (spaceUserRelId != null) {
                        TaxonomySpaceUserRelDO existingSpaceUserRel = taxonomySpaceUserRelMapper.selectById(spaceUserRelId);
                        if (existingSpaceUserRel != null) {
                            taxonomySpaceUserRelMapper.updateById(spaceUserRelDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + spaceUserRelId + " 的空间成员关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + spaceUserRelId + " 的空间成员关系记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的 ID 不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomySpaceUserRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", spaceUserRelId);
                    TaxonomySpaceUserRelDO existingSpaceUserRel = taxonomySpaceUserRelMapper.selectOne(queryWrapper);
                    if (existingSpaceUserRel == null) {
                        taxonomySpaceUserRelMapper.insert(spaceUserRelDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + spaceUserRelId + " 的空间成员关系记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + spaceUserRelId + " 的空间成员关系记录已存在。");
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
     * 批量创建空间成员与角色关系
     *
     * @param saveReqVO 空间成员与角色信息
     * @return 是否创建成功
     */
    @Override
    public Boolean createSpaceUsersAndRoles(TaxonomySpaceUserRelSaveReqVO saveReqVO) {
        List<TaxonomySpaceUserRelDO> spaceUserRelList = new ArrayList<>();
        for (Long userId : saveReqVO.getUserIdList()) {
            TaxonomySpaceUserRelDO spaceUserRelDO = new TaxonomySpaceUserRelDO();
            spaceUserRelDO.setUserId(userId);
            spaceUserRelDO.setSpaceId(saveReqVO.getSpaceId());
            spaceUserRelList.add(spaceUserRelDO);
        }
        List<SysUserRole> sysUserRoleList = buildUserRoleList(saveReqVO.getUserIdList(), saveReqVO.getRoleIdList());
        Boolean aBoolean = taxonomySpaceUserRelMapper.insertBatch(spaceUserRelList);
        int i = sysUserRoleMapper.batchUserRole(sysUserRoleList);
        return aBoolean && i != -1;
    }

    /**
     * 获取空间成员详情，包括角色信息
     *
     * @param id 空间成员关系 ID
     * @return 空间成员详情
     */
    @Override
    public TaxonomySpaceUserRelRespVO getSpaceUserRoleDetail(Long id) {
        TaxonomySpaceUserRelDO spaceUserRelDO = taxonomySpaceUserRelMapper.selectById(id);
        SysUser sysUser = sysUserMapper.selectUserById(spaceUserRelDO.getUserId());
        spaceUserRelDO.setUserName(sysUser.getUserName());
        spaceUserRelDO.setNickName(sysUser.getNickName());
        spaceUserRelDO.setPhoneNumber(sysUser.getPhonenumber());
        List<SysUserRole> userRoleList = sysUserRoleMapper.getUserRoleByRoleId(spaceUserRelDO.getUserId());
        List<Long> roleIdList = userRoleList.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> sysRoleList = selectSpaceRoles(spaceUserRelDO.getSpaceId());
        Set<Long> roleSet = new HashSet<>();
        for (SysRole role : sysRoleList) {
            if (roleIdList.contains(role.getRoleId())) {
                roleSet.add(role.getRoleId());
            }
        }
        TaxonomySpaceUserRelRespVO respVO = BeanUtils.toBean(spaceUserRelDO, TaxonomySpaceUserRelRespVO.class);
        respVO.setRoleIdList(roleSet.stream().collect(Collectors.toList()));
        return respVO;
    }

    private List<SysRole> selectSpaceRoles(Long spaceId) {
        SysRole sysRole = new SysRole();
        sysRole.setSpaceId(spaceId);
        return sysRoleMapper.selectRoleList(sysRole);
    }

    private List<SysUserRole> buildUserRoleList(Collection<Long> userIds, Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(userIds) || CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<SysUserRole> userRoleList = new ArrayList<>();
        for (Long userId : userIds) {
            for (Long roleId : roleIds) {
                SysUserRole sysUserRole = new SysUserRole();
                sysUserRole.setUserId(userId);
                sysUserRole.setRoleId(roleId);
                userRoleList.add(sysUserRole);
            }
        }
        return userRoleList;
    }
}
