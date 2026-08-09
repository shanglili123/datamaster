package com.datamaster.module.assets.convert.assetchild.audit;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditSchedulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditScheduleDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Mapper
public interface AssetsAssetAuditScheduleConvert {
    AssetsAssetAuditScheduleConvert INSTANCE = Mappers.getMapper(AssetsAssetAuditScheduleConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetAuditSchedulePageReqVO
     * @return AssetsAssetAuditScheduleDO
     */
     AssetsAssetAuditScheduleDO convertToDO(AssetsAssetAuditSchedulePageReqVO AssetsAssetAuditSchedulePageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetAuditScheduleSaveReqVO
     * @return AssetsAssetAuditScheduleDO
     */
     AssetsAssetAuditScheduleDO convertToDO(AssetsAssetAuditScheduleSaveReqVO AssetsAssetAuditScheduleSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetAuditScheduleDO
     * @return AssetsAssetAuditScheduleRespVO
     */
     AssetsAssetAuditScheduleRespVO convertToRespVO(AssetsAssetAuditScheduleDO AssetsAssetAuditScheduleDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetAuditScheduleDOList
     * @return List<AssetsAssetAuditScheduleRespVO>
     */
     List<AssetsAssetAuditScheduleRespVO> convertToRespVOList(List<AssetsAssetAuditScheduleDO> AssetsAssetAuditScheduleDOList);
}
