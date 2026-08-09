package com.datamaster.module.assets.convert.asset;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetPageReqVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetRespVO;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;

import java.util.List;

/**
 *  Convert
 *
 * @author lhs
 * @date 2025-01-21
 */
@Mapper
public interface AssetsAssetConvert {
    AssetsAssetConvert INSTANCE = Mappers.getMapper(AssetsAssetConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetPageReqVO
     * @return AssetsAssetDO
     */
     AssetsAssetDO convertToDO(AssetsAssetPageReqVO AssetsAssetPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetSaveReqVO
     * @return AssetsAssetDO
     */
     AssetsAssetDO convertToDO(AssetsAssetSaveReqVO AssetsAssetSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetDO
     * @return AssetsAssetRespVO
     */
     AssetsAssetRespVO convertToRespVO(AssetsAssetDO AssetsAssetDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetDOList
     * @return List<AssetsAssetRespVO>
     */
     List<AssetsAssetRespVO> convertToRespVOList(List<AssetsAssetDO> AssetsAssetDOList);
}
