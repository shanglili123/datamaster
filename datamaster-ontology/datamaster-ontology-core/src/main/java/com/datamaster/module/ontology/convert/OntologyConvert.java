package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 本体 Convert
 */
@Mapper
public interface OntologyConvert {

    OntologyConvert INSTANCE = Mappers.getMapper(OntologyConvert.class);

    /**
     * SaveReqVO 转 DO
     *
     * @param bean 保存 Request VO
     * @return DO
     */
    OntologyDO convert(OntologySaveReqVO bean);

    /**
     * DO 转 RespVO
     *
     * @param bean DO
     * @return Response VO
     */
    OntologyRespVO convert(OntologyDO bean);

    /**
     * DO 列表转 RespVO 列表
     *
     * @param list DO 列表
     * @return Response VO 列表
     */
    List<OntologyRespVO> convertList(List<OntologyDO> list);
}
