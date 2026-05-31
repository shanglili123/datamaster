

package com.datamaster.module.taxonomy.service.rule;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyCleanCatDO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyCleanRuleDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 清洗规则Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface ITaxonomyCleanRuleService extends IService<TaxonomyCleanRuleDO> {

    /**
     * 获得清洗规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 清洗规则分页列表
     */
    PageResult<TaxonomyCleanRuleDO> getAttCleanRulePage(TaxonomyCleanRulePageReqVO pageReqVO);

    /**
     * 创建清洗规则
     *
     * @param createReqVO 清洗规则信息
     * @return 清洗规则编号
     */
    Long createAttCleanRule(TaxonomyCleanRuleSaveReqVO createReqVO);

    /**
     * 更新清洗规则
     *
     * @param updateReqVO 清洗规则信息
     */
    int updateAttCleanRule(TaxonomyCleanRuleSaveReqVO updateReqVO);

    /**
     * 删除清洗规则
     *
     * @param idList 清洗规则编号
     */
    int removeAttCleanRule(Collection<Long> idList);

    /**
     * 获得清洗规则详情
     *
     * @param id 清洗规则编号
     * @return 清洗规则
     */
    TaxonomyCleanRuleDO getAttCleanRuleById(Long id);

    /**
     * 获得全部清洗规则列表
     *
     * @return 清洗规则列表
     */
    List<TaxonomyCleanRuleDO> getAttCleanRuleList();
    List<TaxonomyCleanRuleRespVO> getAttCleanRuleList(TaxonomyCleanRulePageReqVO TaxonomyCleanRule);

    /**
     * 获得全部清洗规则 Map
     *
     * @return 清洗规则 Map
     */
    Map<Long, TaxonomyCleanRuleDO> getAttCleanRuleMap();

    /**
     * 导入清洗规则数据
     *
     * @param importExcelList 清洗规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importAttCleanRule(List<TaxonomyCleanRuleRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获取清洗规则树形结构
     *
     * @return 树形结构列表
     */
    List<TaxonomyCleanRuleRespVO> getAttCleanRuleTree(Long dataElemId);

    List<TaxonomyCleanRuleRespVO> getCleaningRuleTree(Long[] dataElemId);

    /**
     * @param catCode {@link TaxonomyCleanCatDO#code}
     */
    Long getCount(String catCode);

}
