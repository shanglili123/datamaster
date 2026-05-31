package com.datamaster.module.assets.convert.assetchild.audit;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditAlertDO;

import java.util.List;

/**
 * - Convert
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Mapper
public interface AssetsAssetAuditAlertConvert {
    AssetsAssetAuditAlertConvert INSTANCE = Mappers.getMapper(AssetsAssetAuditAlertConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetAuditAlertPageReqVO
     * @return AssetsAssetAuditAlertDO
     */
     AssetsAssetAuditAlertDO convertToDO(AssetsAssetAuditAlertPageReqVO AssetsAssetAuditAlertPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetAuditAlertSaveReqVO
     * @return AssetsAssetAuditAlertDO
     */
     AssetsAssetAuditAlertDO convertToDO(AssetsAssetAuditAlertSaveReqVO AssetsAssetAuditAlertSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetAuditAlertDO
     * @return AssetsAssetAuditAlertRespVO
     */
     AssetsAssetAuditAlertRespVO convertToRespVO(AssetsAssetAuditAlertDO AssetsAssetAuditAlertDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetAuditAlertDOList
     * @return List<AssetsAssetAuditAlertRespVO>
     */
     List<AssetsAssetAuditAlertRespVO> convertToRespVOList(List<AssetsAssetAuditAlertDO> AssetsAssetAuditAlertDOList);
}
