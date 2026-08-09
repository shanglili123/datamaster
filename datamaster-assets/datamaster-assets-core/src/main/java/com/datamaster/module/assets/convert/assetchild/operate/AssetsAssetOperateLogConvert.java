package com.datamaster.module.assets.convert.assetchild.operate;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateLogDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Mapper
public interface AssetsAssetOperateLogConvert {
    AssetsAssetOperateLogConvert INSTANCE = Mappers.getMapper(AssetsAssetOperateLogConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetOperateLogPageReqVO
     * @return AssetsAssetOperateLogDO
     */
     AssetsAssetOperateLogDO convertToDO(AssetsAssetOperateLogPageReqVO AssetsAssetOperateLogPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetOperateLogSaveReqVO
     * @return AssetsAssetOperateLogDO
     */
     AssetsAssetOperateLogDO convertToDO(AssetsAssetOperateLogSaveReqVO AssetsAssetOperateLogSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetOperateLogDO
     * @return AssetsAssetOperateLogRespVO
     */
     AssetsAssetOperateLogRespVO convertToRespVO(AssetsAssetOperateLogDO AssetsAssetOperateLogDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetOperateLogDOList
     * @return List<AssetsAssetOperateLogRespVO>
     */
     List<AssetsAssetOperateLogRespVO> convertToRespVOList(List<AssetsAssetOperateLogDO> AssetsAssetOperateLogDOList);
}
