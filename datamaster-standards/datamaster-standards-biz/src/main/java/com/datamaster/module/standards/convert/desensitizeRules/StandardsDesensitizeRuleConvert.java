

package com.datamaster.module.standards.convert.desensitizeRules;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRulePageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;

/**
 * 脱敏规则 Convert
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Mapper
public interface StandardsDesensitizeRuleConvert {
    StandardsDesensitizeRuleConvert INSTANCE = Mappers.getMapper(StandardsDesensitizeRuleConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDesensitizeRulePageReqVO 请求参数
     * @return StandardsDesensitizeRuleDO
     */
     StandardsDesensitizeRuleDO convertToDO(StandardsDesensitizeRulePageReqVO StandardsDesensitizeRulePageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDesensitizeRuleSaveReqVO 保存请求参数
     * @return StandardsDesensitizeRuleDO
     */
     StandardsDesensitizeRuleDO convertToDO(StandardsDesensitizeRuleSaveReqVO StandardsDesensitizeRuleSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDesensitizeRuleDO 实体对象
     * @return StandardsDesensitizeRuleRespVO
     */
     StandardsDesensitizeRuleRespVO convertToRespVO(StandardsDesensitizeRuleDO StandardsDesensitizeRuleDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDesensitizeRuleDOList 实体对象列表
     * @return List<StandardsDesensitizeRuleRespVO>
     */
     List<StandardsDesensitizeRuleRespVO> convertToRespVOList(List<StandardsDesensitizeRuleDO> StandardsDesensitizeRuleDOList);
}
