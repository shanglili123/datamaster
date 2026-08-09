package com.datamaster.metadata.convert.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskRespVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskSaveReqVO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTaskDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Mapper
public interface AssetsDiscoveryTaskConvert {
    AssetsDiscoveryTaskConvert INSTANCE = Mappers.getMapper(AssetsDiscoveryTaskConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDiscoveryTaskPageReqVO
     * @return AssetsDiscoveryTaskDO
     */
     AssetsDiscoveryTaskDO convertToDO(AssetsDiscoveryTaskPageReqVO AssetsDiscoveryTaskPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDiscoveryTaskSaveReqVO
     * @return AssetsDiscoveryTaskDO
     */
     AssetsDiscoveryTaskDO convertToDO(AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTaskSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDiscoveryTaskDO
     * @return AssetsDiscoveryTaskRespVO
     */
     AssetsDiscoveryTaskRespVO convertToRespVO(AssetsDiscoveryTaskDO AssetsDiscoveryTaskDO);

    /**
     * DOList  RespVOList
     * @param AssetsDiscoveryTaskDOList
     * @return List<AssetsDiscoveryTaskRespVO>
     */
     List<AssetsDiscoveryTaskRespVO> convertToRespVOList(List<AssetsDiscoveryTaskDO> AssetsDiscoveryTaskDOList);
}
