

package com.datamaster.module.taxonomy.convert.rule;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyCleanRuleDO;

import java.util.List;

/**
 * 清洗规则 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Mapper
public interface TaxonomyCleanRuleConvert {
    TaxonomyCleanRuleConvert INSTANCE = Mappers.getMapper(TaxonomyCleanRuleConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param TaxonomyCleanRulePageReqVO 请求参数
     * @return TaxonomyCleanRuleDO
     */
     TaxonomyCleanRuleDO convertToDO(TaxonomyCleanRulePageReqVO TaxonomyCleanRulePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param TaxonomyCleanRuleSaveReqVO 保存请求参数
     * @return TaxonomyCleanRuleDO
     */
     TaxonomyCleanRuleDO convertToDO(TaxonomyCleanRuleSaveReqVO TaxonomyCleanRuleSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param TaxonomyCleanRuleDO 实体对象
     * @return TaxonomyCleanRuleRespVO
     */
     TaxonomyCleanRuleRespVO convertToRespVO(TaxonomyCleanRuleDO TaxonomyCleanRuleDO);

    /**
     * DOList 转换为 RespVOList
     * @param TaxonomyCleanRuleDOList 实体对象列表
     * @return List<TaxonomyCleanRuleRespVO>
     */
     List<TaxonomyCleanRuleRespVO> convertToRespVOList(List<TaxonomyCleanRuleDO> TaxonomyCleanRuleDOList);
}
