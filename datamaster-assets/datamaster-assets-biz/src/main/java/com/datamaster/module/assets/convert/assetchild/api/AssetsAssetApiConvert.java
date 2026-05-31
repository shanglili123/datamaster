package com.datamaster.module.assets.convert.assetchild.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiDO;

import java.util.List;

/**
 * -API Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface AssetsAssetApiConvert {
    AssetsAssetApiConvert INSTANCE = Mappers.getMapper(AssetsAssetApiConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetApiPageReqVO
     * @return AssetsAssetApiDO
     */
     AssetsAssetApiDO convertToDO(AssetsAssetApiPageReqVO AssetsAssetApiPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetApiSaveReqVO
     * @return AssetsAssetApiDO
     */
     AssetsAssetApiDO convertToDO(AssetsAssetApiSaveReqVO AssetsAssetApiSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetApiDO
     * @return AssetsAssetApiRespVO
     */
     AssetsAssetApiRespVO convertToRespVO(AssetsAssetApiDO AssetsAssetApiDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetApiDOList
     * @return List<AssetsAssetApiRespVO>
     */
     List<AssetsAssetApiRespVO> convertToRespVOList(List<AssetsAssetApiDO> AssetsAssetApiDOList);
}
