package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnRespVO;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RelationColumnConvert {

    RelationColumnConvert INSTANCE = Mappers.getMapper(RelationColumnConvert.class);

    RelationColumnDO convert(RelationColumnSaveReqVO bean);

    RelationColumnRespVO convert(RelationColumnDO bean);

    List<RelationColumnRespVO> convertList(List<RelationColumnDO> list);
}
