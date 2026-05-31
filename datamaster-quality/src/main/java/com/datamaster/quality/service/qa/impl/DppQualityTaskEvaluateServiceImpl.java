

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
import com.datamaster.quality.controller.qa.vo.DppQualityTaskEvaluatePageReqVO;
import com.datamaster.quality.controller.qa.vo.DppQualityTaskEvaluateRespVO;
import com.datamaster.quality.controller.qa.vo.DppQualityTaskEvaluateSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.DppQualityTaskEvaluateDO;
import com.datamaster.quality.dal.dataobject.qa.DppQualityTaskObjDO;
import com.datamaster.quality.dal.mapper.qa.DppQualityTaskEvaluateMapper;
import com.datamaster.quality.service.qa.IDppQualityTaskEvaluateService;

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
public class DppQualityTaskEvaluateServiceImpl  extends ServiceImpl<DppQualityTaskEvaluateMapper, DppQualityTaskEvaluateDO> implements IDppQualityTaskEvaluateService {
    @Resource
    private DppQualityTaskEvaluateMapper dppQualityTaskEvaluateMapper;

    @Override
    public PageResult<DppQualityTaskEvaluateDO> getDppQualityTaskEvaluatePage(DppQualityTaskEvaluatePageReqVO pageReqVO) {
        return dppQualityTaskEvaluateMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDppQualityTaskEvaluate(DppQualityTaskEvaluateSaveReqVO createReqVO) {
        DppQualityTaskEvaluateDO dictType = BeanUtils.toBean(createReqVO, DppQualityTaskEvaluateDO.class);
        dppQualityTaskEvaluateMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDppQualityTaskEvaluate(DppQualityTaskEvaluateSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据质量任务-评测规则
        DppQualityTaskEvaluateDO updateObj = BeanUtils.toBean(updateReqVO, DppQualityTaskEvaluateDO.class);
        return dppQualityTaskEvaluateMapper.updateById(updateObj);
    }
    @Override
    public int removeDppQualityTaskEvaluate(Collection<Long> idList) {
        // 批量删除数据质量任务-评测规则
        return dppQualityTaskEvaluateMapper.deleteBatchIds(idList);
    }

    @Override
    public DppQualityTaskEvaluateDO getDppQualityTaskEvaluateById(Long id) {
        return dppQualityTaskEvaluateMapper.selectById(id);
    }

    @Override
    public List<DppQualityTaskEvaluateDO> getDppQualityTaskEvaluateList(List<Long> idList) {
        LambdaQueryWrapperX<DppQualityTaskEvaluateDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.in(DppQualityTaskEvaluateDO::getObjId, idList)
                .eq(DppQualityTaskEvaluateDO::getDelFlag,"0");
        return dppQualityTaskEvaluateMapper.selectList(queryWrapperX);
    }

    @Override
    public List<DppQualityTaskEvaluateDO> getDppQualityTaskEvaluateList() {
        return dppQualityTaskEvaluateMapper.selectList();
    }

    @Override
    public Map<Long, DppQualityTaskEvaluateDO> getDppQualityTaskEvaluateMap() {
        List<DppQualityTaskEvaluateDO> dppQualityTaskEvaluateList = dppQualityTaskEvaluateMapper.selectList();
        return dppQualityTaskEvaluateList.stream()
                .collect(Collectors.toMap(
                        DppQualityTaskEvaluateDO::getId,
                        dppQualityTaskEvaluateDO -> dppQualityTaskEvaluateDO,
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
        public String importDppQualityTaskEvaluate(List<DppQualityTaskEvaluateRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (DppQualityTaskEvaluateRespVO respVO : importExcelList) {
                try {
                    DppQualityTaskEvaluateDO dppQualityTaskEvaluateDO = BeanUtils.toBean(respVO, DppQualityTaskEvaluateDO.class);
                    Long dppQualityTaskEvaluateId = respVO.getId();
                    if (isUpdateSupport) {
                        if (dppQualityTaskEvaluateId != null) {
                            DppQualityTaskEvaluateDO existingDppQualityTaskEvaluate = dppQualityTaskEvaluateMapper.selectById(dppQualityTaskEvaluateId);
                            if (existingDppQualityTaskEvaluate != null) {
                                dppQualityTaskEvaluateMapper.updateById(dppQualityTaskEvaluateDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + dppQualityTaskEvaluateId + " 的数据质量任务-评测规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + dppQualityTaskEvaluateId + " 的数据质量任务-评测规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<DppQualityTaskEvaluateDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", dppQualityTaskEvaluateId);
                        DppQualityTaskEvaluateDO existingDppQualityTaskEvaluate = dppQualityTaskEvaluateMapper.selectOne(queryWrapper);
                        if (existingDppQualityTaskEvaluate == null) {
                            dppQualityTaskEvaluateMapper.insert(dppQualityTaskEvaluateDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + dppQualityTaskEvaluateId + " 的数据质量任务-评测规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + dppQualityTaskEvaluateId + " 的数据质量任务-评测规则记录已存在。");
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
