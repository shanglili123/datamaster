package com.datamaster.module.catalog.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskSchedulerSaveReqVO;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskSchedulerDO;
import com.datamaster.module.catalog.dal.mapper.task.CatalogTaskSchedulerMapper;
import com.datamaster.module.catalog.service.task.ICatalogTaskSchedulerService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集成调度信息Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CatalogTaskSchedulerServiceImpl extends ServiceImpl<CatalogTaskSchedulerMapper, CatalogTaskSchedulerDO> implements ICatalogTaskSchedulerService {
    @Resource
    private CatalogTaskSchedulerMapper CatalogTaskSchedulerMapper;

    @Override
    public PageResult<CatalogTaskSchedulerDO> getCatalogTaskSchedulerPage(CatalogTaskSchedulerPageReqVO pageReqVO) {
        return CatalogTaskSchedulerMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCatalogTaskScheduler(CatalogTaskSchedulerSaveReqVO createReqVO) {
        CatalogTaskSchedulerDO dictType = BeanUtils.toBean(createReqVO, CatalogTaskSchedulerDO.class);
        if(StringUtils.isEmpty(dictType.getStatus())){
            dictType.setStatus("1");
        }
        CatalogTaskSchedulerMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCatalogTaskScheduler(CatalogTaskSchedulerSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成调度信息
        CatalogTaskSchedulerDO updateObj = BeanUtils.toBean(updateReqVO, CatalogTaskSchedulerDO.class);
        return CatalogTaskSchedulerMapper.updateById(updateObj);
    }

    @Override
    public int removeCatalogTaskScheduler(Collection<Long> idList) {
        // 批量删除数据集成调度信息
        return CatalogTaskSchedulerMapper.deleteBatchIds(idList);
    }

    @Override
    public CatalogTaskSchedulerDO getCatalogTaskSchedulerById(Long id) {
        return CatalogTaskSchedulerMapper.selectById(id);
    }

    @Override
    public CatalogTaskSchedulerDO getCatalogTaskSchedulerBytaskId(Long taskId) {
        MPJLambdaWrapper<CatalogTaskSchedulerDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.eq(CatalogTaskSchedulerDO::getTaskId, taskId)
                .eq(CatalogTaskSchedulerDO::getDelFlag, "0")
                .orderByAsc(CatalogTaskSchedulerDO::getCreateTime);
        List<CatalogTaskSchedulerDO> CatalogTaskSchedulerDOS = CatalogTaskSchedulerMapper.selectList(wrapper);
        return CollectionUtils.isEmpty(CatalogTaskSchedulerDOS) ? null : CatalogTaskSchedulerDOS.get(0);
    }

    @Override
    public List<CatalogTaskSchedulerDO> getCatalogTaskSchedulerList() {
        return CatalogTaskSchedulerMapper.selectList();
    }

    @Override
    public Map<Long, CatalogTaskSchedulerDO> getCatalogTaskSchedulerMap() {
        List<CatalogTaskSchedulerDO> CatalogTaskSchedulerList = CatalogTaskSchedulerMapper.selectList();
        return CatalogTaskSchedulerList.stream()
                .collect(Collectors.toMap(
                        CatalogTaskSchedulerDO::getId,
                        CatalogTaskSchedulerDO -> CatalogTaskSchedulerDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成调度信息数据
     *
     * @param importExcelList 数据集成调度信息数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCatalogTaskScheduler(List<CatalogTaskSchedulerRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CatalogTaskSchedulerRespVO respVO : importExcelList) {
            try {
                CatalogTaskSchedulerDO CatalogTaskSchedulerDO = BeanUtils.toBean(respVO, CatalogTaskSchedulerDO.class);
                Long CatalogTaskSchedulerId = respVO.getId();
                if (isUpdateSupport) {
                    if (CatalogTaskSchedulerId != null) {
                        CatalogTaskSchedulerDO existingCatalogTaskScheduler = CatalogTaskSchedulerMapper.selectById(CatalogTaskSchedulerId);
                        if (existingCatalogTaskScheduler != null) {
                            CatalogTaskSchedulerMapper.updateById(CatalogTaskSchedulerDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CatalogTaskSchedulerId + " 的数据集成调度信息记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CatalogTaskSchedulerId + " 的数据集成调度信息记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CatalogTaskSchedulerDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CatalogTaskSchedulerId);
                    CatalogTaskSchedulerDO existingCatalogTaskScheduler = CatalogTaskSchedulerMapper.selectOne(queryWrapper);
                    if (existingCatalogTaskScheduler == null) {
                        CatalogTaskSchedulerMapper.insert(CatalogTaskSchedulerDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CatalogTaskSchedulerId + " 的数据集成调度信息记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CatalogTaskSchedulerId + " 的数据集成调度信息记录已存在。");
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
    public void updateReleaseSchedule(CatalogTaskSchedulerSaveReqVO CatalogTask) {
        LambdaUpdateWrapper<CatalogTaskSchedulerDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CatalogTaskSchedulerDO::getTaskId, CatalogTask.getTaskId())
                .set(CatalogTaskSchedulerDO::getStatus, CatalogTask.getStatus());

        CatalogTaskSchedulerMapper.update(null, wrapper);
    }
}
