

package com.datamaster.module.collector.service.qa.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluatePageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;
import com.datamaster.module.collector.dal.mapper.qa.CollectorQualityTaskEvaluateMapper;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskEvaluateService;

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
public class CollectorQualityTaskEvaluateServiceImpl  extends ServiceImpl<CollectorQualityTaskEvaluateMapper,CollectorQualityTaskEvaluateDO> implements ICollectorQualityTaskEvaluateService {
    @Resource
    private CollectorQualityTaskEvaluateMapper CollectorQualityTaskEvaluateMapper;

    @Override
    public PageResult<CollectorQualityTaskEvaluateDO> getCollectorQualityTaskEvaluatePage(CollectorQualityTaskEvaluatePageReqVO pageReqVO) {
        return CollectorQualityTaskEvaluateMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorQualityTaskEvaluate(CollectorQualityTaskEvaluateSaveReqVO createReqVO) {
        CollectorQualityTaskEvaluateDO dictType = BeanUtils.toBean(createReqVO, CollectorQualityTaskEvaluateDO.class);
        CollectorQualityTaskEvaluateMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorQualityTaskEvaluate(CollectorQualityTaskEvaluateSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量任务-评测规则
        CollectorQualityTaskEvaluateDO updateObj = BeanUtils.toBean(updateReqVO, CollectorQualityTaskEvaluateDO.class);
        return CollectorQualityTaskEvaluateMapper.updateById(updateObj);
    }
    @Override
    public int removeCollectorQualityTaskEvaluate(Collection<Long> idList) {
        // 批量删除数据质量任务-评测规则
        return CollectorQualityTaskEvaluateMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorQualityTaskEvaluateDO getCollectorQualityTaskEvaluateById(Long id) {
        return CollectorQualityTaskEvaluateMapper.selectById(id);
    }

    @Override
    public List<CollectorQualityTaskEvaluateDO> getCollectorQualityTaskEvaluateList() {
        return CollectorQualityTaskEvaluateMapper.selectList();
    }

    @Override
    public Map<Long, CollectorQualityTaskEvaluateDO> getCollectorQualityTaskEvaluateMap() {
        List<CollectorQualityTaskEvaluateDO> CollectorQualityTaskEvaluateList = CollectorQualityTaskEvaluateMapper.selectList();
        return CollectorQualityTaskEvaluateList.stream()
                .collect(Collectors.toMap(
                        CollectorQualityTaskEvaluateDO::getId,
                        CollectorQualityTaskEvaluateDO -> CollectorQualityTaskEvaluateDO,
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
        public String importCollectorQualityTaskEvaluate(List<CollectorQualityTaskEvaluateRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (CollectorQualityTaskEvaluateRespVO respVO : importExcelList) {
                try {
                    CollectorQualityTaskEvaluateDO CollectorQualityTaskEvaluateDO = BeanUtils.toBean(respVO, CollectorQualityTaskEvaluateDO.class);
                    Long CollectorQualityTaskEvaluateId = respVO.getId();
                    if (isUpdateSupport) {
                        if (CollectorQualityTaskEvaluateId != null) {
                            CollectorQualityTaskEvaluateDO existingCollectorQualityTaskEvaluate = CollectorQualityTaskEvaluateMapper.selectById(CollectorQualityTaskEvaluateId);
                            if (existingCollectorQualityTaskEvaluate != null) {
                                CollectorQualityTaskEvaluateMapper.updateById(CollectorQualityTaskEvaluateDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + CollectorQualityTaskEvaluateId + " 的数据质量任务-评测规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + CollectorQualityTaskEvaluateId + " 的数据质量任务-评测规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<CollectorQualityTaskEvaluateDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", CollectorQualityTaskEvaluateId);
                        CollectorQualityTaskEvaluateDO existingCollectorQualityTaskEvaluate = CollectorQualityTaskEvaluateMapper.selectOne(queryWrapper);
                        if (existingCollectorQualityTaskEvaluate == null) {
                            CollectorQualityTaskEvaluateMapper.insert(CollectorQualityTaskEvaluateDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + CollectorQualityTaskEvaluateId + " 的数据质量任务-评测规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + CollectorQualityTaskEvaluateId + " 的数据质量任务-评测规则记录已存在。");
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
