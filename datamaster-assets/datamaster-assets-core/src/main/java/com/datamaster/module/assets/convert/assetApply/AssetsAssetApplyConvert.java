package com.datamaster.module.assets.convert.assetApply;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetApply.vo.AssetsAssetApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetApply.AssetsAssetApplyDO;

import java.util.List;

/**
 *  Convert
 *
 * @author shu
 * @date 2025-03-19
 */
@Mapper
public interface AssetsAssetApplyConvert {
    AssetsAssetApplyConvert INSTANCE = Mappers.getMapper(AssetsAssetApplyConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetApplyPageReqVO
     * @return AssetsAssetApplyDO
     */
     AssetsAssetApplyDO convertToDO(AssetsAssetApplyPageReqVO AssetsAssetApplyPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetApplySaveReqVO
     * @return AssetsAssetApplyDO
     */
     AssetsAssetApplyDO convertToDO(AssetsAssetApplySaveReqVO AssetsAssetApplySaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetApplyDO
     * @return AssetsAssetApplyRespVO
     */
     AssetsAssetApplyRespVO convertToRespVO(AssetsAssetApplyDO AssetsAssetApplyDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetApplyDOList
     * @return List<AssetsAssetApplyRespVO>
     */
     List<AssetsAssetApplyRespVO> convertToRespVOList(List<AssetsAssetApplyDO> AssetsAssetApplyDOList);
}
