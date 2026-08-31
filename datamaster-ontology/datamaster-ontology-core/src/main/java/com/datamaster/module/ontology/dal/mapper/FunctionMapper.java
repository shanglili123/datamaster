package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.function.vo.FunctionPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.FunctionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

public interface FunctionMapper extends BaseMapperX<FunctionDO> {

    default PageResult<FunctionDO> selectPage(FunctionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FunctionDO>()
                .eq(reqVO.getOntologyId() != null, FunctionDO::getOntologyId, reqVO.getOntologyId())
                .eq(reqVO.getLang() != null, FunctionDO::getLang, reqVO.getLang())
                .like(reqVO.getName() != null, FunctionDO::getName, reqVO.getName())
                .orderByDesc(FunctionDO::getId));
    }

    default List<FunctionDO> selectByOntologyId(Long ontologyId) {
        return selectList(new LambdaQueryWrapperX<FunctionDO>()
                .eq(FunctionDO::getOntologyId, ontologyId)
                .orderByAsc(FunctionDO::getId));
    }

    default List<FunctionDO> selectAll() {
        return selectList(new LambdaQueryWrapperX<FunctionDO>().orderByDesc(FunctionDO::getId));
    }
}
