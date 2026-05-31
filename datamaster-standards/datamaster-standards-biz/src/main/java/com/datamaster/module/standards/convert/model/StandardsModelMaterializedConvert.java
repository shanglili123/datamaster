package com.datamaster.module.standards.convert.model;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelMaterializedDO;

import java.util.List;

/**
 * 物化模型记录 Convert
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Mapper
public interface StandardsModelMaterializedConvert {
    StandardsModelMaterializedConvert INSTANCE = Mappers.getMapper(StandardsModelMaterializedConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param StandardsModelMaterializedPageReqVO 请求参数
     * @return StandardsModelMaterializedDO
     */
     StandardsModelMaterializedDO convertToDO(StandardsModelMaterializedPageReqVO StandardsModelMaterializedPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param StandardsModelMaterializedSaveReqVO 保存请求参数
     * @return StandardsModelMaterializedDO
     */
     StandardsModelMaterializedDO convertToDO(StandardsModelMaterializedSaveReqVO StandardsModelMaterializedSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param StandardsModelMaterializedDO 实体对象
     * @return StandardsModelMaterializedRespVO
     */
     StandardsModelMaterializedRespVO convertToRespVO(StandardsModelMaterializedDO StandardsModelMaterializedDO);

    /**
     * DOList 转换为 RespVOList
     * @param StandardsModelMaterializedDOList 实体对象列表
     * @return List<StandardsModelMaterializedRespVO>
     */
     List<StandardsModelMaterializedRespVO> convertToRespVOList(List<StandardsModelMaterializedDO> StandardsModelMaterializedDOList);
}
