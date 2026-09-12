package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.function.vo.FunctionExecPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.FunctionExecutionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;
import java.util.Collection;

public interface FunctionExecutionMapper extends BaseMapperX<FunctionExecutionDO> {

    default PageResult<FunctionExecutionDO> selectPage(FunctionExecPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FunctionExecutionDO>()
                .eq(reqVO.getFunctionId() != null, FunctionExecutionDO::getFunctionId, reqVO.getFunctionId())
                .eq(reqVO.getStatus() != null, FunctionExecutionDO::getStatus, reqVO.getStatus())
                .orderByDesc(FunctionExecutionDO::getId));
    }

    default List<FunctionExecutionDO> selectPendingApprovals(Long ontologyId) {
        return selectList(new LambdaQueryWrapperX<FunctionExecutionDO>()
                // ONT_FUNCTION_EXECUTION 为兼容旧库结构没有 ontology_id，按函数定义反查所属本体。
                .inSql(FunctionExecutionDO::getFunctionId,
                        "SELECT id FROM ONT_FUNCTION WHERE ontology_id = " + ontologyId)
                .eq(FunctionExecutionDO::getStatus, "PENDING_APPROVAL")
                .orderByDesc(FunctionExecutionDO::getId));
    }

    default int deleteByFunctionIds(Collection<Long> functionIds) {
        if (functionIds == null || functionIds.isEmpty()) return 0;
        return delete(new LambdaQueryWrapperX<FunctionExecutionDO>()
                .in(FunctionExecutionDO::getFunctionId, functionIds));
    }
}
