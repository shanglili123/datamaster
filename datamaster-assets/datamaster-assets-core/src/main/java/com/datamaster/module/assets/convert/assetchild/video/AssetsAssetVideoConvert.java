package com.datamaster.module.assets.convert.assetchild.video;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.video.AssetsAssetVideoDO;

import java.util.List;

/**
 * - Convert
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Mapper
public interface AssetsAssetVideoConvert {
    AssetsAssetVideoConvert INSTANCE = Mappers.getMapper(AssetsAssetVideoConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetVideoPageReqVO
     * @return AssetsAssetVideoDO
     */
     AssetsAssetVideoDO convertToDO(AssetsAssetVideoPageReqVO AssetsAssetVideoPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetVideoSaveReqVO
     * @return AssetsAssetVideoDO
     */
     AssetsAssetVideoDO convertToDO(AssetsAssetVideoSaveReqVO AssetsAssetVideoSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetVideoDO
     * @return AssetsAssetVideoRespVO
     */
     AssetsAssetVideoRespVO convertToRespVO(AssetsAssetVideoDO AssetsAssetVideoDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetVideoDOList
     * @return List<AssetsAssetVideoRespVO>
     */
     List<AssetsAssetVideoRespVO> convertToRespVOList(List<AssetsAssetVideoDO> AssetsAssetVideoDOList);
}
