package com.datamaster.module.catalog.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskScopeSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskScopeDO;
import com.datamaster.module.catalog.dal.mapper.task.CatalogTaskScopeMapper;
import com.datamaster.module.catalog.service.task.ICatalogTaskScopeService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采集范围Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskScopeServiceImpl extends ServiceImpl<CatalogTaskScopeMapper, CatalogTaskScopeDO> implements ICatalogTaskScopeService {
    @Resource
    private CatalogTaskScopeMapper CatalogTaskScopeMapper;

    @Override
    public PageResult<CatalogTaskScopeDO> getCatalogTaskScopePage(CatalogTaskScopePageReqVO pageReqVO) {
        return CatalogTaskScopeMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogTaskScope(CatalogTaskScopeSaveReqVO createReqVO) {
        CatalogTaskScopeDO dictType = BeanUtils.toBean(createReqVO, CatalogTaskScopeDO.class);
        CatalogTaskScopeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCatalogTaskScope(CatalogTaskScopeSaveReqVO updateReqVO) {
        // 相关校验

        // 更新采集范围
        CatalogTaskScopeDO updateObj = BeanUtils.toBean(updateReqVO, CatalogTaskScopeDO.class);
        return CatalogTaskScopeMapper.updateById(updateObj);
    }

    @Override
    public int removeCatalogTaskScope(Collection<Long> idList) {
        // 批量删除采集范围
        return CatalogTaskScopeMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogTaskScopeDO getCatalogTaskScopeById(Long id) {
        return CatalogTaskScopeMapper.selectById(id);
    }

    @Override
    public List<CatalogTaskScopeDO> getCatalogTaskScopeList() {
        return CatalogTaskScopeMapper.selectList();
    }

    @Override
    public Map<Long, CatalogTaskScopeDO> getCatalogTaskScopeMap() {
        List<CatalogTaskScopeDO> CatalogTaskScopeList = CatalogTaskScopeMapper.selectList();
        return CatalogTaskScopeList.stream()
                .collect(Collectors.toMap(
                        CatalogTaskScopeDO::getId,
                        CatalogTaskScopeDO -> CatalogTaskScopeDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入采集范围数据
     *
     * @param importExcelList 采集范围数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCatalogTaskScope(List<CatalogTaskScopeRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CatalogTaskScopeRespVO respVO : importExcelList) {
            try {
                CatalogTaskScopeDO CatalogTaskScopeDO = BeanUtils.toBean(respVO, CatalogTaskScopeDO.class);
                Long CatalogTaskScopeId = respVO.getId();
                if (isUpdateSupport) {
                    if (CatalogTaskScopeId != null) {
                        CatalogTaskScopeDO existingCatalogTaskScope = CatalogTaskScopeMapper.selectById(CatalogTaskScopeId);
                        if (existingCatalogTaskScope != null) {
                            CatalogTaskScopeMapper.updateById(CatalogTaskScopeDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CatalogTaskScopeId + " 的采集范围记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CatalogTaskScopeId + " 的采集范围记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CatalogTaskScopeDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CatalogTaskScopeId);
                    CatalogTaskScopeDO existingCatalogTaskScope = CatalogTaskScopeMapper.selectOne(queryWrapper);
                    if (existingCatalogTaskScope == null) {
                        CatalogTaskScopeMapper.insert(CatalogTaskScopeDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CatalogTaskScopeId + " 的采集范围记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CatalogTaskScopeId + " 的采集范围记录已存在。");
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

    @Override
    public void removeCatalogTaskScopeBytaskId(Long taskId) {
        MPJLambdaWrapper<CatalogTaskScopeDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogTaskScopeDO::getTaskId, taskId)
                .eq(CatalogTaskScopeDO::getDelFlag, "0");

        CatalogTaskScopeMapper.delete(wrapper);
    }

    @Override
    public List<CatalogTaskScopeDO> getCatalogTaskScopeListBytaskId(Long taskId) {
        MPJLambdaWrapper<CatalogTaskScopeDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogTaskScopeDO::getTaskId, taskId)
                .eq(CatalogTaskScopeDO::getDelFlag, "0");
        return CatalogTaskScopeMapper.selectList(wrapper);
    }
}
