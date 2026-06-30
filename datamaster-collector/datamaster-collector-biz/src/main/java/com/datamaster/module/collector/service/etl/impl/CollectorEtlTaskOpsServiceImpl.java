package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DSExecuteDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.enums.ExecuteType;
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceLogStatusRespDTO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsEventDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsPolicyDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlSchedulerService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsEventService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsPolicyService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CollectorEtlTaskOpsServiceImpl implements ICollectorEtlTaskOpsService {

    @Resource
    private ICollectorEtlTaskOpsPolicyService policyService;
    @Resource
    private ICollectorEtlTaskOpsEventService eventService;
    @Resource
    private ICollectorEtlTaskInstanceService taskInstanceService;
    @Resource
    private ICollectorEtlTaskExtService taskExtService;
    @Resource
    private ICollectorEtlSchedulerService schedulerService;
    @Resource
    private CollectorEtlTaskMapper taskMapper;
    @Resource
    private IDsEtlExecutorService dsEtlExecutorService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;
    @Resource
    private IDsEtlSchedulerService dsEtlSchedulerService;
    @Value("${ai.skill-model.enabled:false}")
    private Boolean aiSkillModelEnabled;
    @Value("${ai.skill-model.api-url:}")
    private String aiSkillModelApiUrl;
    @Value("${ai.skill-model.api-key:}")
    private String aiSkillModelApiKey;
    @Value("${ai.skill-model.model:qwen-plus}")
    private String aiSkillModelName;
    @Value("${ai.skill-model.temperature:0.2}")
    private Double aiSkillModelTemperature;
    @Value("${ai.skill-model.max-tokens:1024}")
    private Integer aiSkillModelMaxTokens;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleProcessInstanceFinished(ProcessInstance processInstance, CollectorEtlTaskInstanceDO currentInstance) {
        if (processInstance == null || processInstance.getState() == null || !processInstance.getState().isFailure()) {
            return;
        }
        CollectorEtlTaskInstanceDO instance = currentInstance;
        if (instance == null || instance.getTaskId() == null) {
            instance = taskInstanceService.getByDsId(processInstance.getId());
        }
        if (instance == null || instance.getTaskId() == null) {
            return;
        }
        CollectorEtlTaskDO task = taskMapper.selectById(instance.getTaskId());
        if (task == null) {
            return;
        }
        CollectorEtlTaskOpsPolicyDO policy = policyService.getByTaskId(task.getId());
        if (policy == null || (!Boolean.TRUE.equals(policy.getFailStopEnabled()) && !Boolean.TRUE.equals(policy.getAiManaged()))) {
            return;
        }
        if (eventService.count(Wrappers.lambdaQuery(CollectorEtlTaskOpsEventDO.class)
                .eq(CollectorEtlTaskOpsEventDO::getTaskInstanceId, instance.getId())
                .eq(CollectorEtlTaskOpsEventDO::getEventType, "TASK_FAILED")) > 0) {
            return;
        }

        CollectorEtlTaskOpsEventDO event = buildBaseEvent(task, instance, processInstance);
        String logExcerpt = loadLogExcerpt(instance.getId());
        event.setLogExcerpt(logExcerpt);

        if (Boolean.TRUE.equals(policy.getFailStopEnabled())) {
            unloadTaskAndSchedule(task, "失败即停策略触发");
            event.setAction("UNLOAD_TASK");
            event.setActionStatus("SUCCESS");
            event.setReason("任务失败，已按规则下线任务和周期调度。");
            event.setSuggestion("请确认失败原因后再重新发布或恢复调度。");
            event.setFailureType("RULE_FAIL_STOP");
            event.setRiskLevel("HIGH");
            event.setRecoverable(Boolean.FALSE);
            event.setValidFlag(Boolean.TRUE);
            event.setDelFlag(Boolean.FALSE);
            eventService.save(event);
            return;
        }

        if (Boolean.TRUE.equals(policy.getAiManaged())) {
            OpsAnalysis analysis = analyzeFailure(task, instance, logExcerpt);
            applyAnalysis(event, analysis);
            executeAiManagedAction(policy, task, instance, analysis, event);
        }

        event.setValidFlag(Boolean.TRUE);
        event.setDelFlag(Boolean.FALSE);
        eventService.save(event);
    }

    private CollectorEtlTaskOpsEventDO buildBaseEvent(CollectorEtlTaskDO task, CollectorEtlTaskInstanceDO instance,
                                                      ProcessInstance processInstance) {
        CollectorEtlTaskOpsEventDO event = new CollectorEtlTaskOpsEventDO();
        event.setTaskId(task.getId());
        event.setTaskCode(task.getCode());
        event.setTaskType(task.getType());
        event.setTaskInstanceId(instance.getId());
        event.setDsProcessInstanceId(processInstance.getId());
        event.setInstanceStatus(String.valueOf(processInstance.getState().getCode()));
        event.setEventType("TASK_FAILED");
        return event;
    }

    private void executeAiManagedAction(CollectorEtlTaskOpsPolicyDO policy, CollectorEtlTaskDO task,
                                        CollectorEtlTaskInstanceDO instance, OpsAnalysis analysis,
        CollectorEtlTaskOpsEventDO event) {
        if (!analysis.isRecoverable()) {
            event.setAction("WAIT_MANUAL_RECOVER");
            event.setActionStatus("SKIPPED");
            event.setReason(append(event.getReason(), "AI判断不可自动恢复，等待人工处理。"));
            return;
        }
        if (!Boolean.TRUE.equals(policy.getAutoRecoverEnabled())) {
            event.setAction("SUGGEST_RECOVER");
            event.setActionStatus("SKIPPED");
            event.setReason(append(event.getReason(), "策略未开启自动恢复。"));
            return;
        }
        if (StringUtils.equalsIgnoreCase("SUGGEST_ONLY", policy.getRecoverStrategy())) {
            event.setAction("SUGGEST_RECOVER");
            event.setActionStatus("SKIPPED");
            event.setReason(append(event.getReason(), "恢复策略为只给建议。"));
            return;
        }
        int maxRecoverTimes = policy.getMaxRecoverTimes() == null ? 1 : policy.getMaxRecoverTimes();
        long recoveredTimes = eventService.count(Wrappers.lambdaQuery(CollectorEtlTaskOpsEventDO.class)
                .eq(CollectorEtlTaskOpsEventDO::getTaskId, task.getId())
                .eq(CollectorEtlTaskOpsEventDO::getAction, "RECOVER_FROM_FAILURE")
                .eq(CollectorEtlTaskOpsEventDO::getActionStatus, "SUCCESS"));
        if (recoveredTimes >= maxRecoverTimes) {
            unloadTaskAndSchedule(task, "自动恢复次数已达上限");
            event.setAction("UNLOAD_TASK");
            event.setActionStatus("SUCCESS");
            event.setReason(append(event.getReason(), "自动恢复次数已达上限，已下线任务。"));
            return;
        }
        try {
            DsStatusRespDTO response = dsEtlExecutorService.execute(DSExecuteDTO.builder()
                    .processInstanceId(instance.getDsId() == null ? instance.getId() : instance.getDsId())
                    .executeType(ExecuteType.START_FAILURE_TASK_PROCESS)
                    .build(), task.getProjectCode());
            if (response != null && Boolean.TRUE.equals(response.getSuccess())) {
                event.setAction("RECOVER_FROM_FAILURE");
                event.setActionStatus("SUCCESS");
                event.setReason(append(event.getReason(), "已调用DS从失败节点恢复执行。"));
            } else {
                event.setAction("RECOVER_FROM_FAILURE");
                event.setActionStatus("FAILED");
                event.setReason(append(event.getReason(), response == null ? "DS恢复无响应。" : response.getMsg()));
            }
        } catch (Exception e) {
            event.setAction("RECOVER_FROM_FAILURE");
            event.setActionStatus("FAILED");
            event.setReason(append(event.getReason(), "DS恢复异常：" + e.getMessage()));
            log.warn("AI托管自动恢复异常，taskId={}，instanceId={}", task.getId(), instance.getId(), e);
        }
    }

    private OpsAnalysis analyzeFailure(CollectorEtlTaskDO task, CollectorEtlTaskInstanceDO instance, String logExcerpt) {
        OpsAnalysis semanticAnalysis = analyzeFailureByModel(task, instance, logExcerpt);
        if (semanticAnalysis != null) {
            return semanticAnalysis;
        }

        OpsAnalysis analysis = new OpsAnalysis();
        analysis.setRiskLevel("MEDIUM");
        analysis.setFailureType("UNKNOWN");
        analysis.setRecoverable(false);
        analysis.setReason("未匹配到明确失败原因。");
        analysis.setSuggestion("请查看完整日志，确认是否为代码、规则、数据源或运行环境问题。");

        String text = StringUtils.defaultString(logExcerpt).toLowerCase();
        if (containsAny(text, "connection refused", "connect timed out", "read timed out", "connection reset",
                "communications link failure", "unknownhost", "sockettimeoutexception", "network")) {
            analysis.setFailureType("NETWORK_OR_DATASOURCE");
            analysis.setRecoverable(isIncrementalTask(task));
            analysis.setReason("日志中出现网络或数据源连接异常。");
            analysis.setSuggestion(isIncrementalTask(task)
                    ? "该任务为增量集成任务，可从失败节点恢复执行，由增量准备节点重新计算源端和目标端边界。"
                    : "建议确认服务器网络、数据源连通性和账号权限后再手工重跑。");
        } else if (containsAny(text, "syntax error", "sqlsyntax", "column", "table", "字段", "表不存在",
                "parseexception", "analysisexception", "规则", "expression")) {
            analysis.setFailureType("CODE_OR_RULE");
            analysis.setRiskLevel("HIGH");
            analysis.setRecoverable(false);
            analysis.setReason("日志中出现SQL、字段、表或规则表达式异常。");
            analysis.setSuggestion("请修正SQL/字段映射/处理规则后重新发布任务；系统已避免自动重跑。");
        } else if (containsAny(text, "permission denied", "access denied", "authentication", "not authorized")) {
            analysis.setFailureType("PERMISSION");
            analysis.setRiskLevel("HIGH");
            analysis.setRecoverable(false);
            analysis.setReason("日志中出现权限或认证异常。");
            analysis.setSuggestion("请检查源端/目标端账号、库表权限和连接配置后重新发布。");
        } else if (containsAny(text, "outofmemory", "java heap space", "no space left", "disk")) {
            analysis.setFailureType("RESOURCE");
            analysis.setRiskLevel("HIGH");
            analysis.setRecoverable(false);
            analysis.setReason("日志中出现内存或磁盘资源异常。");
            analysis.setSuggestion("请扩容资源或降低并发/批量大小后再恢复任务。");
        }
        analysis.setAiRawResult("{\"engine\":\"rule-fallback\",\"failureType\":\"" + analysis.getFailureType()
                + "\",\"recoverable\":" + analysis.isRecoverable() + "}");
        return analysis;
    }

    private OpsAnalysis analyzeFailureByModel(CollectorEtlTaskDO task, CollectorEtlTaskInstanceDO instance, String logExcerpt) {
        if (!Boolean.TRUE.equals(aiSkillModelEnabled)
                || StringUtils.isBlank(aiSkillModelApiUrl)
                || StringUtils.isBlank(aiSkillModelApiKey)
                || StringUtils.isBlank(logExcerpt)) {
            return null;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("model", aiSkillModelName);
            request.put("temperature", aiSkillModelTemperature == null ? 0.2D : aiSkillModelTemperature);
            request.put("max_tokens", aiSkillModelMaxTokens == null ? 1024 : aiSkillModelMaxTokens);

            JSONArray messages = new JSONArray();
            messages.add(message("system", "你是数据集成任务运维专家。请基于任务上下文和失败日志做语义分析，只返回 JSON，不要返回 Markdown。"));
            messages.add(message("user", buildFailureAnalysisPrompt(task, instance, logExcerpt)));
            request.put("messages", messages);

            HttpUtils.ResponseObject response = HttpUtils.sendPost(resolveChatCompletionsUrl(), request, buildModelHeaders());
            if (response == null || response.getStatus() < 200 || response.getStatus() >= 300) {
                log.warn("AI任务运维语义分析调用失败，status={}", response == null ? null : response.getStatus());
                return null;
            }
            JSONObject responseBody = response.getBody() instanceof JSONObject
                    ? (JSONObject) response.getBody()
                    : JSONObject.parseObject(String.valueOf(response.getBody()));
            JSONArray choices = responseBody.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getString("content");
            return parseModelAnalysis(content);
        } catch (Exception e) {
            log.warn("AI任务运维语义分析异常，taskId={}，instanceId={}", task.getId(),
                    instance == null ? null : instance.getId(), e);
            return null;
        }
    }

    private JSONObject message(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildFailureAnalysisPrompt(CollectorEtlTaskDO task, CollectorEtlTaskInstanceDO instance, String logExcerpt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析 DataMaster 数据任务失败原因，并判断是否适合自动恢复。\n");
        prompt.append("输出 JSON 字段：failureType、riskLevel、recoverable、reason、suggestion。\n");
        prompt.append("failureType 只能是 NETWORK_OR_DATASOURCE、CODE_OR_RULE、PERMISSION、RESOURCE、DATA_QUALITY、SCHEDULER_OR_ENV、UNKNOWN 之一。\n");
        prompt.append("riskLevel 只能是 LOW、MEDIUM、HIGH 之一。\n");
        prompt.append("recoverable 为布尔值。只有临时网络、数据源短暂不可用、调度环境短暂异常，且重跑不会造成重复写入风险时才返回 true；SQL、字段、权限、资源容量、数据质量、配置错误一般返回 false。\n");
        prompt.append("reason 用中文简短说明根因，suggestion 用中文给人工处理建议。\n\n");
        prompt.append("任务信息：\n");
        prompt.append("- taskId: ").append(task.getId()).append("\n");
        prompt.append("- taskName: ").append(StringUtils.defaultString(task.getName())).append("\n");
        prompt.append("- taskCode: ").append(StringUtils.defaultString(task.getCode())).append("\n");
        prompt.append("- taskType: ").append(StringUtils.defaultString(task.getType())).append("\n");
        prompt.append("- projectCode: ").append(StringUtils.defaultString(task.getProjectCode())).append("\n");
        prompt.append("- incrementalTask: ").append(isIncrementalTask(task)).append("\n");
        if (instance != null) {
            prompt.append("- instanceId: ").append(instance.getId()).append("\n");
            prompt.append("- dsProcessInstanceId: ").append(instance.getDsId()).append("\n");
            prompt.append("- instanceStatus: ").append(StringUtils.defaultString(instance.getStatus())).append("\n");
            prompt.append("- runTimes: ").append(instance.getRunTimes()).append("\n");
        }
        prompt.append("\n失败日志片段：\n");
        prompt.append(trim(StringUtils.defaultString(logExcerpt), 12000));
        return prompt.toString();
    }

    private List<HeaderEntity> buildModelHeaders() {
        List<HeaderEntity> headers = new ArrayList<HeaderEntity>();
        HeaderEntity contentType = new HeaderEntity();
        contentType.setKey("Content-Type");
        contentType.setValue("application/json");
        headers.add(contentType);
        HeaderEntity authorization = new HeaderEntity();
        authorization.setKey("Authorization");
        authorization.setValue("Bearer " + aiSkillModelApiKey);
        headers.add(authorization);
        return headers;
    }

    private String resolveChatCompletionsUrl() {
        String baseUrl = aiSkillModelApiUrl.trim();
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private OpsAnalysis parseModelAnalysis(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        JSONObject result = JSONObject.parseObject(content.substring(start, end + 1));
        String failureType = normalizeFailureType(result.getString("failureType"));
        String riskLevel = normalizeRiskLevel(result.getString("riskLevel"));
        if (failureType == null || riskLevel == null) {
            return null;
        }
        OpsAnalysis analysis = new OpsAnalysis();
        analysis.setFailureType(failureType);
        analysis.setRiskLevel(riskLevel);
        analysis.setRecoverable(Boolean.TRUE.equals(result.getBoolean("recoverable")));
        analysis.setReason(StringUtils.defaultIfBlank(result.getString("reason"), "模型未给出明确原因。"));
        analysis.setSuggestion(StringUtils.defaultIfBlank(result.getString("suggestion"), "请查看完整日志并人工确认处理方案。"));
        analysis.setAiRawResult("{\"engine\":\"semantic-model\",\"result\":" + result.toJSONString() + "}");
        return analysis;
    }

    private String normalizeFailureType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("NETWORK_OR_DATASOURCE".equals(normalized) || "CODE_OR_RULE".equals(normalized)
                || "PERMISSION".equals(normalized) || "RESOURCE".equals(normalized)
                || "DATA_QUALITY".equals(normalized) || "SCHEDULER_OR_ENV".equals(normalized)
                || "UNKNOWN".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeRiskLevel(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private void applyAnalysis(CollectorEtlTaskOpsEventDO event, OpsAnalysis analysis) {
        event.setFailureType(analysis.getFailureType());
        event.setRiskLevel(analysis.getRiskLevel());
        event.setRecoverable(analysis.isRecoverable());
        event.setReason(append(event.getReason(), analysis.getReason()));
        event.setSuggestion(analysis.getSuggestion());
        event.setAiRawResult(analysis.getAiRawResult());
    }

    private boolean isIncrementalTask(CollectorEtlTaskDO task) {
        CollectorEtlTaskExtDO ext = taskExtService.getByTaskId(task.getId());
        return ext != null && StringUtils.isNotBlank(ext.getIncrementalType());
    }

    private void unloadTaskAndSchedule(CollectorEtlTaskDO task, String reason) {
        String dsTaskCode = resolveDsTaskCode(task);
        try {
            if (StringUtils.isNotBlank(dsTaskCode)) {
                dsEtlTaskService.releaseTask("OFFLINE", task.getProjectCode(), dsTaskCode);
            }
        } catch (Exception e) {
            log.warn("任务运维下线DS任务异常，taskId={}，reason={}", task.getId(), reason, e);
        }
        CollectorEtlSchedulerDO scheduler = getScheduler(task);
        if (scheduler != null && scheduler.getDsId() != null && scheduler.getDsId() > 0) {
            try {
                dsEtlSchedulerService.offlineScheduler(task.getProjectCode(), scheduler.getDsId());
            } catch (Exception e) {
                log.warn("任务运维下线DS调度异常，taskId={}，schedulerId={}", task.getId(), scheduler.getDsId(), e);
            }
            scheduler.setStatus("0");
            schedulerService.updateById(scheduler);
        }
        task.setStatus("0");
        taskMapper.updateById(task);
    }

    private CollectorEtlSchedulerDO getScheduler(CollectorEtlTaskDO task) {
        CollectorEtlSchedulerPageReqVO reqVO = new CollectorEtlSchedulerPageReqVO();
        reqVO.setTaskId(task.getId());
        reqVO.setTaskCode(task.getCode());
        CollectorEtlSchedulerDO scheduler = schedulerService.getCollectorEtlSchedulerById(reqVO);
        if (scheduler == null || scheduler.getId() == null) {
            reqVO.setTaskCode(null);
            scheduler = schedulerService.getCollectorEtlSchedulerById(reqVO);
        }
        return scheduler;
    }

    private String resolveDsTaskCode(CollectorEtlTaskDO task) {
        CollectorEtlTaskExtDO ext = taskExtService.getByTaskId(task.getId());
        if (ext != null && StringUtils.isNotBlank(ext.getEtlTaskCode())) {
            return ext.getEtlTaskCode();
        }
        return task.getCode();
    }

    private String loadLogExcerpt(Long taskInstanceId) {
        try {
            CollectorEtlTaskInstanceLogStatusRespDTO log = taskInstanceService.getLogByTaskInstanceId(taskInstanceId);
            return extractFailureExcerpt(log == null ? null : log.getLog());
        } catch (Exception e) {
            return "日志读取失败：" + e.getMessage();
        }
    }

    private String extractFailureExcerpt(String logContent) {
        if (StringUtils.isBlank(logContent)) {
            return logContent;
        }
        String[] lines = logContent.split("\\r?\\n");
        int failureLine = findFailureLine(lines);
        if (failureLine < 0) {
            return trim(logContent, 6000);
        }

        int from = Math.max(0, failureLine - 80);
        int to = Math.min(lines.length - 1, failureLine + 120);
        StringBuilder excerpt = new StringBuilder();
        excerpt.append("失败日志片段，命中行 ").append(failureLine + 1).append("：\n");
        for (int i = from; i <= to; i++) {
            excerpt.append(lines[i]).append('\n');
        }
        return trim(excerpt.toString(), 12000);
    }

    private int findFailureLine(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            if (isFailureLine(lines[i])) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFailureLine(String line) {
        if (line == null) {
            return false;
        }
        String lower = line.toLowerCase();
        if (lower.startsWith("error") || lower.contains(" error") || lower.contains("[error]")) {
            return true;
        }
        return containsAny(lower,
                "\terror", "exception", "caused by", "traceback",
                "failed", "failure", "exit code", "syntax error", "permission denied",
                "access denied", "connection refused", "connect timed out", "read timed out",
                "connection reset", "unknownhost", "outofmemory", "java heap space",
                "no space left", "sqlsyntax", "parseexception", "analysisexception",
                "任务失败", "执行失败", "失败", "异常", "报错", "表不存在", "字段不存在");
    }

    private boolean containsAny(String text, String... patterns) {
        if (text == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(value.length() - maxLength);
    }

    private String append(String oldValue, String value) {
        if (StringUtils.isBlank(oldValue)) {
            return value;
        }
        if (StringUtils.isBlank(value)) {
            return oldValue;
        }
        return oldValue + "；" + value;
    }

    @Data
    private static class OpsAnalysis {
        private String failureType;
        private String riskLevel;
        private boolean recoverable;
        private String reason;
        private String suggestion;
        private String aiRawResult;
    }
}
