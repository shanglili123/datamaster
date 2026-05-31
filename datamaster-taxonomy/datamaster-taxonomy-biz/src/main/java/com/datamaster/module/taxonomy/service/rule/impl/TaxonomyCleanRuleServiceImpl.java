

package com.datamaster.module.taxonomy.service.rule.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.text.Convert;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyCleanRuleDO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.enums.CleanRuleTypeEnum;
import com.datamaster.module.taxonomy.dal.mapper.rule.TaxonomyCleanRuleMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyCleanCatService;
import com.datamaster.module.taxonomy.service.rule.ITaxonomyCleanRuleService;

/**
 * 清洗规则Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyCleanRuleServiceImpl extends ServiceImpl<TaxonomyCleanRuleMapper, TaxonomyCleanRuleDO>
        implements ITaxonomyCleanRuleService {
    @Resource
    private TaxonomyCleanRuleMapper TaxonomyCleanRuleMapper;
    @Resource
    private ITaxonomyCleanCatService TaxonomyCleanCatService;

    @Override
    public PageResult<TaxonomyCleanRuleDO> getAttCleanRulePage(TaxonomyCleanRulePageReqVO pageReqVO) {
        return TaxonomyCleanRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttCleanRule(TaxonomyCleanRuleSaveReqVO createReqVO) {
        List<TaxonomyCleanRuleDO> code = TaxonomyCleanRuleMapper.selectList("code", createReqVO.getCode());
        if (code.size() > 0) {
            throw new RuntimeException("规则编码重复请重新输入");
        }
        TaxonomyCleanRuleDO dictType = BeanUtils.toBean(createReqVO, TaxonomyCleanRuleDO.class);
        TaxonomyCleanRuleMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttCleanRule(TaxonomyCleanRuleSaveReqVO updateReqVO) {
        // 相关校验
        List<TaxonomyCleanRuleDO> code = TaxonomyCleanRuleMapper.selectList("code", updateReqVO.getCode());
        if (code.size() > 0) {
            throw new RuntimeException("规则编码重复请重新输入");
        }
        // 更新清洗规则
        TaxonomyCleanRuleDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyCleanRuleDO.class);
        return TaxonomyCleanRuleMapper.updateById(updateObj);
    }

    @Override
    public int removeAttCleanRule(Collection<Long> idList) {
        // 批量删除清洗规则
        return TaxonomyCleanRuleMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyCleanRuleDO getAttCleanRuleById(Long id) {
        return TaxonomyCleanRuleMapper.selectById(id);
    }

    @Override
    public List<TaxonomyCleanRuleDO> getAttCleanRuleList() {
        return TaxonomyCleanRuleMapper.selectList();
    }

    @Override
    public List<TaxonomyCleanRuleRespVO> getAttCleanRuleList(TaxonomyCleanRulePageReqVO TaxonomyCleanRule) {

        MPJLambdaWrapper<TaxonomyCleanRuleDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(TaxonomyCleanRuleDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_CLEAN_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .likeRight(org.apache.commons.lang3.StringUtils.isNotBlank(TaxonomyCleanRule.getCatCode()), TaxonomyCleanRuleDO::getCatCode, TaxonomyCleanRule.getCatCode());
//        LambdaQueryWrapperX<TaxonomyCleanRuleDO> x = new LambdaQueryWrapperX<>();
//        x.eqIfPresent(TaxonomyCleanRuleDO::getType , TaxonomyCleanRule.getType());
//        x.eqIfPresent(TaxonomyCleanRuleDO::getValidFlag , TaxonomyCleanRule.getValidFlag());
        List<TaxonomyCleanRuleDO> TaxonomyCleanRuleDOS = TaxonomyCleanRuleMapper.selectList(lambdaWrapper);
        List<TaxonomyCleanRuleRespVO> bean = BeanUtils.toBean(TaxonomyCleanRuleDOS, TaxonomyCleanRuleRespVO.class);
        for (TaxonomyCleanRuleRespVO respVO : bean) {

            respVO.setParentType(Convert.toStr(respVO.getCatID()));
            respVO.setParentName(respVO.getCatName());
        }
        return bean;
    }

    @Override
    public Map<Long, TaxonomyCleanRuleDO> getAttCleanRuleMap() {
        List<TaxonomyCleanRuleDO> TaxonomyCleanRuleList = TaxonomyCleanRuleMapper.selectList();
        return TaxonomyCleanRuleList.stream()
                .collect(Collectors.toMap(
                        TaxonomyCleanRuleDO::getId,
                        TaxonomyCleanRuleDO -> TaxonomyCleanRuleDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing));
    }

    /**
     * 导入清洗规则数据
     *
     * @param importExcelList 清洗规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttCleanRule(List<TaxonomyCleanRuleRespVO> importExcelList, boolean isUpdateSupport,
                                     String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyCleanRuleRespVO respVO : importExcelList) {
            try {
                TaxonomyCleanRuleDO TaxonomyCleanRuleDO = BeanUtils.toBean(respVO, TaxonomyCleanRuleDO.class);
                Long TaxonomyCleanRuleId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyCleanRuleId != null) {
                        TaxonomyCleanRuleDO existingAttCleanRule = TaxonomyCleanRuleMapper.selectById(TaxonomyCleanRuleId);
                        if (existingAttCleanRule != null) {
                            TaxonomyCleanRuleMapper.updateById(TaxonomyCleanRuleDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyCleanRuleId + " 的清洗规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyCleanRuleId + " 的清洗规则记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyCleanRuleDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyCleanRuleId);
                    TaxonomyCleanRuleDO existingAttCleanRule = TaxonomyCleanRuleMapper.selectOne(queryWrapper);
                    if (existingAttCleanRule == null) {
                        TaxonomyCleanRuleMapper.insert(TaxonomyCleanRuleDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyCleanRuleId + " 的清洗规则记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyCleanRuleId + " 的清洗规则记录已存在。");
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
    public List<TaxonomyCleanRuleRespVO> getAttCleanRuleTree(Long dataElemId) {
        // 1. 获取所有清洗规则列表
        List<TaxonomyCleanRuleDO> list = TaxonomyCleanRuleMapper.selectAttCleanRuleList(dataElemId);
        // 2. 转换为VO对象
        List<TaxonomyCleanRuleRespVO> voList = BeanUtils.toBean(list, TaxonomyCleanRuleRespVO.class);
        // 3. 构建树形结构
        return buildTreeByType(voList);
    }

    @Override
    public List<TaxonomyCleanRuleRespVO> getCleaningRuleTree(Long[] dataElemId) {
        List<TaxonomyCleanRuleDO> list =null;
        if (dataElemId == null || dataElemId.length == 0) {
            // 数组为空或未初始化
            list = TaxonomyCleanRuleMapper.selectList();
        }else {
            list = TaxonomyCleanRuleMapper.getCleaningRuleTreeIds(dataElemId);
        }
        // 2. 转换为VO对象
        List<TaxonomyCleanRuleRespVO> voList = BeanUtils.toBean(list, TaxonomyCleanRuleRespVO.class);
        // 3. 构建树形结构
        return buildTreeByType(voList);
    }

    @Override
    public Long getCount(String catCode) {
        return TaxonomyCleanRuleMapper.selectCount(Wrappers.lambdaQuery(TaxonomyCleanRuleDO.class)
                .likeRight(TaxonomyCleanRuleDO::getCatCode, catCode));
    }

    /**
     * 构建树形结构 - 以type字段作为父节点
     *
     * @param list 规则列表
     * @return 树形结构列表
     */
    private List<TaxonomyCleanRuleRespVO> buildTreeByType(List<TaxonomyCleanRuleRespVO> list) {
        List<TaxonomyCleanRuleRespVO> resultList = new ArrayList<>();
        // 创建type映射，用于存储相同type的节点
        Map<String, List<TaxonomyCleanRuleRespVO>> typeMap = list.stream()
                .collect(Collectors.groupingBy(TaxonomyCleanRuleRespVO::getType));

        // 遍历每个type分组
        for (Map.Entry<String, List<TaxonomyCleanRuleRespVO>> entry : typeMap.entrySet()) {
            String type = entry.getKey();
            List<TaxonomyCleanRuleRespVO> typeNodes = entry.getValue();
            for (TaxonomyCleanRuleRespVO typeNode : typeNodes) {
                typeNode.setDataType("2");
            }
            // 创建父节点
            TaxonomyCleanRuleRespVO parentNode = new TaxonomyCleanRuleRespVO();
            parentNode.setId(0L); // 设置一个特殊的ID
            parentNode.setType(type);
            parentNode.setDataType("1");
            String typeName = CleanRuleTypeEnum.getNameByType(type);
            parentNode.setName(typeName); // 设置父节点名称
            parentNode.setChildren(new ArrayList<>(typeNodes));

            resultList.add(parentNode);
        }

        return resultList;
    }
}
