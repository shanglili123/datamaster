package com.datamaster.module.assets.convert.assetchild.files;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.files.AssetsAssetFilesDO;

import java.util.List;

/**
 * - Convert
 *
 * @author DATAMASTER
 * @date 2025-06-26
 */
@Mapper
public interface AssetsAssetFilesConvert {
    AssetsAssetFilesConvert INSTANCE = Mappers.getMapper(AssetsAssetFilesConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetFilesPageReqVO
     * @return AssetsAssetFilesDO
     */
     AssetsAssetFilesDO convertToDO(AssetsAssetFilesPageReqVO AssetsAssetFilesPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetFilesSaveReqVO
     * @return AssetsAssetFilesDO
     */
     AssetsAssetFilesDO convertToDO(AssetsAssetFilesSaveReqVO AssetsAssetFilesSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetFilesDO
     * @return AssetsAssetFilesRespVO
     */
     AssetsAssetFilesRespVO convertToRespVO(AssetsAssetFilesDO AssetsAssetFilesDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetFilesDOList
     * @return List<AssetsAssetFilesRespVO>
     */
     List<AssetsAssetFilesRespVO> convertToRespVOList(List<AssetsAssetFilesDO> AssetsAssetFilesDOList);
}
