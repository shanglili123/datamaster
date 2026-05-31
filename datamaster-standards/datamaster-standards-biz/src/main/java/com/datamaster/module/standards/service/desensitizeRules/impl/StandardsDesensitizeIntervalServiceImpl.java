

package com.datamaster.module.standards.service.desensitizeRules.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.oss.ServiceException;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalPageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import com.datamaster.module.standards.dal.mapper.desensitizeRules.StandardsDesensitizeIntervalMapper;
import com.datamaster.module.standards.service.desensitizeRules.IStandardsDesensitizeIntervalService;
/**
 * 脱敏区间Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDesensitizeIntervalServiceImpl  extends ServiceImpl<StandardsDesensitizeIntervalMapper,StandardsDesensitizeIntervalDO> implements IStandardsDesensitizeIntervalService {
    @Resource
    private StandardsDesensitizeIntervalMapper StandardsDesensitizeIntervalMapper;

    @Override
    public PageResult<StandardsDesensitizeIntervalDO> getDgDesensitizeIntervalPage(StandardsDesensitizeIntervalPageReqVO pageReqVO) {
        return StandardsDesensitizeIntervalMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDesensitizeInterval(StandardsDesensitizeIntervalSaveReqVO createReqVO) {
        StandardsDesensitizeIntervalDO dictType = BeanUtils.toBean(createReqVO, StandardsDesensitizeIntervalDO.class);
        StandardsDesensitizeIntervalMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDesensitizeInterval(StandardsDesensitizeIntervalSaveReqVO updateReqVO) {
        // 相关校验

        // 更新脱敏区间
        StandardsDesensitizeIntervalDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDesensitizeIntervalDO.class);
        return StandardsDesensitizeIntervalMapper.updateById(updateObj);
    }
    @Override
    public int removeDgDesensitizeInterval(Collection<Long> idList) {
        // 批量删除脱敏区间
        return StandardsDesensitizeIntervalMapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDesensitizeIntervalDO getDgDesensitizeIntervalById(Long id) {
        return StandardsDesensitizeIntervalMapper.selectById(id);
    }

    @Override
    public List<StandardsDesensitizeIntervalDO> getDgDesensitizeIntervalList() {
        return StandardsDesensitizeIntervalMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDesensitizeIntervalDO> getDgDesensitizeIntervalMap() {
        List<StandardsDesensitizeIntervalDO> StandardsDesensitizeIntervalList = StandardsDesensitizeIntervalMapper.selectList();
        return StandardsDesensitizeIntervalList.stream()
                .collect(Collectors.toMap(
                        StandardsDesensitizeIntervalDO::getId,
                        StandardsDesensitizeIntervalDO -> StandardsDesensitizeIntervalDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入脱敏区间数据
         *
         * @param importExcelList 脱敏区间数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDgDesensitizeInterval(List<StandardsDesensitizeIntervalRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDesensitizeIntervalRespVO respVO : importExcelList) {
                try {
                    StandardsDesensitizeIntervalDO StandardsDesensitizeIntervalDO = BeanUtils.toBean(respVO, StandardsDesensitizeIntervalDO.class);
                    Long StandardsDesensitizeIntervalId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDesensitizeIntervalId != null) {
                            StandardsDesensitizeIntervalDO existingDgDesensitizeInterval = StandardsDesensitizeIntervalMapper.selectById(StandardsDesensitizeIntervalId);
                            if (existingDgDesensitizeInterval != null) {
                                StandardsDesensitizeIntervalMapper.updateById(StandardsDesensitizeIntervalDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDesensitizeIntervalId + " 的脱敏区间记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDesensitizeIntervalId + " 的脱敏区间记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDesensitizeIntervalDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDesensitizeIntervalId);
                        StandardsDesensitizeIntervalDO existingDgDesensitizeInterval = StandardsDesensitizeIntervalMapper.selectOne(queryWrapper);
                        if (existingDgDesensitizeInterval == null) {
                            StandardsDesensitizeIntervalMapper.insert(StandardsDesensitizeIntervalDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDesensitizeIntervalId + " 的脱敏区间记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDesensitizeIntervalId + " 的脱敏区间记录已存在。");
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
