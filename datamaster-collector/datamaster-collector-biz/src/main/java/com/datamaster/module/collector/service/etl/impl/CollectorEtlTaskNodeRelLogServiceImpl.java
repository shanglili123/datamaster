

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelLogDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskNodeRelLogMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskNodeRelLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据集成任务节点关系-日志Service业务层处理
 *
 * @author lili.shang
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskNodeRelLogServiceImpl  extends ServiceImpl<CollectorEtlTaskNodeRelLogMapper,CollectorEtlTaskNodeRelLogDO> implements ICollectorEtlTaskNodeRelLogService {
    @Resource
    private CollectorEtlTaskNodeRelLogMapper CollectorEtlTaskNodeRelLogMapper;

    @Override
    public PageResult<CollectorEtlTaskNodeRelLogDO> getCollectorEtlTaskNodeRelLogPage(CollectorEtlTaskNodeRelLogPageReqVO pageReqVO) {
        return CollectorEtlTaskNodeRelLogMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CollectorEtlTaskNodeRelLogRespVO> getCollectorEtlTaskNodeRelLogRespVOList(CollectorEtlTaskNodeRelLogPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlTaskNodeRelLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskNodeRelLogDO.class)
                .eq(reqVO.getTaskId() != null, CollectorEtlTaskNodeRelLogDO::getTaskId, reqVO.getTaskId())
                .eq(reqVO.getTaskVersion() != null, CollectorEtlTaskNodeRelLogDO::getTaskVersion, reqVO.getTaskVersion())
                .eq(StringUtils.isNotBlank(reqVO.getTaskCode()), CollectorEtlTaskNodeRelLogDO::getTaskCode, reqVO.getTaskCode());
        List<CollectorEtlTaskNodeRelLogDO> CollectorEtlTaskNodeRelDOS = CollectorEtlTaskNodeRelLogMapper.selectList(wrapper);
        return BeanUtils.toBean(CollectorEtlTaskNodeRelDOS, CollectorEtlTaskNodeRelLogRespVO.class);
    }


    @Override
    public CollectorEtlTaskNodeRelLogRespVO getCollectorEtlTaskNodeRelLogById(CollectorEtlTaskNodeRelLogPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlTaskNodeRelLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskNodeRelLogDO.class)
                .eq(reqVO.getTaskId() != null, CollectorEtlTaskNodeRelLogDO::getTaskId, reqVO.getTaskId())
                .eq(reqVO.getTaskVersion() != null, CollectorEtlTaskNodeRelLogDO::getTaskVersion, reqVO.getTaskVersion())
                .eq(StringUtils.isNotBlank(reqVO.getTaskCode()), CollectorEtlTaskNodeRelLogDO::getTaskCode, reqVO.getTaskCode());
        CollectorEtlTaskNodeRelLogDO CollectorEtlTaskNodeRelLogDO = CollectorEtlTaskNodeRelLogMapper.selectOne(wrapper);
        return BeanUtils.toBean(CollectorEtlTaskNodeRelLogDO, CollectorEtlTaskNodeRelLogRespVO.class);
    }

    @Override
    public Long createCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLogSaveReqVO createReqVO) {
        CollectorEtlTaskNodeRelLogDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlTaskNodeRelLogDO.class);
        dictType.setId(null);
        CollectorEtlTaskNodeRelLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public void createCollectorEtlTaskNodeRelLogBatch(List<CollectorEtlTaskNodeRelLogSaveReqVO> CollectorEtlTaskNodeRelLogSaveReqVOS) {
        if (CollectorEtlTaskNodeRelLogSaveReqVOS == null || CollectorEtlTaskNodeRelLogSaveReqVOS.isEmpty()) {
            return;
        }
        for (CollectorEtlTaskNodeRelLogSaveReqVO CollectorEtlTaskNodeRelLogSaveReqVO : CollectorEtlTaskNodeRelLogSaveReqVOS) {
            this.createCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLogSaveReqVO);
        }
    }

    @Override
    public int updateCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务节点关系-日志
        CollectorEtlTaskNodeRelLogDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskNodeRelLogDO.class);
        return CollectorEtlTaskNodeRelLogMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorEtlTaskNodeRelLog(Collection<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        // 批量删除数据集成任务节点关系-日志
        return CollectorEtlTaskNodeRelLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlTaskNodeRelLogDO getCollectorEtlTaskNodeRelLogById(Long id) {
        return CollectorEtlTaskNodeRelLogMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlTaskNodeRelLogDO> getCollectorEtlTaskNodeRelLogList() {
        return CollectorEtlTaskNodeRelLogMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlTaskNodeRelLogDO> getCollectorEtlTaskNodeRelLogMap() {
        List<CollectorEtlTaskNodeRelLogDO> CollectorEtlTaskNodeRelLogList = CollectorEtlTaskNodeRelLogMapper.selectList();
        return CollectorEtlTaskNodeRelLogList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlTaskNodeRelLogDO::getId,
                        CollectorEtlTaskNodeRelLogDO -> CollectorEtlTaskNodeRelLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据集成任务节点关系-日志数据
         *
         * @param importExcelList 数据集成任务节点关系-日志数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importCollectorEtlTaskNodeRelLog(List<CollectorEtlTaskNodeRelLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorEtlTaskNodeRelLogRespVO respVO : importExcelList) {
                try {
                    CollectorEtlTaskNodeRelLogDO CollectorEtlTaskNodeRelLogDO = BeanUtils.toBean(respVO, CollectorEtlTaskNodeRelLogDO.class);
                    Long CollectorEtlTaskNodeRelLogId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorEtlTaskNodeRelLogId != null) {
                            CollectorEtlTaskNodeRelLogDO existingCollectorEtlTaskNodeRelLog = CollectorEtlTaskNodeRelLogMapper.selectById(CollectorEtlTaskNodeRelLogId);
                            if (existingCollectorEtlTaskNodeRelLog != null) {
                                CollectorEtlTaskNodeRelLogMapper.updateById(CollectorEtlTaskNodeRelLogDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorEtlTaskNodeRelLogId + " 的数据集成任务节点关系-日志记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorEtlTaskNodeRelLogId + " 的数据集成任务节点关系-日志记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorEtlTaskNodeRelLogDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorEtlTaskNodeRelLogId);
                        CollectorEtlTaskNodeRelLogDO existingCollectorEtlTaskNodeRelLog = CollectorEtlTaskNodeRelLogMapper.selectOne(queryWrapper);
                        if (existingCollectorEtlTaskNodeRelLog == null) {
                            CollectorEtlTaskNodeRelLogMapper.insert(CollectorEtlTaskNodeRelLogDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorEtlTaskNodeRelLogId + " 的数据集成任务节点关系-日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorEtlTaskNodeRelLogId + " 的数据集成任务节点关系-日志记录已存在。");
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
