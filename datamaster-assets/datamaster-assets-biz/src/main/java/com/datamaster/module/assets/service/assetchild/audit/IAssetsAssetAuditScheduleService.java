package com.datamaster.module.assets.service.assetchild.audit;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditSchedulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditScheduleDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
public interface IAssetsAssetAuditScheduleService extends IService<AssetsAssetAuditScheduleDO> {

    /**
     *
     *
     * @param pageReqVO
     * @return
     */
    PageResult<AssetsAssetAuditScheduleDO> getDaAssetAuditSchedulePage(AssetsAssetAuditSchedulePageReqVO pageReqVO);

    /**
     *
     *
     * @param createReqVO
     * @return
     */
    Long createDaAssetAuditSchedule(AssetsAssetAuditScheduleSaveReqVO createReqVO);

    /**
     *
     *
     * @param updateReqVO
     */
    int updateDaAssetAuditSchedule(AssetsAssetAuditScheduleSaveReqVO updateReqVO);

    /**
     *
     *
     * @param idList
     */
    int removeDaAssetAuditSchedule(Collection<Long> idList);

    /**
     *
     *
     * @param id
     * @return
     */
    AssetsAssetAuditScheduleDO getDaAssetAuditScheduleById(Long id);

    /**
     *
     *
     * @return
     */
    List<AssetsAssetAuditScheduleDO> getDaAssetAuditScheduleList();

    /**
     *  Map
     *
     * @return  Map
     */
    Map<Long, AssetsAssetAuditScheduleDO> getDaAssetAuditScheduleMap();

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    String importDaAssetAuditSchedule(List<AssetsAssetAuditScheduleRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
