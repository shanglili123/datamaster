package com.datamaster.module.assets.convert.datasource;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Mapper
public interface AssetsDatasourceProjectRelConvert {
    AssetsDatasourceProjectRelConvert INSTANCE = Mappers.getMapper(AssetsDatasourceProjectRelConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDatasourceProjectRelPageReqVO
     * @return AssetsDatasourceProjectRelDO
     */
     AssetsDatasourceProjectRelDO convertToDO(AssetsDatasourceProjectRelPageReqVO AssetsDatasourceProjectRelPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDatasourceProjectRelSaveReqVO
     * @return AssetsDatasourceProjectRelDO
     */
     AssetsDatasourceProjectRelDO convertToDO(AssetsDatasourceProjectRelSaveReqVO AssetsDatasourceProjectRelSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDatasourceProjectRelDO
     * @return AssetsDatasourceProjectRelRespVO
     */
     AssetsDatasourceProjectRelRespVO convertToRespVO(AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO);

    /**
     * DOList  RespVOList
     * @param AssetsDatasourceProjectRelDOList
     * @return List<AssetsDatasourceProjectRelRespVO>
     */
     List<AssetsDatasourceProjectRelRespVO> convertToRespVOList(List<AssetsDatasourceProjectRelDO> AssetsDatasourceProjectRelDOList);
}
