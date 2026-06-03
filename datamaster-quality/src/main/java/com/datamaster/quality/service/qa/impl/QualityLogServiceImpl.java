

package com.datamaster.quality.service.qa.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.quality.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityLogRespVO;
import com.datamaster.quality.controller.qa.vo.QualityLogSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityLogDO;
import com.datamaster.quality.dal.mapper.qa.QualityLogMapper;
import com.datamaster.quality.service.qa.IQualityLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据质量日志Service业务层处理
 *
 * @author lili.shang
 * @date 2025-07-19
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityLogServiceImpl  extends ServiceImpl<QualityLogMapper, QualityLogDO> implements IQualityLogService {
    @Resource
    private QualityLogMapper QualityLogMapper;

    @Override
    public PageResult<QualityLogDO> getQualityLogPage(QualityLogPageReqVO pageReqVO) {
        return QualityLogMapper.selectPage(pageReqVO);
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

        // 更新数据质量日志
        QualityLogDO updateObj = BeanUtils.toBean(updateReqVO, QualityLogDO.class);
        return QualityLogMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityLog(Collection<Long> idList) {
        // 批量删除数据质量日志
        return QualityLogMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityLogDO getQualityLogById(Long id) {
        return QualityLogMapper.selectById(id);
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
         * 导入数据质量日志数据
         *
         * @param importExcelList 数据质量日志数据列表
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
                                successMessages.add("数据更新成功，ID为 " + QualityLogId + " 的数据质量日志记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + QualityLogId + " 的数据质量日志记录不存在。");
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
                            successMessages.add("数据插入成功，ID为 " + QualityLogId + " 的数据质量日志记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + QualityLogId + " 的数据质量日志记录已存在。");
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
