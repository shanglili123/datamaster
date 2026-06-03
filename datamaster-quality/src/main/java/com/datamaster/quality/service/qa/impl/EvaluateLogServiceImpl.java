

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
import com.datamaster.quality.controller.qa.vo.EvaluateLogPageReqVO;
import com.datamaster.quality.controller.qa.vo.EvaluateLogRespVO;
import com.datamaster.quality.controller.qa.vo.EvaluateLogSaveReqVO;
import com.datamaster.quality.dal.dataobject.qa.EvaluateLogDO;
import com.datamaster.quality.dal.mapper.qa.EvaluateLogMapper;
import com.datamaster.quality.service.qa.IEvaluateLogService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 评测规则结果Service业务层处理
 *
 * @author lili.shang
 * @date 2025-07-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class EvaluateLogServiceImpl  extends ServiceImpl<EvaluateLogMapper, EvaluateLogDO> implements IEvaluateLogService {
    @Resource
    private EvaluateLogMapper EvaluateLogMapper;

    @Override
    public PageResult<EvaluateLogDO> getEvaluateLogPage(EvaluateLogPageReqVO pageReqVO) {
        return EvaluateLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createEvaluateLog(EvaluateLogSaveReqVO createReqVO) {
        EvaluateLogDO dictType = BeanUtils.toBean(createReqVO, EvaluateLogDO.class);
        EvaluateLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateEvaluateLog(EvaluateLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新评测规则结果
        EvaluateLogDO updateObj = BeanUtils.toBean(updateReqVO, EvaluateLogDO.class);
        return EvaluateLogMapper.updateById(updateObj);
    }
    @Override
    public int removeEvaluateLog(Collection<Long> idList) {
        // 批量删除评测规则结果
        return EvaluateLogMapper.deleteBatchIds(idList);
    }

    @Override
    public EvaluateLogDO getEvaluateLogById(Long id) {
        return EvaluateLogMapper.selectById(id);
    }

    @Override
    public List<EvaluateLogDO> getEvaluateLogList() {
        return EvaluateLogMapper.selectList();
    }

    @Override
    public Map<Long, EvaluateLogDO> getEvaluateLogMap() {
        List<EvaluateLogDO> EvaluateLogList = EvaluateLogMapper.selectList();
        return EvaluateLogList.stream()
                .collect(Collectors.toMap(
                        EvaluateLogDO::getId,
                        EvaluateLogDO -> EvaluateLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入评测规则结果数据
         *
         * @param importExcelList 评测规则结果数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importEvaluateLog(List<EvaluateLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (EvaluateLogRespVO respVO : importExcelList) {
                try {
                    EvaluateLogDO EvaluateLogDO = BeanUtils.toBean(respVO, EvaluateLogDO.class);
                    Long EvaluateLogId = respVO.getId();
                    if (isUpdateSupport) {
                        if (EvaluateLogId != null) {
                            EvaluateLogDO existingEvaluateLog = EvaluateLogMapper.selectById(EvaluateLogId);
                            if (existingEvaluateLog != null) {
                                EvaluateLogMapper.updateById(EvaluateLogDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + EvaluateLogId + " 的评测规则结果记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + EvaluateLogId + " 的评测规则结果记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<EvaluateLogDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", EvaluateLogId);
                        EvaluateLogDO existingEvaluateLog = EvaluateLogMapper.selectOne(queryWrapper);
                        if (existingEvaluateLog == null) {
                            EvaluateLogMapper.insert(EvaluateLogDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + EvaluateLogId + " 的评测规则结果记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + EvaluateLogId + " 的评测规则结果记录已存在。");
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
