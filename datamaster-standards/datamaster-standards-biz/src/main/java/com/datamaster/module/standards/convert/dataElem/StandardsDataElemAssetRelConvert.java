package com.datamaster.module.standards.convert.dataElem;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemAssetRelDO;

import java.util.List;

/**
 * 数据元数据资产关联信息 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsDataElemAssetRelConvert {
    StandardsDataElemAssetRelConvert INSTANCE = Mappers.getMapper(StandardsDataElemAssetRelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsDataElemAssetRelPageReqVO 请求参数
     * @return StandardsDataElemAssetRelDO
     */
     StandardsDataElemAssetRelDO convertToDO(StandardsDataElemAssetRelPageReqVO StandardsDataElemAssetRelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsDataElemAssetRelSaveReqVO 保存请求参数
     * @return StandardsDataElemAssetRelDO
     */
     StandardsDataElemAssetRelDO convertToDO(StandardsDataElemAssetRelSaveReqVO StandardsDataElemAssetRelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsDataElemAssetRelDO 实体对象
     * @return StandardsDataElemAssetRelRespVO
     */
     StandardsDataElemAssetRelRespVO convertToRespVO(StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsDataElemAssetRelDOList 实体对象列表
     * @return List<StandardsDataElemAssetRelRespVO>
     */
     List<StandardsDataElemAssetRelRespVO> convertToRespVOList(List<StandardsDataElemAssetRelDO> StandardsDataElemAssetRelDOList);
}
