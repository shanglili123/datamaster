package com.datamaster.module.assets.convert.assetchild.operate;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplyRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateApplySaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateApplyDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Mapper
public interface AssetsAssetOperateApplyConvert {
    AssetsAssetOperateApplyConvert INSTANCE = Mappers.getMapper(AssetsAssetOperateApplyConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsAssetOperateApplyPageReqVO
     * @return AssetsAssetOperateApplyDO
     */
     AssetsAssetOperateApplyDO convertToDO(AssetsAssetOperateApplyPageReqVO AssetsAssetOperateApplyPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsAssetOperateApplySaveReqVO
     * @return AssetsAssetOperateApplyDO
     */
     AssetsAssetOperateApplyDO convertToDO(AssetsAssetOperateApplySaveReqVO AssetsAssetOperateApplySaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsAssetOperateApplyDO
     * @return AssetsAssetOperateApplyRespVO
     */
     AssetsAssetOperateApplyRespVO convertToRespVO(AssetsAssetOperateApplyDO AssetsAssetOperateApplyDO);

    /**
     * DOList  RespVOList
     * @param AssetsAssetOperateApplyDOList
     * @return List<AssetsAssetOperateApplyRespVO>
     */
     List<AssetsAssetOperateApplyRespVO> convertToRespVOList(List<AssetsAssetOperateApplyDO> AssetsAssetOperateApplyDOList);
}
