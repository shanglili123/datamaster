

package com.datamaster.module.taxonomy.service.rule.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyAuditRuleDO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.enums.RuleTypeEnum;
import com.datamaster.module.taxonomy.dal.mapper.rule.TaxonomyAuditRuleMapper;
import com.datamaster.module.taxonomy.service.rule.ITaxonomyAuditRuleService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 稽查规则Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyAuditRuleServiceImpl extends ServiceImpl<TaxonomyAuditRuleMapper, TaxonomyAuditRuleDO>
        implements ITaxonomyAuditRuleService {
    @Resource
    private TaxonomyAuditRuleMapper TaxonomyAuditRuleMapper;

    @Override
    public PageResult<TaxonomyAuditRuleDO> getAttAuditRulePage(TaxonomyAuditRulePageReqVO pageReqVO) {
        return TaxonomyAuditRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttAuditRule(TaxonomyAuditRuleSaveReqVO createReqVO) {
        TaxonomyAuditRuleDO dictType = BeanUtils.toBean(createReqVO, TaxonomyAuditRuleDO.class);
        TaxonomyAuditRuleMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttAuditRule(TaxonomyAuditRuleSaveReqVO updateReqVO) {
        // 相关校验

        // 更新稽查规则
        TaxonomyAuditRuleDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyAuditRuleDO.class);
        return TaxonomyAuditRuleMapper.updateById(updateObj);
    }

    @Override
    public int removeAttAuditRule(Collection<Long> idList) {
        // 批量删除稽查规则
        return TaxonomyAuditRuleMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyAuditRuleDO getAttAuditRuleById(Long id) {
        return TaxonomyAuditRuleMapper.selectById(id);
    }

    @Override
    public List<TaxonomyAuditRuleDO> getAttAuditRuleList() {
        return TaxonomyAuditRuleMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyAuditRuleDO> getAttAuditRuleMap() {
        List<TaxonomyAuditRuleDO> TaxonomyAuditRuleList = TaxonomyAuditRuleMapper.selectList();
        return TaxonomyAuditRuleList.stream()
                .collect(Collectors.toMap(
                        TaxonomyAuditRuleDO::getId,
                        TaxonomyAuditRuleDO -> TaxonomyAuditRuleDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }

    @Override
    public List<TaxonomyAuditRuleRespVO> getAttAuditRuleTree(Long dataElemId) {
        // 1. 获取所有稽查规则列表
        List<TaxonomyAuditRuleDO> list = TaxonomyAuditRuleMapper.selectAttAuditRuleList(dataElemId);
        // 2. 转换为VO对象
        List<TaxonomyAuditRuleRespVO> voList = BeanUtils.toBean(list, TaxonomyAuditRuleRespVO.class);
        // 3. 构建树形结构
        return buildTreeByType(voList);
    }

    /**
     * 构建树形结构 - 以type字段作为父节点
     *
     * @param list 规则列表
     * @return 树形结构列表
     */
    private List<TaxonomyAuditRuleRespVO> buildTreeByType(List<TaxonomyAuditRuleRespVO> list) {
        List<TaxonomyAuditRuleRespVO> resultList = new ArrayList<>();
        // 创建type映射，用于存储相同type的节点
        Map<String, List<TaxonomyAuditRuleRespVO>> typeMap = list.stream()
                .collect(Collectors.groupingBy(TaxonomyAuditRuleRespVO::getType));

        // 遍历每个type分组
        for (Map.Entry<String, List<TaxonomyAuditRuleRespVO>> entry : typeMap.entrySet()) {
            String type = entry.getKey();
            List<TaxonomyAuditRuleRespVO> typeNodes = entry.getValue();
            for (TaxonomyAuditRuleRespVO typeNode : typeNodes) {
                typeNode.setDataType("2");
            }
            // 创建父节点
            TaxonomyAuditRuleRespVO parentNode = new TaxonomyAuditRuleRespVO();
            parentNode.setId(0L); // 设置一个特殊的ID
            parentNode.setType(type);
            parentNode.setDataType("1");
            // 使用枚举获取类型名称
            String typeName = RuleTypeEnum.getNameByType(type);
            parentNode.setName(typeName); // 设置父节点名称
            parentNode.setChildren(new ArrayList<>(typeNodes));

            resultList.add(parentNode);
        }

        return resultList;
    }

    /**
     * 导入稽查规则数据
     *
     * @param importExcelList 稽查规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttAuditRule(List<TaxonomyAuditRuleRespVO> importExcelList, boolean isUpdateSupport,
                                     String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyAuditRuleRespVO respVO : importExcelList) {
            try {
                TaxonomyAuditRuleDO TaxonomyAuditRuleDO = BeanUtils.toBean(respVO, TaxonomyAuditRuleDO.class);
                Long TaxonomyAuditRuleId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyAuditRuleId != null) {
                        TaxonomyAuditRuleDO existingAttAuditRule = TaxonomyAuditRuleMapper.selectById(TaxonomyAuditRuleId);
                        if (existingAttAuditRule != null) {
                            TaxonomyAuditRuleMapper.updateById(TaxonomyAuditRuleDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyAuditRuleId + " 的稽查规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyAuditRuleId + " 的稽查规则记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyAuditRuleDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyAuditRuleId);
                    TaxonomyAuditRuleDO existingAttAuditRule = TaxonomyAuditRuleMapper.selectOne(queryWrapper);
                    if (existingAttAuditRule == null) {
                        TaxonomyAuditRuleMapper.insert(TaxonomyAuditRuleDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyAuditRuleId + " 的稽查规则记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyAuditRuleId + " 的稽查规则记录已存在。");
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
