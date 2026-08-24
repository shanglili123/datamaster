package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.relation.vo.RelationRespVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 本体关系 Convert
 */
@Mapper
public interface RelationConvert {

    RelationConvert INSTANCE = Mappers.getMapper(RelationConvert.class);

    /**
     * SaveReqVO 转 DO
     *
     * @param bean 保存 Request VO
     * @return DO
     */
    RelationDO convert(RelationSaveReqVO bean);

    /**
     * DO 转 RespVO
     *
     * @param bean DO
     * @return Response VO
     */
    RelationRespVO convert(RelationDO bean);

    /**
     * DO 列表转 RespVO 列表
     *
     * @param list DO 列表
     * @return Response VO 列表
     */
    List<RelationRespVO> convertList(List<RelationDO> list);
}
