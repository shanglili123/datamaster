package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ConceptTableConvert {

    ConceptTableConvert INSTANCE = Mappers.getMapper(ConceptTableConvert.class);

    ConceptTableDO convert(ConceptTableSaveReqVO bean);

    ConceptTableRespVO convert(ConceptTableDO bean);

    List<ConceptTableRespVO> convertList(List<ConceptTableDO> list);
}
