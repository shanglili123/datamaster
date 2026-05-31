

package com.datamaster.module.standards.service.desensitizeRules.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRulePageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
import com.datamaster.module.standards.dal.mapper.dataCategory.StandardsDataCategoryMapper;
import com.datamaster.module.standards.dal.mapper.dataCategoryCat.StandardsDataCategoryCatMapper;
import com.datamaster.module.standards.dal.mapper.desensitizeRules.StandardsDesensitizeIntervalMapper;
import com.datamaster.module.standards.dal.mapper.desensitizeRules.StandardsDesensitizeRuleMapper;
import com.datamaster.module.standards.service.desensitizeRules.IStandardsDesensitizeRuleService;
/**
 * 脱敏规则Service业务层处理
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class StandardsDesensitizeRuleServiceImpl  extends ServiceImpl<StandardsDesensitizeRuleMapper,StandardsDesensitizeRuleDO> implements IStandardsDesensitizeRuleService {
    @Resource
    private StandardsDesensitizeRuleMapper StandardsDesensitizeRuleMapper;

    @Resource
    private StandardsDesensitizeIntervalMapper StandardsDesensitizeIntervalMapper;

    @Resource
    private StandardsDataCategoryMapper StandardsDataCategoryMapper;


    @Override
    public PageResult<StandardsDesensitizeRuleDO> getDgDesensitizeRulePage(StandardsDesensitizeRulePageReqVO pageReqVO) {
        return StandardsDesensitizeRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDgDesensitizeRule(StandardsDesensitizeRuleSaveReqVO createReqVO) {
        StandardsDesensitizeRuleDO dictType = BeanUtils.toBean(createReqVO, StandardsDesensitizeRuleDO.class);
        //判断数据分类是否在当前规则下已存在
        if (StandardsDesensitizeRuleMapper.selectCount(new LambdaQueryWrapper<StandardsDesensitizeRuleDO>()
                .eq(StandardsDesensitizeRuleDO::getDataCategoryId, dictType.getDataCategoryId())) > 0) {
            throw new IllegalArgumentException("数据分类已存在");
        }

        StandardsDesensitizeRuleMapper.insert(dictType);
        List<StandardsDesensitizeIntervalDO> intervalList = createReqVO.getIntervalList();
        if (StringUtils.isNotNull(intervalList)) {
            intervalList.forEach(interval -> {
                interval.setDesensitizeRuleId(dictType.getId());
            });
            StandardsDesensitizeIntervalMapper.insertBatch(intervalList);
        }
        return dictType.getId();
    }

    @Override
    public int updateDgDesensitizeRule(StandardsDesensitizeRuleSaveReqVO updateReqVO) {
        // 更新脱敏规则
        StandardsDesensitizeRuleDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDesensitizeRuleDO.class);

       //先判断updateObj旧的区间是否存在，存在则删除旧的区间
        if (StringUtils.isNotNull(updateObj.getIntervalList())) {
            // 先删除旧的区间
            StandardsDesensitizeIntervalMapper.delete(
                    Wrappers.lambdaQuery(StandardsDesensitizeIntervalDO.class)
                            .eq(StandardsDesensitizeIntervalDO::getDesensitizeRuleId, updateObj.getId())
            );
        }
        // 再插入新的区间
        List<StandardsDesensitizeIntervalDO> intervalList = updateReqVO.getIntervalList();
        if (StringUtils.isNotNull(intervalList)) {
            intervalList.forEach(interval -> {
                interval.setDesensitizeRuleId(updateObj.getId());
                interval.setId(null);//防止再次插入报错
            });
            StandardsDesensitizeIntervalMapper.insertBatch(intervalList);
        }
        return StandardsDesensitizeRuleMapper.updateById(updateObj);
    }
    @Override
    public int removeDgDesensitizeRule(Collection<Long> idList) {
        // 批量删除脱敏规则和区间数据
        StandardsDesensitizeIntervalMapper.delete(
                Wrappers.lambdaQuery(StandardsDesensitizeIntervalDO.class)
                        .in(StandardsDesensitizeIntervalDO::getDesensitizeRuleId, idList)
        );
        //StandardsDesensitizeIntervalMapper.delete(new QueryWrapper<StandardsDesensitizeIntervalDO>().in("desensitize_id", idList));
        return StandardsDesensitizeRuleMapper.deleteBatchIds(idList);
    }
    @Override
    public StandardsDesensitizeRuleDO getDgDesensitizeRuleById(Long id) {
        StandardsDesensitizeRuleDO rule = StandardsDesensitizeRuleMapper.selectById(id);
        //将rule中的分类ID转换为分类名称
        if(rule.getDataCategoryId() != null){
            StandardsDataCategoryDO StandardsDataCategoryDO = StandardsDataCategoryMapper.selectById(rule.getDataCategoryId());
            if(StandardsDataCategoryDO!=null) {
                rule.setDataCategoryName(StandardsDataCategoryDO.getName());
            }
        }
        //根据脱敏规则ID 查询区间集合存入StandardsDesensitizeRuleDO
        rule.setIntervalList(StandardsDesensitizeIntervalMapper.selectList(new LambdaQueryWrapper<StandardsDesensitizeIntervalDO>().eq(StandardsDesensitizeIntervalDO::getDesensitizeRuleId, id)));
        return rule;
    }

    @Override
    public StandardsDesensitizeRuleDO getDgDesensitizeRuleByDataCategoryId(Long dataCategoryId) {
        StandardsDesensitizeRuleDO rule =  StandardsDesensitizeRuleMapper.selectOne(new LambdaQueryWrapper<StandardsDesensitizeRuleDO>().eq(StandardsDesensitizeRuleDO::getDataCategoryId, dataCategoryId));
        if(rule!=null){
            rule.setIntervalList(StandardsDesensitizeIntervalMapper.selectList(new LambdaQueryWrapper<StandardsDesensitizeIntervalDO>().eq(StandardsDesensitizeIntervalDO::getDesensitizeRuleId, rule.getId())));
        }
        return rule;
    }

    @Override
    public List<StandardsDesensitizeRuleDO> getDgDesensitizeRuleList() {
        return StandardsDesensitizeRuleMapper.selectList();
    }

    @Override
    public Map<Long, StandardsDesensitizeRuleDO> getDgDesensitizeRuleMap() {
        List<StandardsDesensitizeRuleDO> StandardsDesensitizeRuleList = StandardsDesensitizeRuleMapper.selectList();
        return StandardsDesensitizeRuleList.stream()
                .collect(Collectors.toMap(
                        StandardsDesensitizeRuleDO::getId,
                        StandardsDesensitizeRuleDO -> StandardsDesensitizeRuleDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入脱敏规则数据
         *
         * @param importExcelList 脱敏规则数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importDgDesensitizeRule(List<StandardsDesensitizeRuleRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (StandardsDesensitizeRuleRespVO respVO : importExcelList) {
                try {
                    StandardsDesensitizeRuleDO StandardsDesensitizeRuleDO = BeanUtils.toBean(respVO, StandardsDesensitizeRuleDO.class);
                    Long StandardsDesensitizeRuleId = respVO.getId();
                    if (isUpdateSupport) {
                        if (StandardsDesensitizeRuleId != null) {
                            StandardsDesensitizeRuleDO existingDgDesensitizeRule = StandardsDesensitizeRuleMapper.selectById(StandardsDesensitizeRuleId);
                            if (existingDgDesensitizeRule != null) {
                                StandardsDesensitizeRuleMapper.updateById(StandardsDesensitizeRuleDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + StandardsDesensitizeRuleId + " 的脱敏规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + StandardsDesensitizeRuleId + " 的脱敏规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<StandardsDesensitizeRuleDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", StandardsDesensitizeRuleId);
                        StandardsDesensitizeRuleDO existingDgDesensitizeRule = StandardsDesensitizeRuleMapper.selectOne(queryWrapper);
                        if (existingDgDesensitizeRule == null) {
                            StandardsDesensitizeRuleMapper.insert(StandardsDesensitizeRuleDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + StandardsDesensitizeRuleId + " 的脱敏规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + StandardsDesensitizeRuleId + " 的脱敏规则记录已存在。");
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
    public Long getCountByCategoryIds(Collection<Long> idList) {
        return baseMapper.selectCount(Wrappers
                .lambdaQuery(StandardsDesensitizeRuleDO.class)
                .in(StandardsDesensitizeRuleDO::getDataCategoryId, idList));
    }
}
