package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.function.vo.*;
import java.util.List;

public interface IFunctionService {
    Long createFunction(FunctionSaveReqVO createReqVO);
    Integer updateFunction(FunctionSaveReqVO updateReqVO);
    Integer deleteFunction(Long id);
    FunctionRespVO getFunctionById(Long id);
    PageResult<FunctionRespVO> getFunctionPage(FunctionPageReqVO pageReqVO);
    List<FunctionRespVO> getFunctionsByOntologyId(Long ontologyId);
    FunctionExecRespVO submitExecution(FunctionExecReqVO reqVO);
    void approveExecution(FunctionApprovalReqVO reqVO);
    void rejectExecution(FunctionApprovalReqVO reqVO);
    FunctionExecRespVO executeFunction(Long executionId);
    FunctionExecRespVO getExecutionById(Long id);
    PageResult<FunctionExecRespVO> getExecutionPage(FunctionExecPageReqVO pageReqVO);
    List<FunctionExecRespVO> getPendingApprovals(Long ontologyId);
}
