

package com.datamaster.module.taxonomy.convert.rule;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyAuditRuleDO;

import java.util.List;

/**
 * 稽查规则 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyAuditRuleConvert {
    TaxonomyAuditRuleConvert INSTANCE = Mappers.getMapper(TaxonomyAuditRuleConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyAuditRulePageReqVO 请求参数
     * @return TaxonomyAuditRuleDO
     */
     TaxonomyAuditRuleDO convertToDO(TaxonomyAuditRulePageReqVO TaxonomyAuditRulePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyAuditRuleSaveReqVO 保存请求参数
     * @return TaxonomyAuditRuleDO
     */
     TaxonomyAuditRuleDO convertToDO(TaxonomyAuditRuleSaveReqVO TaxonomyAuditRuleSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyAuditRuleDO 实体对象
     * @return TaxonomyAuditRuleRespVO
     */
     TaxonomyAuditRuleRespVO convertToRespVO(TaxonomyAuditRuleDO TaxonomyAuditRuleDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyAuditRuleDOList 实体对象列表
     * @return List<TaxonomyAuditRuleRespVO>
     */
     List<TaxonomyAuditRuleRespVO> convertToRespVOList(List<TaxonomyAuditRuleDO> TaxonomyAuditRuleDOList);
}
