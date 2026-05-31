package com.datamaster.module.assets.convert.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskDO;

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
