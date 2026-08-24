package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnRespVO;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface PropertyColumnConvert {

    PropertyColumnConvert INSTANCE = Mappers.getMapper(PropertyColumnConvert.class);

    PropertyColumnDO convert(PropertyColumnSaveReqVO bean);

    PropertyColumnRespVO convert(PropertyColumnDO bean);

    List<PropertyColumnRespVO> convertList(List<PropertyColumnDO> list);
}
