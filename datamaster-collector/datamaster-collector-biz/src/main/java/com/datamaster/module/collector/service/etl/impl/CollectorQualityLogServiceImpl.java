

package com.datamaster.module.collector.service.etl.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.DateUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogSaveReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskAssetReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorQualityLogMapper;
import com.datamaster.module.collector.dal.mapper.qa.CollectorQualityTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEvaluateLogService;
import com.datamaster.module.collector.service.etl.ICollectorQualityLogService;
import com.datamaster.module.system.api.message.dto.MessageSaveReqDTO;
import com.datamaster.module.system.service.ISysMessageService;

import static com.datamaster.common.utils.DateUtils.YYYY_MM_DD_HH_MM_SS;

/**
 * 数据质量日志Service业务层处理
 *
 * @author lili.shang
 * @date 2025-07-19
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CollectorQualityLogServiceImpl  extends ServiceImpl<CollectorQualityLogMapper,CollectorQualityLogDO> implements ICollectorQualityLogService {
    private final CollectorQualityLogMapper CollectorQualityLogMapper;
    private final CollectorQualityTaskMapper qualityTaskMapper;
    private final ICollectorEvaluateLogService CollectorEvaluateLogService;
    private final ISysMessageService messageService;

    @Override
    public PageResult<CollectorQualityLogDO> getCollectorQualityLogPage(CollectorQualityLogPageReqVO pageReqVO) {
        PageResult<CollectorQualityLogDO> CollectorQualityLogDOPageResult = CollectorQualityLogMapper.selectPage(pageReqVO);
        List<CollectorQualityLogDO> rows = (List<CollectorQualityLogDO>)CollectorQualityLogDOPageResult.getRows();
        List<CollectorQualityLogDO> CollectorQualityLogDOS = new ArrayList<>();
        for (CollectorQualityLogDO row : rows) {
            Map<String, Object> map = CollectorEvaluateLogService.sumTotalAndProblemTotalByTaskLogId(String.valueOf(row.getId()));

            // 获取总数与问题数（确保 null 安全）
            Long total = map.get("total") == null ? 0L : (Long) map.get("total");
            Long problemTotal = map.get("problemTotal") == null ? 0L : (Long) map.get("problemTotal");

            // 计算质量评分（百分比，保留两位小数）
            BigDecimal score = BigDecimal.ZERO;
            if (total > 0) {
                score = BigDecimal.valueOf(total - problemTotal)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            }

            // 设置评分与问题数
            row.setScore(score.longValue());
            row.setProblemData(problemTotal);
            CollectorQualityLogDOS.add(row);
        }
        CollectorQualityLogDOPageResult.setRows(CollectorQualityLogDOS);
        return CollectorQualityLogDOPageResult;
    }

    @Override
    public Long createCollectorQualityLog(CollectorQualityLogSaveReqVO createReqVO) {
        CollectorQualityLogDO dictType = BeanUtils.toBean(createReqVO, CollectorQualityLogDO.class);
        CollectorQualityLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorQualityLog(CollectorQualityLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量日志
        CollectorQualityLogDO updateObj = BeanUtils.toBean(updateReqVO, CollectorQualityLogDO.class);
        return CollectorQualityLogMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorQualityLog(Collection<Long> idList) {
        // 批量删除数据质量日志
        return CollectorQualityLogMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorQualityLogDO getCollectorQualityLogById(Long id) {
        return CollectorQualityLogMapper.selectById(id);
    }

    @Override
    public CollectorQualityLogDO selectPrevLogByIdWithWrapper(Long id) {
        return CollectorQualityLogMapper.selectPrevLogByIdWithWrapper(String.valueOf(id));
    }

    @Override
    public CollectorQualityLogDO getCollectorQualityLogById(CollectorQualityTaskAssetReqVO reqVO) {
        LambdaQueryWrapper<CollectorQualityLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectorQualityLogDO::getQualityId, reqVO.getId())
                .orderByDesc(CollectorQualityLogDO::getStartTime);

        Page<CollectorQualityLogDO> page = new Page<>(1, 1);
        IPage<CollectorQualityLogDO> resultPage = CollectorQualityLogMapper.selectPage(page, wrapper);

        return resultPage.getRecords().isEmpty() ? null : resultPage.getRecords().get(0);
    }

    @Override
    public List<CollectorQualityLogDO> getCollectorQualityLogList() {
        return CollectorQualityLogMapper.selectList();
    }

    @Override
    public Map<Long, CollectorQualityLogDO> getCollectorQualityLogMap() {
        List<CollectorQualityLogDO> CollectorQualityLogList = CollectorQualityLogMapper.selectList();
        return CollectorQualityLogList.stream()
                .collect(Collectors.toMap(
                        CollectorQualityLogDO::getId,
                        CollectorQualityLogDO -> CollectorQualityLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据质量日志数据
     *
     * @param importExcelList 数据质量日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importCollectorQualityLog(List<CollectorQualityLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorQualityLogRespVO respVO : importExcelList) {
            try {
                CollectorQualityLogDO CollectorQualityLogDO = BeanUtils.toBean(respVO, CollectorQualityLogDO.class);
                Long CollectorQualityLogId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorQualityLogId != null) {
                        CollectorQualityLogDO existingCollectorQualityLog = CollectorQualityLogMapper.selectById(CollectorQualityLogId);
                        if (existingCollectorQualityLog != null) {
                            CollectorQualityLogMapper.updateById(CollectorQualityLogDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorQualityLogId + " 的数据质量日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorQualityLogId + " 的数据质量日志记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorQualityLogDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorQualityLogId);
                    CollectorQualityLogDO existingCollectorQualityLog = CollectorQualityLogMapper.selectOne(queryWrapper);
                    if (existingCollectorQualityLog == null) {
                        CollectorQualityLogMapper.insert(CollectorQualityLogDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorQualityLogId + " 的数据质量日志记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorQualityLogId + " 的数据质量日志记录已存在。");
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
    public void sendMessage(Long id) {
        CollectorQualityLogDO CollectorQualityLogDO = CollectorQualityLogMapper.selectById(id);
        CollectorQualityTaskDO CollectorQualityTaskDO = qualityTaskMapper.selectById(CollectorQualityLogDO.getQualityId());
        Map<String, Object> map = CollectorEvaluateLogService.sumTotalAndProblemTotalByTaskLogId(String.valueOf(CollectorQualityLogDO.getId()));
        // 获取总数与问题数（确保 null 安全）
        Long total = map.get("total") == null ? 0L : (Long) map.get("total");
        Long problemTotal = map.get("problemTotal") == null ? 0L : (Long) map.get("problemTotal");

        // 计算质量评分（百分比，保留两位小数）
        BigDecimal score = BigDecimal.ZERO;
        if (total > 0) {
            score = BigDecimal.valueOf(total - problemTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
        MessageSaveReqDTO messageSaveReqDTO = new MessageSaveReqDTO();
        messageSaveReqDTO.setReceiverId(Long.valueOf(CollectorQualityTaskDO.getContactId()));
        HashMap<String, Object> messageMeta = new HashMap<>();
        messageMeta.put("taskName", CollectorQualityTaskDO.getTaskName());
        messageMeta.put("executionTime", DateUtils.parseDateToStr(YYYY_MM_DD_HH_MM_SS,CollectorQualityLogDO.getEndTime()));
        messageMeta.put("qualityScore", score);
        messageMeta.put("totalNumber", total);
        messageMeta.put("errorNumber", problemTotal);
        messageService.send(7L, messageSaveReqDTO, messageMeta);
    }
}
