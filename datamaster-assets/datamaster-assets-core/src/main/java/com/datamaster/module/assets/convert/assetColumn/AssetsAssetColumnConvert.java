package com.datamaster.module.assets.convert.assetColumn;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnRespVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;

import java.util.List;

/**
 *  Convert
 *
 * @author lhs
 * @date 2025-01-21
 */
@Mapper
public interface AssetsAssetColumnConvert {
    AssetsAssetColumnConvert INSTANCE = Mappers.getMapper(AssetsAssetColumnConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetColumnPageReqVO
     * @return AssetsAssetColumnDO
     */
     AssetsAssetColumnDO convertToDO(AssetsAssetColumnPageReqVO AssetsAssetColumnPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetColumnSaveReqVO
     * @return AssetsAssetColumnDO
     */
     AssetsAssetColumnDO convertToDO(AssetsAssetColumnSaveReqVO AssetsAssetColumnSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetColumnDO
     * @return AssetsAssetColumnRespVO
     */
     AssetsAssetColumnRespVO convertToRespVO(AssetsAssetColumnDO AssetsAssetColumnDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetColumnDOList
     * @return List<AssetsAssetColumnRespVO>
     */
     List<AssetsAssetColumnRespVO> convertToRespVOList(List<AssetsAssetColumnDO> AssetsAssetColumnDOList);
}
