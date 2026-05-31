package com.datamaster.module.assets.convert.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryColumnDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Mapper
public interface AssetsDiscoveryColumnConvert {
    AssetsDiscoveryColumnConvert INSTANCE = Mappers.getMapper(AssetsDiscoveryColumnConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDiscoveryColumnPageReqVO
     * @return AssetsDiscoveryColumnDO
     */
     AssetsDiscoveryColumnDO convertToDO(AssetsDiscoveryColumnPageReqVO AssetsDiscoveryColumnPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDiscoveryColumnSaveReqVO
     * @return AssetsDiscoveryColumnDO
     */
     AssetsDiscoveryColumnDO convertToDO(AssetsDiscoveryColumnSaveReqVO AssetsDiscoveryColumnSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDiscoveryColumnDO
     * @return AssetsDiscoveryColumnRespVO
     */
     AssetsDiscoveryColumnRespVO convertToRespVO(AssetsDiscoveryColumnDO AssetsDiscoveryColumnDO);

    /**
     * DOList  RespVOList
     * @param AssetsDiscoveryColumnDOList
     * @return List<AssetsDiscoveryColumnRespVO>
     */
     List<AssetsDiscoveryColumnRespVO> convertToRespVOList(List<AssetsDiscoveryColumnDO> AssetsDiscoveryColumnDOList);
}
