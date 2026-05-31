package com.datamaster.module.catalog.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstancePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceDO;
import com.datamaster.module.catalog.dal.mapper.task.CatalogTaskInstanceMapper;
import com.datamaster.module.catalog.service.task.ICatalogTaskInstanceService;
import com.datamaster.module.system.service.ISysUserService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采集任务实例Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskInstanceServiceImpl extends ServiceImpl<CatalogTaskInstanceMapper, CatalogTaskInstanceDO> implements ICatalogTaskInstanceService {
    @Resource
    private CatalogTaskInstanceMapper CatalogTaskInstanceMapper;
    @Resource
    private ISysUserService sysUserService;

    @Override
    public PageResult<CatalogTaskInstanceDO> getCatalogTaskInstancePage(CatalogTaskInstancePageReqVO pageReqVO) {
        PageResult<CatalogTaskInstanceDO> CatalogTaskInstanceDOPageResult = CatalogTaskInstanceMapper.selectPage(pageReqVO);
        List<CatalogTaskInstanceDO> rows = CatalogTaskInstanceDOPageResult.getRows();

        if (CollectionUtils.isEmpty(rows)) {
            return CatalogTaskInstanceDOPageResult;
        }

        // FIXME(用户查询避免循环查询，临时方案)  使用 Map 缓存用户信息,避免重复查询
        Map<Long, SysUser> userCache = Maps.newHashMap();
        for (CatalogTaskInstanceDO row : rows) {
            // 获取创建人手机号
            Long creatorId = row.getCreatorId();
            if (creatorId != null && !userCache.containsKey(creatorId)) {
                SysUser sysUser = sysUserService.selectUserById(creatorId);
                if (sysUser != null) {
                    userCache.put(creatorId, sysUser);
                }
            }
            SysUser creatorUser = userCache.get(creatorId);
            if (creatorUser != null) {
                row.setCreatePhoneNumber(creatorUser.getPhonenumber());
            }

        }
        CatalogTaskInstanceDOPageResult.setRows(rows);
        return CatalogTaskInstanceDOPageResult;
    }

    @Override
    public Long createCatalogTaskInstance(CatalogTaskInstanceSaveReqVO createReqVO) {
        CatalogTaskInstanceDO dictType = BeanUtils.toBean(createReqVO, CatalogTaskInstanceDO.class);
        CatalogTaskInstanceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public Long createCatalogTaskInstance(CatalogTaskInstanceDO createReqVO) {
        CatalogTaskInstanceMapper.insert(createReqVO);
        return createReqVO.getId();
    }

    @Override
    public int updateCatalogTaskInstance(CatalogTaskInstanceSaveReqVO updateReqVO) {
        // 相关校验

        // 更新采集任务实例
        CatalogTaskInstanceDO updateObj = BeanUtils.toBean(updateReqVO, CatalogTaskInstanceDO.class);
        return CatalogTaskInstanceMapper.updateById(updateObj);
    }

    @Override
    public int removeCatalogTaskInstance(Collection<Long> idList) {
        // 批量删除采集任务实例
        return CatalogTaskInstanceMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogTaskInstanceDO getCatalogTaskInstanceById(Long id) {
        return CatalogTaskInstanceMapper.selectById(id);
    }

    @Override
    public CatalogTaskInstanceDO getCatalogTaskInstanceByTaskId(Long taskId) {
        MPJLambdaWrapper<CatalogTaskInstanceDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogTaskInstanceDO::getTaskId, taskId)
                .orderByDesc(CatalogTaskInstanceDO::getCreateTime);

        // 只取第 1 页、1 条
        Page<CatalogTaskInstanceDO> page = new Page<>(1, 1);
        Page<CatalogTaskInstanceDO> result = CatalogTaskInstanceMapper.selectPage(page, wrapper);

        return result.getRecords().isEmpty() ? null : result.getRecords().get(0);
    }

    @Override
    public List<CatalogTaskInstanceDO> getCatalogTaskInstanceList() {
        return CatalogTaskInstanceMapper.selectList();
    }

    @Override
    public Map<Long, CatalogTaskInstanceDO> getCatalogTaskInstanceMap() {
        List<CatalogTaskInstanceDO> CatalogTaskInstanceList = CatalogTaskInstanceMapper.selectList();
        return CatalogTaskInstanceList.stream()
                .collect(Collectors.toMap(
                        CatalogTaskInstanceDO::getId,
                        CatalogTaskInstanceDO -> CatalogTaskInstanceDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入采集任务实例数据
     *
     * @param importExcelList 采集任务实例数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCatalogTaskInstance(List<CatalogTaskInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CatalogTaskInstanceRespVO respVO : importExcelList) {
            try {
                CatalogTaskInstanceDO CatalogTaskInstanceDO = BeanUtils.toBean(respVO, CatalogTaskInstanceDO.class);
                Long CatalogTaskInstanceId = respVO.getId();
                if (isUpdateSupport) {
                    if (CatalogTaskInstanceId != null) {
                        CatalogTaskInstanceDO existingCatalogTaskInstance = CatalogTaskInstanceMapper.selectById(CatalogTaskInstanceId);
                        if (existingCatalogTaskInstance != null) {
                            CatalogTaskInstanceMapper.updateById(CatalogTaskInstanceDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CatalogTaskInstanceId + " 的采集任务实例记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CatalogTaskInstanceId + " 的采集任务实例记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CatalogTaskInstanceDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CatalogTaskInstanceId);
                    CatalogTaskInstanceDO existingCatalogTaskInstance = CatalogTaskInstanceMapper.selectOne(queryWrapper);
                    if (existingCatalogTaskInstance == null) {
                        CatalogTaskInstanceMapper.insert(CatalogTaskInstanceDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CatalogTaskInstanceId + " 的采集任务实例记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CatalogTaskInstanceId + " 的采集任务实例记录已存在。");
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
}
