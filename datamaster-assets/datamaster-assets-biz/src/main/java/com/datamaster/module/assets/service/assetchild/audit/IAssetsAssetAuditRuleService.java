package com.datamaster.module.assets.service.assetchild.audit;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditRuleDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface IAssetsAssetAuditRuleService extends IService<AssetsAssetAuditRuleDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetAuditRuleDO> getDaAssetAuditRulePage(AssetsAssetAuditRulePageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetAuditRule(AssetsAssetAuditRuleSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetAuditRule(AssetsAssetAuditRuleSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetAuditRule(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetAuditRuleDO getDaAssetAuditRuleById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetAuditRuleDO> getDaAssetAuditRuleList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetAuditRuleDO> getDaAssetAuditRuleMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetAuditRule(List<AssetsAssetAuditRuleRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
