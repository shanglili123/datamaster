package com.datamaster.module.assets.convert.assetchild.geo;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.geo.AssetsAssetGeoDO;

import java.util.List;

/**
 * - Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface AssetsAssetGeoConvert {
    AssetsAssetGeoConvert INSTANCE = Mappers.getMapper(AssetsAssetGeoConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetGeoPageReqVO
     * @return AssetsAssetGeoDO
     */
     AssetsAssetGeoDO convertToDO(AssetsAssetGeoPageReqVO AssetsAssetGeoPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetGeoSaveReqVO
     * @return AssetsAssetGeoDO
     */
     AssetsAssetGeoDO convertToDO(AssetsAssetGeoSaveReqVO AssetsAssetGeoSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetGeoDO
     * @return AssetsAssetGeoRespVO
     */
     AssetsAssetGeoRespVO convertToRespVO(AssetsAssetGeoDO AssetsAssetGeoDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetGeoDOList
     * @return List<AssetsAssetGeoRespVO>
     */
     List<AssetsAssetGeoRespVO> convertToRespVOList(List<AssetsAssetGeoDO> AssetsAssetGeoDOList);
}
