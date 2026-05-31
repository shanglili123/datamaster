

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskLogDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskLogMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据集成任务-日志Service业务层处理
 *
 * @author qdata
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskLogServiceImpl  extends ServiceImpl<CollectorEtlTaskLogMapper,CollectorEtlTaskLogDO> implements ICollectorEtlTaskLogService {
    @Resource
    private CollectorEtlTaskLogMapper CollectorEtlTaskLogMapper;

    @Override
    public PageResult<CollectorEtlTaskLogDO> getCollectorEtlTaskLogPage(CollectorEtlTaskLogPageReqVO pageReqVO) {
        return CollectorEtlTaskLogMapper.selectPage(pageReqVO);
    }

    @Override
    public CollectorEtlTaskLogRespVO getCollectorEtlTaskLogById(CollectorEtlTaskLogPageReqVO reqVO) {
        MPJLambdaWrapper<CollectorEtlTaskLogDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(CollectorEtlTaskLogDO.class)
                .eq(StringUtils.isNotEmpty(reqVO.getCode()) , CollectorEtlTaskLogDO::getCode, reqVO.getCode())
                .eq(reqVO.getVersion() != null, CollectorEtlTaskLogDO::getVersion, reqVO.getVersion());
        CollectorEtlTaskLogDO CollectorEtlNodeLogDO = CollectorEtlTaskLogMapper.selectOne(wrapper);
        return BeanUtils.toBean(CollectorEtlNodeLogDO, CollectorEtlTaskLogRespVO.class);
    }

    @Override
    public Long createCollectorEtlTaskLog(CollectorEtlTaskLogSaveReqVO createReqVO) {
        CollectorEtlTaskLogDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlTaskLogDO.class);
        CollectorEtlTaskLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlTaskLog(CollectorEtlTaskLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务-日志
        CollectorEtlTaskLogDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskLogDO.class);
        return CollectorEtlTaskLogMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorEtlTaskLog(Collection<Long> idList) {
        // 批量删除数据集成任务-日志
        return CollectorEtlTaskLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlTaskLogDO getCollectorEtlTaskLogById(Long id) {
        return CollectorEtlTaskLogMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlTaskLogDO> getCollectorEtlTaskLogList() {
        return CollectorEtlTaskLogMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlTaskLogDO> getCollectorEtlTaskLogMap() {
        List<CollectorEtlTaskLogDO> CollectorEtlTaskLogList = CollectorEtlTaskLogMapper.selectList();
        return CollectorEtlTaskLogList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlTaskLogDO::getId,
                        CollectorEtlTaskLogDO -> CollectorEtlTaskLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据集成任务-日志数据
         *
         * @param importExcelList 数据集成任务-日志数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importCollectorEtlTaskLog(List<CollectorEtlTaskLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorEtlTaskLogRespVO respVO : importExcelList) {
                try {
                    CollectorEtlTaskLogDO CollectorEtlTaskLogDO = BeanUtils.toBean(respVO, CollectorEtlTaskLogDO.class);
                    Long CollectorEtlTaskLogId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorEtlTaskLogId != null) {
                            CollectorEtlTaskLogDO existingCollectorEtlTaskLog = CollectorEtlTaskLogMapper.selectById(CollectorEtlTaskLogId);
                            if (existingCollectorEtlTaskLog != null) {
                                CollectorEtlTaskLogMapper.updateById(CollectorEtlTaskLogDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorEtlTaskLogId + " 的数据集成任务-日志记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorEtlTaskLogId + " 的数据集成任务-日志记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorEtlTaskLogDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorEtlTaskLogId);
                        CollectorEtlTaskLogDO existingCollectorEtlTaskLog = CollectorEtlTaskLogMapper.selectOne(queryWrapper);
                        if (existingCollectorEtlTaskLog == null) {
                            CollectorEtlTaskLogMapper.insert(CollectorEtlTaskLogDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorEtlTaskLogId + " 的数据集成任务-日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorEtlTaskLogId + " 的数据集成任务-日志记录已存在。");
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
    public Integer queryMaxVersionByCode(String taskCode) {
        return baseMapper.queryMaxVersionByCode(taskCode);
    }
}
