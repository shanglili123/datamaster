package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.function.vo.FunctionRespVO;
import com.datamaster.module.ontology.controller.admin.function.vo.FunctionSaveReqVO;
import com.datamaster.module.ontology.controller.admin.function.vo.FunctionExecRespVO;
import com.datamaster.module.ontology.dal.dataobject.FunctionDO;
import com.datamaster.module.ontology.dal.dataobject.FunctionExecutionDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper
public interface FunctionConvert {
    FunctionConvert INSTANCE = Mappers.getMapper(FunctionConvert.class);
    @Mapping(source = "params", target = "paramNames")
    @Mapping(target = "params", ignore = true)
    FunctionDO convert(FunctionSaveReqVO bean);
    @Mapping(source = "paramNames", target = "params")
    FunctionRespVO convert(FunctionDO bean);
    List<FunctionRespVO> convertList(List<FunctionDO> list);
    FunctionExecRespVO convertExec(FunctionExecutionDO bean);
    List<FunctionExecRespVO> convertExecList(List<FunctionExecutionDO> list);
}
