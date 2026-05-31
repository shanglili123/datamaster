package com.datamaster.module.assets.convert.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTableRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTableSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTableDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Mapper
public interface AssetsDiscoveryTableConvert {
    AssetsDiscoveryTableConvert INSTANCE = Mappers.getMapper(AssetsDiscoveryTableConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDiscoveryTablePageReqVO
     * @return AssetsDiscoveryTableDO
     */
     AssetsDiscoveryTableDO convertToDO(AssetsDiscoveryTablePageReqVO AssetsDiscoveryTablePageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDiscoveryTableSaveReqVO
     * @return AssetsDiscoveryTableDO
     */
     AssetsDiscoveryTableDO convertToDO(AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTableSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDiscoveryTableDO
     * @return AssetsDiscoveryTableRespVO
     */
     AssetsDiscoveryTableRespVO convertToRespVO(AssetsDiscoveryTableDO AssetsDiscoveryTableDO);

    /**
     * DOList  RespVOList
     * @param AssetsDiscoveryTableDOList
     * @return List<AssetsDiscoveryTableRespVO>
     */
     List<AssetsDiscoveryTableRespVO> convertToRespVOList(List<AssetsDiscoveryTableDO> AssetsDiscoveryTableDOList);
}
