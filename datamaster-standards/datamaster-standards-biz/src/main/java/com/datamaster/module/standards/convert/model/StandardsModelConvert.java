package com.datamaster.module.standards.convert.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;

import java.util.List;

/**
 * 逻辑模型 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsModelConvert {
    StandardsModelConvert INSTANCE = Mappers.getMapper(StandardsModelConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsModelPageReqVO 请求参数
     * @return StandardsModelDO
     */
     StandardsModelDO convertToDO(StandardsModelPageReqVO StandardsModelPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsModelSaveReqVO 保存请求参数
     * @return StandardsModelDO
     */
     StandardsModelDO convertToDO(StandardsModelSaveReqVO StandardsModelSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsModelDO 实体对象
     * @return StandardsModelRespVO
     */
     StandardsModelRespVO convertToRespVO(StandardsModelDO StandardsModelDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsModelDOList 实体对象列表
     * @return List<StandardsModelRespVO>
     */
     List<StandardsModelRespVO> convertToRespVOList(List<StandardsModelDO> StandardsModelDOList);
}
