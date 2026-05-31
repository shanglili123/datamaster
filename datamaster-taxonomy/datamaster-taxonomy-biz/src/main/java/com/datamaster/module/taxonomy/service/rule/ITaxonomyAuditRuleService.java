

package com.datamaster.module.taxonomy.service.rule;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyAuditRuleDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 稽查规则Service接口
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
public interface ITaxonomyAuditRuleService extends IService<TaxonomyAuditRuleDO> {

    /**
     * 获得稽查规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 稽查规则分页列表
     */
    PageResult<TaxonomyAuditRuleDO> getAttAuditRulePage(TaxonomyAuditRulePageReqVO pageReqVO);

    /**
     * 创建稽查规则
     *
     * @param createReqVO 稽查规则信息
     * @return 稽查规则编号
     */
    Long createAttAuditRule(TaxonomyAuditRuleSaveReqVO createReqVO);

    /**
     * 更新稽查规则
     *
     * @param updateReqVO 稽查规则信息
     */
    int updateAttAuditRule(TaxonomyAuditRuleSaveReqVO updateReqVO);

    /**
     * 删除稽查规则
     *
     * @param idList 稽查规则编号
     */
    int removeAttAuditRule(Collection<Long> idList);

    /**
     * 获得稽查规则详情
     *
     * @param id 稽查规则编号
     * @return 稽查规则
     */
    TaxonomyAuditRuleDO getAttAuditRuleById(Long id);

    /**
     * 获得全部稽查规则列表
     *
     * @return 稽查规则列表
     */
    List<TaxonomyAuditRuleDO> getAttAuditRuleList();

    /**
     * 获得全部稽查规则 Map
     *
     * @return 稽查规则 Map
     */
    Map<Long, TaxonomyAuditRuleDO> getAttAuditRuleMap();

    /**
     * 导入稽查规则数据
     *
     * @param importExcelList 稽查规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importAttAuditRule(List<TaxonomyAuditRuleRespVO> importExcelList, boolean isUpdateSupport, String operName);

    /**
     * 获取稽查规则树形结构
     *
     * @return 树形结构列表
     */
    List<TaxonomyAuditRuleRespVO> getAttAuditRuleTree(Long dataElemId);
}
