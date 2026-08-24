package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptRespVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 本体概念 Convert
 */
@Mapper
public interface ConceptConvert {

    ConceptConvert INSTANCE = Mappers.getMapper(ConceptConvert.class);

    /**
     * SaveReqVO 转 DO
     *
     * @param bean 保存 Request VO
     * @return DO
     */
    ConceptDO convert(ConceptSaveReqVO bean);

    /**
     * DO 转 RespVO
     *
     * @param bean DO
     * @return Response VO
     */
    ConceptRespVO convert(ConceptDO bean);

    /**
     * DO 列表转 RespVO 列表
     *
     * @param list DO 列表
     * @return Response VO 列表
     */
    List<ConceptRespVO> convertList(List<ConceptDO> list);
}
