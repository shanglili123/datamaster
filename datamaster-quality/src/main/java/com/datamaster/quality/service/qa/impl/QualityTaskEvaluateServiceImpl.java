

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
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluatePageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluateRespVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskEvaluateSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskEvaluateDO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskObjDO;
import com.datamaster.quality.dal.mapper.qa.QualityTaskEvaluateMapper;
import com.datamaster.quality.service.qa.IQualityTaskEvaluateService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据质量任务-评测规则Service业务层处理
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityTaskEvaluateServiceImpl  extends ServiceImpl<QualityTaskEvaluateMapper, QualityTaskEvaluateDO> implements IQualityTaskEvaluateService {
    @Resource
    private QualityTaskEvaluateMapper QualityTaskEvaluateMapper;

    @Override
    public PageResult<QualityTaskEvaluateDO> getQualityTaskEvaluatePage(QualityTaskEvaluatePageReqVO pageReqVO) {
        return QualityTaskEvaluateMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createQualityTaskEvaluate(QualityTaskEvaluateSaveReqVO createReqVO) {
        QualityTaskEvaluateDO dictType = BeanUtils.toBean(createReqVO, QualityTaskEvaluateDO.class);
        QualityTaskEvaluateMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateQualityTaskEvaluate(QualityTaskEvaluateSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量任务-评测规则
        QualityTaskEvaluateDO updateObj = BeanUtils.toBean(updateReqVO, QualityTaskEvaluateDO.class);
        return QualityTaskEvaluateMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityTaskEvaluate(Collection<Long> idList) {
        // 批量删除数据质量任务-评测规则
        return QualityTaskEvaluateMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityTaskEvaluateDO getQualityTaskEvaluateById(Long id) {
        return QualityTaskEvaluateMapper.selectById(id);
    }

    @Override
    public List<QualityTaskEvaluateDO> getQualityTaskEvaluateList(List<Long> idList) {
        LambdaQueryWrapperX<QualityTaskEvaluateDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.in(QualityTaskEvaluateDO::getObjId, idList)
                .eq(QualityTaskEvaluateDO::getDelFlag,"0");
        return QualityTaskEvaluateMapper.selectList(queryWrapperX);
    }

    @Override
    public List<QualityTaskEvaluateDO> getQualityTaskEvaluateList() {
        return QualityTaskEvaluateMapper.selectList();
    }

    @Override
    public Map<Long, QualityTaskEvaluateDO> getQualityTaskEvaluateMap() {
        List<QualityTaskEvaluateDO> QualityTaskEvaluateList = QualityTaskEvaluateMapper.selectList();
        return QualityTaskEvaluateList.stream()
                .collect(Collectors.toMap(
                        QualityTaskEvaluateDO::getId,
                        QualityTaskEvaluateDO -> QualityTaskEvaluateDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据质量任务-评测规则数据
         *
         * @param importExcelList 数据质量任务-评测规则数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityTaskEvaluate(List<QualityTaskEvaluateRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityTaskEvaluateRespVO respVO : importExcelList) {
                try {
                    QualityTaskEvaluateDO QualityTaskEvaluateDO = BeanUtils.toBean(respVO, QualityTaskEvaluateDO.class);
                    Long QualityTaskEvaluateId = respVO.getId();
                    if (isUpdateSupport) {
                        if (QualityTaskEvaluateId != null) {
                            QualityTaskEvaluateDO existingQualityTaskEvaluate = QualityTaskEvaluateMapper.selectById(QualityTaskEvaluateId);
                            if (existingQualityTaskEvaluate != null) {
                                QualityTaskEvaluateMapper.updateById(QualityTaskEvaluateDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + QualityTaskEvaluateId + " 的数据质量任务-评测规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + QualityTaskEvaluateId + " 的数据质量任务-评测规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityTaskEvaluateDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", QualityTaskEvaluateId);
                        QualityTaskEvaluateDO existingQualityTaskEvaluate = QualityTaskEvaluateMapper.selectOne(queryWrapper);
                        if (existingQualityTaskEvaluate == null) {
                            QualityTaskEvaluateMapper.insert(QualityTaskEvaluateDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + QualityTaskEvaluateId + " 的数据质量任务-评测规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + QualityTaskEvaluateId + " 的数据质量任务-评测规则记录已存在。");
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
