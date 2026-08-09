

package com.datamaster.module.collector.service.etl.impl;

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlSchedulerMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlSchedulerService;

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
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlSchedulerServiceImpl  extends ServiceImpl<CollectorEtlSchedulerMapper,CollectorEtlSchedulerDO> implements ICollectorEtlSchedulerService {
    @Resource
    private CollectorEtlSchedulerMapper CollectorEtlSchedulerMapper;

    @Override
    public PageResult<CollectorEtlSchedulerDO> getCollectorEtlSchedulerPage(CollectorEtlSchedulerPageReqVO pageReqVO) {
        return CollectorEtlSchedulerMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO createReqVO) {
        CollectorEtlSchedulerDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlSchedulerDO.class);
        CollectorEtlSchedulerMapper.insert(dictType);
        return dictType.getId();
    }


    @Override
    public CollectorEtlSchedulerDO createCollectorEtlSchedulerNew(CollectorEtlSchedulerSaveReqVO createReqVO) {
        CollectorEtlSchedulerDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlSchedulerDO.class);
        CollectorEtlSchedulerMapper.insert(dictType);
        return dictType;
    }

    @Override
    public int updateCollectorEtlScheduler(CollectorEtlSchedulerSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成调度信息
        CollectorEtlSchedulerDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlSchedulerDO.class);
        return CollectorEtlSchedulerMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorEtlScheduler(Collection<Long> idList) {
        // 批量删除数据集成调度信息
        return CollectorEtlSchedulerMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlSchedulerDO getCollectorEtlSchedulerById(Long id) {
        return CollectorEtlSchedulerMapper.selectById(id);
    }

    @Override
    public CollectorEtlSchedulerDO getCollectorEtlSchedulerById(CollectorEtlSchedulerPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlSchedulerDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlSchedulerDO.class)
                .eq(reqVO.getTaskId() != null, CollectorEtlSchedulerDO::getTaskId, reqVO.getTaskId())
                .eq(reqVO.getTaskCode() != null, CollectorEtlSchedulerDO::getTaskCode, reqVO.getTaskCode())
                .eq(reqVO.getDsId() != null, CollectorEtlSchedulerDO::getDsId, reqVO.getDsId())
                .eq(reqVO.getId() != null, CollectorEtlSchedulerDO::getId, reqVO.getId())
                .eq(StringUtils.isNotBlank(reqVO.getTaskCode()), CollectorEtlSchedulerDO::getTaskCode, reqVO.getTaskCode());
        return CollectorEtlSchedulerMapper.selectOne(wrapper);
    }

    @Override
    public List<CollectorEtlSchedulerDO> getCollectorEtlSchedulerList() {
        return CollectorEtlSchedulerMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlSchedulerDO> getCollectorEtlSchedulerMap() {
        List<CollectorEtlSchedulerDO> CollectorEtlSchedulerList = CollectorEtlSchedulerMapper.selectList();
        return CollectorEtlSchedulerList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlSchedulerDO::getId,
                        CollectorEtlSchedulerDO -> CollectorEtlSchedulerDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据集成调度信息数据
         *
         * @param importExcelList 数据集成调度信息数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importCollectorEtlScheduler(List<CollectorEtlSchedulerRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorEtlSchedulerRespVO respVO : importExcelList) {
                try {
                    CollectorEtlSchedulerDO CollectorEtlSchedulerDO = BeanUtils.toBean(respVO, CollectorEtlSchedulerDO.class);
                    Long CollectorEtlSchedulerId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorEtlSchedulerId != null) {
                            CollectorEtlSchedulerDO existingCollectorEtlScheduler = CollectorEtlSchedulerMapper.selectById(CollectorEtlSchedulerId);
                            if (existingCollectorEtlScheduler != null) {
                                CollectorEtlSchedulerMapper.updateById(CollectorEtlSchedulerDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorEtlSchedulerId + " 的数据集成调度信息记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorEtlSchedulerId + " 的数据集成调度信息记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorEtlSchedulerDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorEtlSchedulerId);
                        CollectorEtlSchedulerDO existingCollectorEtlScheduler = CollectorEtlSchedulerMapper.selectOne(queryWrapper);
                        if (existingCollectorEtlScheduler == null) {
                            CollectorEtlSchedulerMapper.insert(CollectorEtlSchedulerDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorEtlSchedulerId + " 的数据集成调度信息记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorEtlSchedulerId + " 的数据集成调度信息记录已存在。");
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
