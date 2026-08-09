

package com.datamaster.metadata.service.qa.impl;

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
import com.datamaster.metadata.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityLogRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityLogSaveReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskAssetReqVO;
import com.datamaster.metadata.dal.dataobject.qa.QualityLogDO;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskDO;
import com.datamaster.metadata.dal.mapper.qa.QualityLogMapper;
import com.datamaster.metadata.dal.mapper.qa.QualityTaskMapper;
import com.datamaster.metadata.service.qa.IEvaluateLogService;
import com.datamaster.metadata.service.qa.IQualityLogService;
import com.datamaster.module.system.api.message.dto.MessageSaveReqDTO;
import com.datamaster.module.system.service.ISysMessageService;

import static com.datamaster.common.utils.DateUtils.YYYY_MM_DD_HH_MM_SS;

/**
 * 质量探查日志Service业务层处理
 *
 * @author lili.shang
 * @date 2025-07-19
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class QualityLogServiceImpl  extends ServiceImpl<QualityLogMapper,QualityLogDO> implements IQualityLogService {
    private final QualityLogMapper QualityLogMapper;
    private final QualityTaskMapper qualityTaskMapper;
    private final IEvaluateLogService EvaluateLogService;
    private final ISysMessageService messageService;

    @Override
    public PageResult<QualityLogDO> getQualityLogPage(QualityLogPageReqVO pageReqVO) {
        PageResult<QualityLogDO> QualityLogDOPageResult = QualityLogMapper.selectPage(pageReqVO);
        List<QualityLogDO> rows = (List<QualityLogDO>)QualityLogDOPageResult.getRows();
        List<QualityLogDO> QualityLogDOS = new ArrayList<>();
        for (QualityLogDO row : rows) {
            Map<String, Object> map = EvaluateLogService.sumTotalAndProblemTotalByTaskLogId(String.valueOf(row.getId()));

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
            QualityLogDOS.add(row);
        }
        QualityLogDOPageResult.setRows(QualityLogDOS);
        return QualityLogDOPageResult;
    }

    @Override
    public Long createQualityLog(QualityLogSaveReqVO createReqVO) {
        QualityLogDO dictType = BeanUtils.toBean(createReqVO, QualityLogDO.class);
        QualityLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateQualityLog(QualityLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新质量探查日志
        QualityLogDO updateObj = BeanUtils.toBean(updateReqVO, QualityLogDO.class);
        return QualityLogMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityLog(Collection<Long> idList) {
        // 批量删除质量探查日志
        return QualityLogMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityLogDO getQualityLogById(Long id) {
        return QualityLogMapper.selectById(id);
    }

    @Override
    public QualityLogDO selectPrevLogByIdWithWrapper(Long id) {
        return QualityLogMapper.selectPrevLogByIdWithWrapper(id);
    }

    @Override
    public QualityLogDO getQualityLogById(QualityTaskAssetReqVO reqVO) {
        LambdaQueryWrapper<QualityLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QualityLogDO::getQualityId, reqVO.getId())
                .orderByDesc(QualityLogDO::getStartTime);

        Page<QualityLogDO> page = new Page<>(1, 1);
        IPage<QualityLogDO> resultPage = QualityLogMapper.selectPage(page, wrapper);

        return resultPage.getRecords().isEmpty() ? null : resultPage.getRecords().get(0);
    }

    @Override
    public List<QualityLogDO> getQualityLogList() {
        return QualityLogMapper.selectList();
    }

    @Override
    public Map<Long, QualityLogDO> getQualityLogMap() {
        List<QualityLogDO> QualityLogList = QualityLogMapper.selectList();
        return QualityLogList.stream()
                .collect(Collectors.toMap(
                        QualityLogDO::getId,
                        QualityLogDO -> QualityLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入质量探查日志数据
     *
     * @param importExcelList 质量探查日志数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importQualityLog(List<QualityLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (QualityLogRespVO respVO : importExcelList) {
            try {
                QualityLogDO QualityLogDO = BeanUtils.toBean(respVO, QualityLogDO.class);
                Long QualityLogId = respVO.getId();
                if (isUpdateSupport) {
                    if (QualityLogId != null) {
                        QualityLogDO existingQualityLog = QualityLogMapper.selectById(QualityLogId);
                        if (existingQualityLog != null) {
                            QualityLogMapper.updateById(QualityLogDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + QualityLogId + " 的质量探查日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + QualityLogId + " 的质量探查日志记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<QualityLogDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", QualityLogId);
                    QualityLogDO existingQualityLog = QualityLogMapper.selectOne(queryWrapper);
                    if (existingQualityLog == null) {
                        QualityLogMapper.insert(QualityLogDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + QualityLogId + " 的质量探查日志记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + QualityLogId + " 的质量探查日志记录已存在。");
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
        QualityLogDO QualityLogDO = QualityLogMapper.selectById(id);
        QualityTaskDO QualityTaskDO = qualityTaskMapper.selectById(QualityLogDO.getQualityId());
        Map<String, Object> map = EvaluateLogService.sumTotalAndProblemTotalByTaskLogId(String.valueOf(QualityLogDO.getId()));
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
        messageSaveReqDTO.setReceiverId(QualityTaskDO.getContactId());
        HashMap<String, Object> messageMeta = new HashMap<>();
        messageMeta.put("taskName", QualityTaskDO.getTaskName());
        messageMeta.put("executionTime", DateUtils.parseDateToStr(YYYY_MM_DD_HH_MM_SS,QualityLogDO.getEndTime()));
        messageMeta.put("qualityScore", score);
        messageMeta.put("totalNumber", total);
        messageMeta.put("errorNumber", problemTotal);
        messageService.send(7L, messageSaveReqDTO, messageMeta);
    }
}
