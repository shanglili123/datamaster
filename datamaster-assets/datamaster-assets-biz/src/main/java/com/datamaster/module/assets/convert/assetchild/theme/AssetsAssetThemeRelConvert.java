package com.datamaster.module.assets.convert.assetchild.theme;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.theme.AssetsAssetThemeRelDO;

import java.util.List;

/**
 * - Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface AssetsAssetThemeRelConvert {
    AssetsAssetThemeRelConvert INSTANCE = Mappers.getMapper(AssetsAssetThemeRelConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetThemeRelPageReqVO
     * @return AssetsAssetThemeRelDO
     */
     AssetsAssetThemeRelDO convertToDO(AssetsAssetThemeRelPageReqVO AssetsAssetThemeRelPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetThemeRelSaveReqVO
     * @return AssetsAssetThemeRelDO
     */
     AssetsAssetThemeRelDO convertToDO(AssetsAssetThemeRelSaveReqVO AssetsAssetThemeRelSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetThemeRelDO
     * @return AssetsAssetThemeRelRespVO
     */
     AssetsAssetThemeRelRespVO convertToRespVO(AssetsAssetThemeRelDO AssetsAssetThemeRelDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetThemeRelDOList
     * @return List<AssetsAssetThemeRelRespVO>
     */
     List<AssetsAssetThemeRelRespVO> convertToRespVOList(List<AssetsAssetThemeRelDO> AssetsAssetThemeRelDOList);
}
