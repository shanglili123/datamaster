package com.datamaster.module.assets.service.assetchild.audit;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditAlertDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface IAssetsAssetAuditAlertService extends IService<AssetsAssetAuditAlertDO> {

    /**
     * -
     *
     * @param pageReqVO
     * @return -
     */
    PageResult<AssetsAssetAuditAlertDO> getDaAssetAuditAlertPage(AssetsAssetAuditAlertPageReqVO pageReqVO);

    /**
     * -
     *
     * @param createReqVO -
     * @return -
     */
    Long createDaAssetAuditAlert(AssetsAssetAuditAlertSaveReqVO createReqVO);

    /**
     * -
     *
     * @param updateReqVO -
     */
    int updateDaAssetAuditAlert(AssetsAssetAuditAlertSaveReqVO updateReqVO);

    /**
     * -
     *
     * @param idList -
     */
    int removeDaAssetAuditAlert(Collection<Long> idList);

    /**
     * -
     *
     * @param id -
     * @return -
     */
    AssetsAssetAuditAlertDO getDaAssetAuditAlertById(Long id);

    /**
     * -
     *
     * @return -
     */
    List<AssetsAssetAuditAlertDO> getDaAssetAuditAlertList();

    /**
     * - Map
     *
     * @return - Map
     */
    Map<Long, AssetsAssetAuditAlertDO> getDaAssetAuditAlertMap();

    /**
     * -
     *
     * @param importExcelList -
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetAuditAlert(List<AssetsAssetAuditAlertRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
