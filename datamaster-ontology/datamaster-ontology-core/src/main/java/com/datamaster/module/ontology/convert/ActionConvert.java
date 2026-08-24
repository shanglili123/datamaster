package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.action.vo.ActionRespVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ActionSaveReqVO;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionRespVO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ActionConvert {

    ActionConvert INSTANCE = Mappers.getMapper(ActionConvert.class);

    ActionDO convert(ActionSaveReqVO bean);

    ActionRespVO convert(ActionDO bean);

    List<ActionRespVO> convertList(List<ActionDO> list);

    ExecutionRespVO convert(ActionExecutionDO bean);

    List<ExecutionRespVO> convertExecutionList(List<ActionExecutionDO> list);
}
