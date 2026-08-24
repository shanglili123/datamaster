package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.function.vo.*;
import com.datamaster.module.ontology.convert.FunctionConvert;
import com.datamaster.module.ontology.dal.dataobject.FunctionDO;
import com.datamaster.module.ontology.dal.dataobject.FunctionExecutionDO;
import com.datamaster.module.ontology.dal.mapper.FunctionExecutionMapper;
import com.datamaster.module.ontology.dal.mapper.FunctionMapper;
import com.datamaster.module.ontology.service.IFunctionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;

@Service
@Validated
public class FunctionServiceImpl implements IFunctionService {

    private static final Logger log = LoggerFactory.getLogger(FunctionServiceImpl.class);
    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";

    @Resource private FunctionMapper functionMapper;
    @Resource private FunctionExecutionMapper executionMapper;
    @Resource private ObjectMapper objectMapper;

    @Override
    public Long createFunction(FunctionSaveReqVO reqVO) {
        FunctionDO func = FunctionConvert.INSTANCE.convert(reqVO);
        functionMapper.insert(func);
        return func.getId();
    }

    @Override
    public Integer updateFunction(FunctionSaveReqVO reqVO) {
        FunctionDO func = FunctionConvert.INSTANCE.convert(reqVO);
        return functionMapper.updateById(func);
    }

    @Override
    public Integer deleteFunction(Long id) {
        return functionMapper.deleteById(id);
    }

    @Override
    public FunctionRespVO getFunctionById(Long id) {
        return FunctionConvert.INSTANCE.convert(functionMapper.selectById(id));
    }

    @Override
    public PageResult<FunctionRespVO> getFunctionPage(FunctionPageReqVO pageReqVO) {
        PageResult<FunctionDO> page = functionMapper.selectPage(pageReqVO);
        return new PageResult<>(FunctionConvert.INSTANCE.convertList(page.getRows()), page.getTotal());
    }

    @Override
    public List<FunctionRespVO> getFunctionsByOntologyId(Long ontologyId) {
        return FunctionConvert.INSTANCE.convertList(functionMapper.selectByOntologyId(ontologyId));
    }

    @Override
    @Transactional
    public FunctionExecRespVO submitExecution(FunctionExecReqVO reqVO) {
        FunctionDO func = functionMapper.selectById(reqVO.getFunctionId());
        if (func == null) throw new RuntimeException("函数不存在: " + reqVO.getFunctionId());
        FunctionExecutionDO exec = FunctionExecutionDO.builder()
                .functionId(func.getId())
                .ontologyId(func.getOntologyId())
                .inputParams(reqVO.getInputParams())
                .status(STATUS_PENDING)
                .build();
        executionMapper.insert(exec);
        return FunctionConvert.INSTANCE.convertExec(exec);
    }

    @Override
    @Transactional
    public void approveExecution(FunctionApprovalReqVO reqVO) {
        FunctionExecutionDO exec = executionMapper.selectById(reqVO.getExecutionId());
        if (exec == null) throw new RuntimeException("执行记录不存在");
        if (!STATUS_PENDING.equals(exec.getStatus())) throw new RuntimeException("当前状态不允许审批: " + exec.getStatus());
        exec.setStatus(STATUS_APPROVED);
        exec.setApprovalReason(reqVO.getApprovalReason());
        exec.setApproveTime(new Date());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public void rejectExecution(FunctionApprovalReqVO reqVO) {
        FunctionExecutionDO exec = executionMapper.selectById(reqVO.getExecutionId());
        if (exec == null) throw new RuntimeException("执行记录不存在");
        if (!STATUS_PENDING.equals(exec.getStatus())) throw new RuntimeException("当前状态不允许拒绝: " + exec.getStatus());
        exec.setStatus(STATUS_REJECTED);
        exec.setApprovalReason(reqVO.getApprovalReason());
        exec.setApproveTime(new Date());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public FunctionExecRespVO executeFunction(Long executionId) {
        FunctionExecutionDO exec = executionMapper.selectById(executionId);
        if (exec == null) throw new RuntimeException("执行记录不存在");
        if (!STATUS_APPROVED.equals(exec.getStatus())) {
            throw new RuntimeException("只有已批准的才能执行，当前状态: " + exec.getStatus());
        }
        FunctionDO func = functionMapper.selectById(exec.getFunctionId());
        long start = System.currentTimeMillis();
        try {
            String result = runFunction(func, exec.getInputParams());
            exec.setOutputResult(result);
            exec.setStatus(STATUS_EXECUTED);
            exec.setExecuteTime(new Date());
        } catch (Exception e) {
            log.error("函数执行失败", e);
            exec.setStatus(STATUS_FAILED);
            exec.setErrorMessage(e.getMessage());
        }
        exec.setDurationMs(System.currentTimeMillis() - start);
        executionMapper.updateById(exec);
        return FunctionConvert.INSTANCE.convertExec(exec);
    }

    @Override
    public FunctionExecRespVO getExecutionById(Long id) {
        return FunctionConvert.INSTANCE.convertExec(executionMapper.selectById(id));
    }

    @Override
    public PageResult<FunctionExecRespVO> getExecutionPage(FunctionExecPageReqVO pageReqVO) {
        PageResult<FunctionExecutionDO> page = executionMapper.selectPage(pageReqVO);
        return new PageResult<>(FunctionConvert.INSTANCE.convertExecList(page.getRows()), page.getTotal());
    }

    @Override
    public List<FunctionExecRespVO> getPendingApprovals(Long ontologyId) {
        return FunctionConvert.INSTANCE.convertExecList(executionMapper.selectPendingApprovals(ontologyId));
    }

    // ================================================================
    //  Execution Engine
    // ================================================================

    private String runFunction(FunctionDO func, String inputParams) throws Exception {
        Map<String, Object> params = parseParams(inputParams);
        switch (func.getLang()) {
            case "TYPESCRIPT": return runTypeScript(func.getBody(), params);
            case "PYTHON": return runPython(func.getBody(), params);
            default: throw new RuntimeException("不支持的语言: " + func.getLang());
        }
    }

    private String runTypeScript(String body, Map<String, Object> params) throws Exception {
        String wrappedScript = "var input = " + toJson(params) + ";\n"
                + "(function() {\n" + body + "\n})();";
        ProcessBuilder pb = new ProcessBuilder("node", "-e", wrappedScript);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = readStream(proc.getInputStream());
        int exit = proc.waitFor();
        if (exit != 0) throw new RuntimeException("TypeScript执行失败 (exit=" + exit + "): " + output);
        return output.trim();
    }

    private String runPython(String body, Map<String, Object> params) throws Exception {
        File tmpFile = File.createTempFile("ont_func_", ".py");
        tmpFile.deleteOnExit();
        String script = "import json\n"
                + "input = json.loads('''" + toJson(params) + "''')\n"
                + body;
        try (FileWriter fw = new FileWriter(tmpFile)) { fw.write(script); }
        ProcessBuilder pb = new ProcessBuilder("python3", tmpFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = readStream(proc.getInputStream());
        int exit = proc.waitFor();
        if (exit != 0) throw new RuntimeException("Python执行失败 (exit=" + exit + "): " + output);
        return output.trim();
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private Map<String, Object> parseParams(String inputParams) {
        if (inputParams == null || inputParams.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(inputParams, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("参数解析失败: " + e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
