

package com.datamaster.api.ds.service.etl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DsLogResultDTO;
import com.datamaster.api.ds.api.etl.DsStartTaskReqDTO;
import com.datamaster.api.ds.api.etl.DsTaskSaveReqDTO;
import com.datamaster.api.ds.api.etl.DsTaskSaveRespDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessDefinition;
import com.datamaster.api.ds.api.etl.ds.ProcessTaskRelation;
import com.datamaster.api.ds.api.etl.ds.TaskDefinition;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.httpClient.DsRequestUtils;
import com.datamaster.common.httpClient.constants.DataMasterDSApiType;
import com.datamaster.common.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <P>
 * 用途:ds数据集成任务相关接口实现
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-20 09:51
 **/
@Slf4j
@Service
public class DsEtlTaskServiceImpl implements IDsEtlTaskService {
    @Override
    public DsTaskSaveRespDTO createTask(DsTaskSaveReqDTO dsTaskSaveReqDTO, Long projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.CREATE_PROCESS_DEFINITION;
        return requestWorkflowDefinition(apiType, String.valueOf(projectCode), null, buildWorkflowDefinitionParams(dsTaskSaveReqDTO));
    }

    @Override
    public DsTaskSaveRespDTO updateTask(DsTaskSaveReqDTO dsTaskSaveReqDTO, String projectCode, String taskCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.UPDATE_PROCESS_DEFINITION;
        return requestWorkflowDefinition(apiType, String.valueOf(projectCode), taskCode, buildWorkflowDefinitionParams(dsTaskSaveReqDTO));
    }

    @Override
    public ProcessDefinition getTaskByName(String projectCode, String name) {
        if (StringUtils.isBlank(projectCode) || StringUtils.isBlank(name)) {
            return null;
        }
        DataMasterDSApiType apiType = DataMasterDSApiType.GET_PROCESS_DEFINITION_LIST;
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", 1);
        params.put("pageSize", 100);
        params.put("searchVal", name);
        JSONObject response = DsRequestUtils.request(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(),
                null, params,
                JSONObject.class);
        if (response == null || !Boolean.TRUE.equals(response.getBoolean("success"))) {
            return null;
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return null;
        }
        List<ProcessDefinition> definitions = data.getList("totalList", ProcessDefinition.class);
        if (definitions == null) {
            return null;
        }
        return definitions.stream()
                .filter(definition -> StringUtils.equals(name, definition.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public DsStatusRespDTO releaseTask(String releaseState, String projectCode, String code) {
        DataMasterDSApiType apiType = DataMasterDSApiType.RELEASE_PROCESS_DEFINITION;
        Map<String, Object> params = new HashMap<>();
        params.put("releaseState", releaseState);
        return DsRequestUtils.request(DsRequestUtils.replaceProjectCodeAndCode(apiType.getUrl(), projectCode, code),
                apiType.getMethod(),
                null, params,
                DsStatusRespDTO.class);
    }

    @Override
    public DsStatusRespDTO deleteTask(String projectCode, String code) {
        DataMasterDSApiType apiType = DataMasterDSApiType.DELETE_PROCESS_DEFINITION;
        return DsRequestUtils.request(DsRequestUtils.replaceProjectCodeAndCode(apiType.getUrl(), projectCode, code),
                apiType.getMethod(),
                null, null,
                DsStatusRespDTO.class);
    }

    @Override
    public DsStatusRespDTO startTask(DsStartTaskReqDTO dsStartTaskReqDTO, String projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.POST_START_PROCESS;
        JSONObject params = JSONObject.parseObject(JSONObject.toJSONString(dsStartTaskReqDTO));
        params.put("workflowDefinitionCode", dsStartTaskReqDTO.getProcessDefinitionCode());
        params.put("workflowInstancePriority", dsStartTaskReqDTO.getProcessInstancePriority());
        params.remove("processDefinitionCode");
        params.remove("processInstancePriority");
        return DsRequestUtils.requestForm(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(), params,
                DsStatusRespDTO.class);
    }

    @Override
    public DsTaskSaveRespDTO batchCopy(String code, String projectCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.BATCH_COPY_PROCESS_DEFINITION;

        // URL 拼接
        String url = DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode);

        // 表单参数
        Map<String, Object> params = new HashMap<>();
        params.put("codes", code);
        params.put("targetProjectCode", projectCode);

        // 调用
        return DsRequestUtils.requestForm(url, apiType.getMethod(), params, DsTaskSaveRespDTO.class);
    }

    @Override
    public List<TaskInstance> listTaskInstances(String projectCode, Long processInstanceId) {
        if (StringUtils.isBlank(projectCode) || processInstanceId == null) {
            return java.util.Collections.emptyList();
        }
        DataMasterDSApiType apiType = DataMasterDSApiType.GET_TASK_INSTANCE_LIST;
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", 1);
        params.put("pageSize", 100);
        params.put("workflowInstanceId", processInstanceId);
        params.put("processInstanceId", processInstanceId);
        JSONObject response = DsRequestUtils.request(DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode),
                apiType.getMethod(), null, params, JSONObject.class);
        if (response == null || !(Boolean.TRUE.equals(response.getBoolean("success")) || Integer.valueOf(0).equals(response.getInteger("code")))) {
            return java.util.Collections.emptyList();
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            return java.util.Collections.emptyList();
        }
        List<TaskInstance> list = data.getList("totalList", TaskInstance.class);
        return list == null ? java.util.Collections.emptyList() : list;
    }

    @Override
    public String getTaskInstanceLog(Long taskInstanceId) {
        DsLogResultDTO result = getTaskInstanceLog(taskInstanceId, 0, 100000);
        return result == null || result.getLogContent() == null ? "" : result.getLogContent();
    }

    @Override
    public DsLogResultDTO getTaskInstanceLog(Long taskInstanceId, Integer skipLineNum, Integer limit) {
        if (taskInstanceId == null) {
            return DsLogResultDTO.builder()
                    .fromLineNum(skipLineNum == null ? 0 : skipLineNum)
                    .toLineNum(skipLineNum == null ? 0 : skipLineNum)
                    .logContent("")
                    .end(true)
                    .build();
        }
        int fromLineNum = Math.max(skipLineNum == null ? 0 : skipLineNum, 0);
        int safeLimit = limit == null || limit <= 0 ? 2000 : Math.min(limit, 10000);
        DataMasterDSApiType apiType = DataMasterDSApiType.GET_TASK_INSTANCE_LOG;
        Map<String, Object> params = new HashMap<>();
        params.put("taskInstanceId", taskInstanceId);
        params.put("skipLineNum", fromLineNum);
        params.put("limit", safeLimit);
        JSONObject response = DsRequestUtils.request(apiType.getUrl(), apiType.getMethod(), null, params, JSONObject.class);
        if (response == null || !(Boolean.TRUE.equals(response.getBoolean("success")) || Integer.valueOf(0).equals(response.getInteger("code")))) {
            return DsLogResultDTO.builder()
                    .fromLineNum(fromLineNum)
                    .toLineNum(fromLineNum)
                    .logContent("")
                    .end(true)
                    .build();
        }
        return extractLogResult(response.get("data"), fromLineNum, safeLimit);
    }

    @Override
    public String downloadTaskInstanceLog(Long taskInstanceId) {
        if (taskInstanceId == null) {
            return "";
        }
        DataMasterDSApiType apiType = DataMasterDSApiType.DOWNLOAD_TASK_INSTANCE_LOG;
        Map<String, Object> params = new HashMap<>();
        params.put("taskInstanceId", taskInstanceId);
        String body = DsRequestUtils.requestRaw(apiType.getUrl(), apiType.getMethod(), params);
        if (StringUtils.isBlank(body)) {
            return "";
        }
        String trimBody = StringUtils.trim(body);
        if (StringUtils.startsWith(trimBody, "{")) {
            return extractLogContent(JSON.parseObject(trimBody).get("data"));
        }
        return body;
    }

    private String extractLogContent(Object data) {
        DsLogResultDTO result = extractLogResult(data, 0, 100000);
        return result == null || result.getLogContent() == null ? "" : result.getLogContent();
    }

    private DsLogResultDTO extractLogResult(Object data, int fromLineNum, int limit) {
        if (data == null) {
            return DsLogResultDTO.builder()
                    .fromLineNum(fromLineNum)
                    .toLineNum(fromLineNum)
                    .logContent("")
                    .end(true)
                    .build();
        }
        String content;
        Integer lineNum = null;
        Boolean end = null;
        if (data instanceof JSONObject) {
            JSONObject object = (JSONObject) data;
            lineNum = object.getInteger("lineNum");
            if (lineNum == null) {
                lineNum = object.getInteger("toLineNum");
            }
            end = object.getBoolean("end");
            if (end == null) {
                end = object.getBoolean("isEnd");
            }
            content = firstNotBlank(object.getString("message"), object.getString("log"), object.getString("content"));
        } else if (data instanceof JSONArray) {
            content = ((JSONArray) data).toJSONString();
        } else {
            content = String.valueOf(data);
        }
        if (content == null) {
            content = "";
        }
        int lineCount = countLines(content);
        int toLineNum = lineNum == null ? fromLineNum + lineCount : Math.max(lineNum, fromLineNum);
        boolean isEnd = end == null ? lineCount < limit : end;
        return DsLogResultDTO.builder()
                .fromLineNum(fromLineNum)
                .toLineNum(toLineNum)
                .logContent(content)
                .end(isEnd)
                .build();
    }

    private String firstNotBlank(String first, String second, String third) {
        if (StringUtils.isNotBlank(first)) {
            return first;
        }
        if (StringUtils.isNotBlank(second)) {
            return second;
        }
        return third;
    }

    private int countLines(String content) {
        if (StringUtils.isBlank(content)) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private JSONObject buildWorkflowDefinitionParams(DsTaskSaveReqDTO dsTaskSaveReqDTO) {
        JSONObject params = JSONObject.parseObject(JSON.toJSONString(dsTaskSaveReqDTO));
        params.put("taskRelationJson", normalizeWorkflowJson(dsTaskSaveReqDTO.getTaskRelationJson()));
        params.put("taskDefinitionJson", normalizeWorkflowJson(dsTaskSaveReqDTO.getTaskDefinitionJson()));
        renameWorkflowKeys(params);
        params.entrySet().removeIf(entry -> entry.getValue() == null
                || (entry.getValue() instanceof String && StringUtils.isBlank((String) entry.getValue())));
        return params;
    }

    private DsTaskSaveRespDTO requestWorkflowDefinition(DataMasterDSApiType apiType, String projectCode, String taskCode, JSONObject params) {
        String url = StringUtils.isBlank(taskCode)
                ? DsRequestUtils.replaceProjectCode(apiType.getUrl(), projectCode)
                : DsRequestUtils.replaceProjectCodeAndCode(apiType.getUrl(), projectCode, taskCode);
        JSONObject response = DsRequestUtils.requestForm(url, apiType.getMethod(), params, JSONObject.class);
        DsTaskSaveRespDTO result = response == null ? new DsTaskSaveRespDTO() : response.toJavaObject(DsTaskSaveRespDTO.class);
        if (response == null || !result.isOk()) {
            return result;
        }

        ProcessDefinition definition = parseWorkflowDefinition(response.getJSONObject("data"));
        String definitionCode = definition == null ? taskCode : definition.getCode();
        if (isIncompleteDefinition(definition) && StringUtils.isNotBlank(definitionCode)) {
            ProcessDefinition detail = getTaskDetail(projectCode, definitionCode);
            if (detail != null) {
                definition = detail;
            }
        }
        result.setData(definition);
        return result;
    }

    private ProcessDefinition getTaskDetail(String projectCode, String taskCode) {
        DataMasterDSApiType apiType = DataMasterDSApiType.GET_PROCESS_DEFINITION;
        JSONObject response = DsRequestUtils.request(
                DsRequestUtils.replaceProjectCodeAndCode(apiType.getUrl(), projectCode, taskCode),
                apiType.getMethod(), null, null, JSONObject.class);
        if (response == null || !(Boolean.TRUE.equals(response.getBoolean("success")) || Integer.valueOf(0).equals(response.getInteger("code")))) {
            return null;
        }
        return parseWorkflowDefinition(response.getJSONObject("data"));
    }

    private boolean isIncompleteDefinition(ProcessDefinition definition) {
        return definition == null
                || definition.getTaskDefinitionList() == null
                || definition.getTaskRelationList() == null;
    }

    private ProcessDefinition parseWorkflowDefinition(JSONObject data) {
        if (data == null) {
            return null;
        }
        JSONObject workflowDefinition = data.getJSONObject("workflowDefinition");
        ProcessDefinition definition = (workflowDefinition == null ? data : workflowDefinition).toJavaObject(ProcessDefinition.class);

        List<TaskDefinition> taskDefinitionList = data.getList("taskDefinitionList", TaskDefinition.class);
        if (taskDefinitionList != null) {
            definition.setTaskDefinitionList(taskDefinitionList);
        }

        List<ProcessTaskRelation> taskRelationList = data.getList("workflowTaskRelationList", ProcessTaskRelation.class);
        if (taskRelationList == null) {
            taskRelationList = data.getList("taskRelationList", ProcessTaskRelation.class);
        }
        if (taskRelationList != null) {
            definition.setTaskRelationList(taskRelationList);
        }
        return definition;
    }

    private String normalizeWorkflowJson(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        Object value = JSON.parse(json);
        renameWorkflowKeys(value);
        return JSON.toJSONString(value);
    }

    private void renameWorkflowKeys(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            renameKey(object, "processDefinitionCode", "workflowDefinitionCode");
            renameKey(object, "processDefinitionVersion", "workflowDefinitionVersion");
            renameKey(object, "processInstancePriority", "workflowInstancePriority");
            for (Object child : object.values()) {
                renameWorkflowKeys(child);
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (Object child : array) {
                renameWorkflowKeys(child);
            }
        }
    }

    private void renameKey(JSONObject object, String oldKey, String newKey) {
        if (object.containsKey(oldKey)) {
            object.put(newKey, object.get(oldKey));
            object.remove(oldKey);
        }
    }
}
