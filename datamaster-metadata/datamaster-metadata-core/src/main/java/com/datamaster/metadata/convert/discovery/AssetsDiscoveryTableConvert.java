package com.datamaster.metadata.convert.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTableRespVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTableSaveReqVO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTableDO;

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
