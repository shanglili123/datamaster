package com.datamaster.module.assets.convert.datasource;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;

import java.util.List;

/**
 *  Convert
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Mapper
public interface AssetsDatasourceSpaceRelConvert {
    AssetsDatasourceSpaceRelConvert INSTANCE = Mappers.getMapper(AssetsDatasourceSpaceRelConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDatasourceSpaceRelPageReqVO
     * @return AssetsDatasourceSpaceRelDO
     */
     AssetsDatasourceSpaceRelDO convertToDO(AssetsDatasourceSpaceRelPageReqVO AssetsDatasourceSpaceRelPageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDatasourceSpaceRelSaveReqVO
     * @return AssetsDatasourceSpaceRelDO
     */
     AssetsDatasourceSpaceRelDO convertToDO(AssetsDatasourceSpaceRelSaveReqVO AssetsDatasourceSpaceRelSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDatasourceSpaceRelDO
     * @return AssetsDatasourceSpaceRelRespVO
     */
     AssetsDatasourceSpaceRelRespVO convertToRespVO(AssetsDatasourceSpaceRelDO AssetsDatasourceSpaceRelDO);

    /**
     * DOList  RespVOList
     * @param AssetsDatasourceSpaceRelDOList
     * @return List<AssetsDatasourceSpaceRelRespVO>
     */
     List<AssetsDatasourceSpaceRelRespVO> convertToRespVOList(List<AssetsDatasourceSpaceRelDO> AssetsDatasourceSpaceRelDOList);
}
