package com.datamaster.module.assets.convert.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogPageReqVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogRespVO;
import com.datamaster.module.assets.controller.admin.discovery.vo.AssetsDiscoveryTaskLogSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTaskLogDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-02-17
 */
@Mapper
public interface AssetsDiscoveryTaskLogConvert {
    AssetsDiscoveryTaskLogConvert INSTANCE = Mappers.getMapper(AssetsDiscoveryTaskLogConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDiscoveryTaskLogPageReqVO
     * @return AssetsDiscoveryTaskLogDO
     */
     AssetsDiscoveryTaskLogDO convertToDO(AssetsDiscoveryTaskLogPageReqVO AssetsDiscoveryTaskLogPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDiscoveryTaskLogSaveReqVO
     * @return AssetsDiscoveryTaskLogDO
     */
     AssetsDiscoveryTaskLogDO convertToDO(AssetsDiscoveryTaskLogSaveReqVO AssetsDiscoveryTaskLogSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDiscoveryTaskLogDO
     * @return AssetsDiscoveryTaskLogRespVO
     */
     AssetsDiscoveryTaskLogRespVO convertToRespVO(AssetsDiscoveryTaskLogDO AssetsDiscoveryTaskLogDO);

    /**
     * DOList  RespVOList
     * @param AssetsDiscoveryTaskLogDOList
     * @return List<AssetsDiscoveryTaskLogRespVO>
     */
     List<AssetsDiscoveryTaskLogRespVO> convertToRespVOList(List<AssetsDiscoveryTaskLogDO> AssetsDiscoveryTaskLogDOList);
}
