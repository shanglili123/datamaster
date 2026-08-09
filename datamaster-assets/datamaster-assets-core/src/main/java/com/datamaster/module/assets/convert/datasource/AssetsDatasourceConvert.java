package com.datamaster.module.assets.convert.datasource;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourcePageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceDO;

import java.util.List;

/**
 *  Convert
 *
 * @author lhs
 * @date 2025-01-21
 */
@Mapper
public interface AssetsDatasourceConvert {
    AssetsDatasourceConvert INSTANCE = Mappers.getMapper(AssetsDatasourceConvert.class);

    /**
     * PageReqVO  DO
     * @param AssetsDatasourcePageReqVO
     * @return AssetsDatasourceDO
     */
     AssetsDatasourceDO convertToDO(AssetsDatasourcePageReqVO AssetsDatasourcePageReqVO);

    /**
     * SaveReqVO  DO
     * @param AssetsDatasourceSaveReqVO
     * @return AssetsDatasourceDO
     */
     AssetsDatasourceDO convertToDO(AssetsDatasourceSaveReqVO AssetsDatasourceSaveReqVO);

    /**
     * DO  RespVO
     * @param AssetsDatasourceDO
     * @return AssetsDatasourceRespVO
     */
     AssetsDatasourceRespVO convertToRespVO(AssetsDatasourceDO AssetsDatasourceDO);

    /**
     * DOList  RespVOList
     * @param AssetsDatasourceDOList
     * @return List<AssetsDatasourceRespVO>
     */
     List<AssetsDatasourceRespVO> convertToRespVOList(List<AssetsDatasourceDO> AssetsDatasourceDOList);
}
