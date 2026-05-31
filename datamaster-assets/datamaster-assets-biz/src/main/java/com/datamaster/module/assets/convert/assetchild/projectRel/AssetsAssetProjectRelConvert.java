package com.datamaster.module.assets.convert.assetchild.projectRel;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.projectRel.vo.AssetsAssetProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.projectRel.AssetsAssetProjectRelDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Mapper
public interface AssetsAssetProjectRelConvert {
    AssetsAssetProjectRelConvert INSTANCE = Mappers.getMapper(AssetsAssetProjectRelConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetProjectRelPageReqVO
     * @return AssetsAssetProjectRelDO
     */
     AssetsAssetProjectRelDO convertToDO(AssetsAssetProjectRelPageReqVO AssetsAssetProjectRelPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetProjectRelSaveReqVO
     * @return AssetsAssetProjectRelDO
     */
     AssetsAssetProjectRelDO convertToDO(AssetsAssetProjectRelSaveReqVO AssetsAssetProjectRelSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetProjectRelDO
     * @return AssetsAssetProjectRelRespVO
     */
     AssetsAssetProjectRelRespVO convertToRespVO(AssetsAssetProjectRelDO AssetsAssetProjectRelDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetProjectRelDOList
     * @return List<AssetsAssetProjectRelRespVO>
     */
     List<AssetsAssetProjectRelRespVO> convertToRespVOList(List<AssetsAssetProjectRelDO> AssetsAssetProjectRelDOList);
}
