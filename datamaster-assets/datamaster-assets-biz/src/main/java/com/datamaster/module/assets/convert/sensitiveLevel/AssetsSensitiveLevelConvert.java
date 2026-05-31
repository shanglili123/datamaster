package com.datamaster.module.assets.convert.sensitiveLevel;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelPageReqVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelRespVO;
import com.datamaster.module.assets.controller.admin.sensitiveLevel.vo.AssetsSensitiveLevelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.sensitiveLevel.AssetsSensitiveLevelDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface AssetsSensitiveLevelConvert {
    AssetsSensitiveLevelConvert INSTANCE = Mappers.getMapper(AssetsSensitiveLevelConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsSensitiveLevelPageReqVO
     * @return AssetsSensitiveLevelDO
     */
     AssetsSensitiveLevelDO convertToDO(AssetsSensitiveLevelPageReqVO AssetsSensitiveLevelPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsSensitiveLevelSaveReqVO
     * @return AssetsSensitiveLevelDO
     */
     AssetsSensitiveLevelDO convertToDO(AssetsSensitiveLevelSaveReqVO AssetsSensitiveLevelSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsSensitiveLevelDO
     * @return AssetsSensitiveLevelRespVO
     */
     AssetsSensitiveLevelRespVO convertToRespVO(AssetsSensitiveLevelDO AssetsSensitiveLevelDO);

    /**
     * DOList  RespVOList
     * @param AssetsSensitiveLevelDOList
     * @return List<AssetsSensitiveLevelRespVO>
     */
     List<AssetsSensitiveLevelRespVO> convertToRespVOList(List<AssetsSensitiveLevelDO> AssetsSensitiveLevelDOList);
}
