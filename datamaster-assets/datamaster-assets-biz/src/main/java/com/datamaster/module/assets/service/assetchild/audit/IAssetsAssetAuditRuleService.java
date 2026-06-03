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
    PageResult<AssetsAssetAuditRuleDO> getAssetAuditRulePage(AssetsAssetAuditRulePageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createAssetAuditRule(AssetsAssetAuditRuleSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateAssetAuditRule(AssetsAssetAuditRuleSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeAssetAuditRule(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetAuditRuleDO getAssetAuditRuleById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetAuditRuleDO> getAssetAuditRuleList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetAuditRuleDO> getAssetAuditRuleMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importAssetAuditRule(List<AssetsAssetAuditRuleRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
