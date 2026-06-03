

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
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.qa.vo.QualityTaskPageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskRespVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskDO;
import com.datamaster.quality.dal.mapper.qa.QualityTaskMapper;
import com.datamaster.quality.service.qa.IQualityTaskEvaluateService;
import com.datamaster.quality.service.qa.IQualityTaskObjService;
import com.datamaster.quality.service.qa.IQualityTaskService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据质量任务Service业务层处理
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityTaskServiceImpl  extends ServiceImpl<QualityTaskMapper, QualityTaskDO> implements IQualityTaskService {
    @Resource
    private QualityTaskMapper QualityTaskMapper;

    @Override
    public PageResult<QualityTaskDO> getQualityTaskPage(QualityTaskPageReqVO pageReqVO) {
        return QualityTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createQualityTask(QualityTaskSaveReqVO createReqVO) {
        QualityTaskDO dictType = BeanUtils.toBean(createReqVO, QualityTaskDO.class);
        QualityTaskMapper.insert(dictType);


        return dictType.getId();
    }

    @Override
    public int updateQualityTask(QualityTaskSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量任务
        QualityTaskDO updateObj = BeanUtils.toBean(updateReqVO, QualityTaskDO.class);
        return QualityTaskMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityTask(Collection<Long> idList) {
        // 批量删除数据质量任务
        return QualityTaskMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityTaskRespVO getQualityTaskById(Long id) {
        QualityTaskDO QualityTaskDO = QualityTaskMapper.selectById(id);

        QualityTaskRespVO bean = BeanUtils.toBean(QualityTaskDO, QualityTaskRespVO.class);
        return bean;
    }

    @Override
    public List<QualityTaskDO> getQualityTaskList() {
        return QualityTaskMapper.selectList();
    }

    @Override
    public Map<Long, QualityTaskDO> getQualityTaskMap() {
        List<QualityTaskDO> QualityTaskList = QualityTaskMapper.selectList();
        return QualityTaskList.stream()
                .collect(Collectors.toMap(
                        QualityTaskDO::getId,
                        QualityTaskDO -> QualityTaskDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据质量任务数据
         *
         * @param importExcelList 数据质量任务数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityTask(List<QualityTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityTaskRespVO respVO : importExcelList) {
                try {
                    QualityTaskDO QualityTaskDO = BeanUtils.toBean(respVO, QualityTaskDO.class);
                    Long QualityTaskId = respVO.getId();
                    if (isUpdateSupport) {
                        if (QualityTaskId != null) {
                            QualityTaskDO existingQualityTask = QualityTaskMapper.selectById(QualityTaskId);
                            if (existingQualityTask != null) {
                                QualityTaskMapper.updateById(QualityTaskDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + QualityTaskId + " 的数据质量任务记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + QualityTaskId + " 的数据质量任务记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityTaskDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", QualityTaskId);
                        QualityTaskDO existingQualityTask = QualityTaskMapper.selectOne(queryWrapper);
                        if (existingQualityTask == null) {
                            QualityTaskMapper.insert(QualityTaskDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + QualityTaskId + " 的数据质量任务记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + QualityTaskId + " 的数据质量任务记录已存在。");
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
