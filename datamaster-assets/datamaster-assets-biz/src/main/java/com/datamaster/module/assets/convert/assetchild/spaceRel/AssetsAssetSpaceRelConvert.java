package com.datamaster.module.assets.convert.assetchild.spaceRel;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.spaceRel.vo.AssetsAssetSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel.AssetsAssetSpaceRelDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Mapper
public interface AssetsAssetSpaceRelConvert {
    AssetsAssetSpaceRelConvert INSTANCE = Mappers.getMapper(AssetsAssetSpaceRelConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetSpaceRelPageReqVO
     * @return AssetsAssetSpaceRelDO
     */
     AssetsAssetSpaceRelDO convertToDO(AssetsAssetSpaceRelPageReqVO AssetsAssetSpaceRelPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetSpaceRelSaveReqVO
     * @return AssetsAssetSpaceRelDO
     */
     AssetsAssetSpaceRelDO convertToDO(AssetsAssetSpaceRelSaveReqVO AssetsAssetSpaceRelSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetSpaceRelDO
     * @return AssetsAssetSpaceRelRespVO
     */
     AssetsAssetSpaceRelRespVO convertToRespVO(AssetsAssetSpaceRelDO AssetsAssetSpaceRelDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetSpaceRelDOList
     * @return List<AssetsAssetSpaceRelRespVO>
     */
     List<AssetsAssetSpaceRelRespVO> convertToRespVOList(List<AssetsAssetSpaceRelDO> AssetsAssetSpaceRelDOList);
}
