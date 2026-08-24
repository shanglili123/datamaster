package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableRespVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RelationTableConvert {

    RelationTableConvert INSTANCE = Mappers.getMapper(RelationTableConvert.class);

    RelationTableDO convert(RelationTableSaveReqVO bean);

    RelationTableRespVO convert(RelationTableDO bean);

    List<RelationTableRespVO> convertList(List<RelationTableDO> list);
}
