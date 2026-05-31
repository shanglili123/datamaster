package com.datamaster.module.assets.convert.assetchild.api;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiParamDO;

import java.util.List;

/**
 * -API- Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface AssetsAssetApiParamConvert {
    AssetsAssetApiParamConvert INSTANCE = Mappers.getMapper(AssetsAssetApiParamConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetApiParamPageReqVO
     * @return AssetsAssetApiParamDO
     */
     AssetsAssetApiParamDO convertToDO(AssetsAssetApiParamPageReqVO AssetsAssetApiParamPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetApiParamSaveReqVO
     * @return AssetsAssetApiParamDO
     */
     AssetsAssetApiParamDO convertToDO(AssetsAssetApiParamSaveReqVO AssetsAssetApiParamSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetApiParamDO
     * @return AssetsAssetApiParamRespVO
     */
     AssetsAssetApiParamRespVO convertToRespVO(AssetsAssetApiParamDO AssetsAssetApiParamDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetApiParamDOList
     * @return List<AssetsAssetApiParamRespVO>
     */
     List<AssetsAssetApiParamRespVO> convertToRespVOList(List<AssetsAssetApiParamDO> AssetsAssetApiParamDOList);
}
