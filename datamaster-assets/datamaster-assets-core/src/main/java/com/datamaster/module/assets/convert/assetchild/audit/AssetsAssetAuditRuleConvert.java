package com.datamaster.module.assets.convert.assetchild.audit;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditRuleDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Mapper
public interface AssetsAssetAuditRuleConvert {
    AssetsAssetAuditRuleConvert INSTANCE = Mappers.getMapper(AssetsAssetAuditRuleConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetAuditRulePageReqVO
     * @return AssetsAssetAuditRuleDO
     */
     AssetsAssetAuditRuleDO convertToDO(AssetsAssetAuditRulePageReqVO AssetsAssetAuditRulePageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetAuditRuleSaveReqVO
     * @return AssetsAssetAuditRuleDO
     */
     AssetsAssetAuditRuleDO convertToDO(AssetsAssetAuditRuleSaveReqVO AssetsAssetAuditRuleSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetAuditRuleDO
     * @return AssetsAssetAuditRuleRespVO
     */
     AssetsAssetAuditRuleRespVO convertToRespVO(AssetsAssetAuditRuleDO AssetsAssetAuditRuleDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetAuditRuleDOList
     * @return List<AssetsAssetAuditRuleRespVO>
     */
     List<AssetsAssetAuditRuleRespVO> convertToRespVOList(List<AssetsAssetAuditRuleDO> AssetsAssetAuditRuleDOList);
}
