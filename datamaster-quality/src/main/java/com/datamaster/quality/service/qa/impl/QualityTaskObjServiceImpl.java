

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
import com.datamaster.quality.controller.qa.vo.QualityTaskObjPageReqVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskObjRespVO;
import com.datamaster.quality.controller.qa.vo.QualityTaskObjSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskObjDO;
import com.datamaster.quality.dal.mapper.qa.QualityTaskObjMapper;
import com.datamaster.quality.service.qa.IQualityTaskObjService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 数据质量任务-稽查对象Service业务层处理
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityTaskObjServiceImpl  extends ServiceImpl<QualityTaskObjMapper, QualityTaskObjDO> implements IQualityTaskObjService {
    @Resource
    private QualityTaskObjMapper QualityTaskObjMapper;

    @Override
    public PageResult<QualityTaskObjDO> getQualityTaskObjPage(QualityTaskObjPageReqVO pageReqVO) {
        return QualityTaskObjMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createQualityTaskObj(QualityTaskObjSaveReqVO createReqVO) {
        QualityTaskObjDO dictType = BeanUtils.toBean(createReqVO, QualityTaskObjDO.class);
        QualityTaskObjMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateQualityTaskObj(QualityTaskObjSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量任务-稽查对象
        QualityTaskObjDO updateObj = BeanUtils.toBean(updateReqVO, QualityTaskObjDO.class);
        return QualityTaskObjMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityTaskObj(Collection<Long> idList) {
        // 批量删除数据质量任务-稽查对象
        return QualityTaskObjMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityTaskObjDO getQualityTaskObjById(Long id) {
        return QualityTaskObjMapper.selectById(id);
    }

    @Override
    public List<QualityTaskObjDO> getQualityTaskObjList(String taskId) {
        LambdaQueryWrapperX<QualityTaskObjDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eq(QualityTaskObjDO::getTaskId, taskId)
                .eq(QualityTaskObjDO::getDelFlag,"0");
        return QualityTaskObjMapper.selectList(queryWrapperX);
    }

    @Override
    public List<QualityTaskObjDO> getQualityTaskObjList() {
        return QualityTaskObjMapper.selectList();
    }

    @Override
    public Map<Long, QualityTaskObjDO> getQualityTaskObjMap() {
        List<QualityTaskObjDO> QualityTaskObjList = QualityTaskObjMapper.selectList();
        return QualityTaskObjList.stream()
                .collect(Collectors.toMap(
                        QualityTaskObjDO::getId,
                        QualityTaskObjDO -> QualityTaskObjDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入数据质量任务-稽查对象数据
         *
         * @param importExcelList 数据质量任务-稽查对象数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityTaskObj(List<QualityTaskObjRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityTaskObjRespVO respVO : importExcelList) {
                try {
                    QualityTaskObjDO QualityTaskObjDO = BeanUtils.toBean(respVO, QualityTaskObjDO.class);
                    Long QualityTaskObjId = respVO.getId();
                    if (isUpdateSupport) {
                        if (QualityTaskObjId != null) {
                            QualityTaskObjDO existingQualityTaskObj = QualityTaskObjMapper.selectById(QualityTaskObjId);
                            if (existingQualityTaskObj != null) {
                                QualityTaskObjMapper.updateById(QualityTaskObjDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + QualityTaskObjId + " 的数据质量任务-稽查对象记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + QualityTaskObjId + " 的数据质量任务-稽查对象记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityTaskObjDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", QualityTaskObjId);
                        QualityTaskObjDO existingQualityTaskObj = QualityTaskObjMapper.selectOne(queryWrapper);
                        if (existingQualityTaskObj == null) {
                            QualityTaskObjMapper.insert(QualityTaskObjDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + QualityTaskObjId + " 的数据质量任务-稽查对象记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + QualityTaskObjId + " 的数据质量任务-稽查对象记录已存在。");
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
