package com.datamaster.module.assets.convert.assetchild.gis;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.gis.AssetsAssetGisDO;

import java.util.List;

/**
 * - Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface AssetsAssetGisConvert {
    AssetsAssetGisConvert INSTANCE = Mappers.getMapper(AssetsAssetGisConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetGisPageReqVO
     * @return AssetsAssetGisDO
     */
     AssetsAssetGisDO convertToDO(AssetsAssetGisPageReqVO AssetsAssetGisPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetGisSaveReqVO
     * @return AssetsAssetGisDO
     */
     AssetsAssetGisDO convertToDO(AssetsAssetGisSaveReqVO AssetsAssetGisSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetGisDO
     * @return AssetsAssetGisRespVO
     */
     AssetsAssetGisRespVO convertToRespVO(AssetsAssetGisDO AssetsAssetGisDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetGisDOList
     * @return List<AssetsAssetGisRespVO>
     */
     List<AssetsAssetGisRespVO> convertToRespVOList(List<AssetsAssetGisDO> AssetsAssetGisDOList);
}
