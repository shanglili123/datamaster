package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.enums.WorkflowExecutionStatus;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlIncrementalService;
import com.datamaster.module.collector.service.etl.ICollectorEtlSchedulerService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsService;
import com.datamaster.module.collector.service.etl.incremental.IncrementalBoundaryQuery;
import com.datamaster.module.collector.utils.model.FlinkxIncrementalConfig;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.redis.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Service
public class CollectorEtlIncrementalServiceImpl implements ICollectorEtlIncrementalService {

    private static final String RUNNING_KEY_PREFIX = "COL:ETL:FLINKX:INCREMENTAL:RUNNING:";

    @Resource
    private ICollectorEtlTaskExtService collectorEtlTaskExtService;
    @Resource
    private CollectorEtlTaskMapper collectorEtlTaskMapper;
    @Resource
    private IAssetsDatasourceApiService assetsDatasourceApiService;
    @Resource
    private IncrementalBoundaryQuery incrementalBoundaryQuery;
    @Resource
    private ICollectorEtlSchedulerService collectorEtlSchedulerService;
    @Resource
    private IDsEtlSchedulerService dsEtlSchedulerService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;
    @Resource
    private ICollectorEtlTaskInstanceService collectorEtlTaskInstanceService;
    @Resource
    private CollectorEtlTaskStatusPushService collectorEtlTaskStatusPushService;
    @Resource
    private ICollectorEtlTaskOpsService collectorEtlTaskOpsService;
    @Resource
    private IRedisService redisService;

    @Value("${ds.incremental_running_ttl_seconds:86400}")
    private long incrementalRunningTtlSeconds;

    @Override
    public String prepareIncrementalTask(Long taskId, Long processInstanceId) {
        validateProcessInstanceId(processInstanceId);
        CollectorEtlTaskDO task = collectorEtlTaskMapper.selectById(taskId);
        CollectorEtlTaskExtDO taskExt = collectorEtlTaskExtService.getByTaskId(taskId);
        acquireRunningSlot(taskId, processInstanceId);
        try {
            validateTask(task, taskExt);
            AssetsDatasourceRespDTO source = assetsDatasourceApiService.getDatasourceById(taskExt.getSourceDatasourceId());
            AssetsDatasourceRespDTO target = assetsDatasourceApiService.getDatasourceById(taskExt.getTargetDatasourceId());

            Object sourceMax = incrementalBoundaryQuery.queryMaxValue(
                    source, taskExt.getSourceTableName(), taskExt.getSourceIncrementColumn());
            Object targetMax = incrementalBoundaryQuery.queryMaxValue(
                    target, taskExt.getTargetTableName(), taskExt.getTargetIncrementColumn());

            String startValue = formatBoundary(
                    targetMax == null ? taskExt.getIncrementalInitialValue() : targetMax,
                    taskExt.getIncrementalType(), taskExt.getIncrementalTimeFormat());
            String endValue = sourceMax == null ? null : formatBoundary(
                    sourceMax, taskExt.getIncrementalType(), taskExt.getIncrementalTimeFormat());
            String runtimeJobJson = buildRuntimeJobJson(taskExt, startValue, endValue);

            collectorEtlTaskExtService.lambdaUpdate()
                    .eq(CollectorEtlTaskExtDO::getId, taskExt.getId())
                    .set(CollectorEtlTaskExtDO::getIncrementalStartValue, startValue)
                    .set(CollectorEtlTaskExtDO::getIncrementalEndValue, endValue)
                    .set(CollectorEtlTaskExtDO::getFlinkxJobJson, runtimeJobJson)
                    .update();
            return runtimeJobJson;
        } catch (Exception e) {
            releaseRunningSlot(taskId, processInstanceId);
            unloadAfterFailure(task, taskExt);
            throw e instanceof ServiceException
                    ? (ServiceException) e
                    : new ServiceException("增量同步准备失败: " + e.getMessage());
        }
    }

    @Override
    public void completeIncrementalTask(Long taskId, Long processInstanceId, Integer status) {
        validateProcessInstanceId(processInstanceId);
        CollectorEtlTaskInstanceDO instance = collectorEtlTaskInstanceService.getByDsId(processInstanceId);
        Date now = new Date();
        if (instance == null) {
            CollectorEtlTaskDO task = collectorEtlTaskMapper.selectById(taskId);
            CollectorEtlTaskExtDO taskExt = collectorEtlTaskExtService.getByTaskId(taskId);
            if (task == null) {
                log.warn("FLINKX完成回调: 任务不存在, taskId={}", taskId);
                return;
            }
            String taskType = task.getType();
            if (taskType == null) {
                taskType = "1";
            }
            // 🏆 关键改进：根据status参数决定任务状态
            instance = CollectorEtlTaskInstanceDO.builder()
                    .id(processInstanceId)
                    .catId(task.getCatId())
                    .catCode(task.getCatCode())
                    .taskId(task.getId())
                    .taskCode(resolveTaskCode(task, taskExt))
                    .taskType(taskType)
                    .taskVersion(resolveTaskVersion(taskExt))
                    .name(task.getName())
                    .personCharge(task.getPersonCharge())
                    .contactNumber(task.getContactNumber())
                    .projectId(task.getProjectId())
                    .projectCode(task.getProjectCode())
                    // 🏆 根据status参数设置任务状态
                    .status(status == 1 ? String.valueOf(WorkflowExecutionStatus.SUCCESS.getCode()) : String.valueOf(WorkflowExecutionStatus.FAILURE.getCode()))
                    .endTime(now)
                    .startTime(now)
                    .dsId(processInstanceId)
                    .build();
            collectorEtlTaskInstanceService.save(instance);
        } else {
            if (!taskId.equals(instance.getTaskId())) {
                throw new ServiceException("DolphinScheduler流程实例不属于当前增量任务");
            }
            // 如果前端已经传递了status，直接使用前端传入的状态
            if (status != null && status >= 0) {
                // 🏆 根据status参数确定任务最终状态
                if (status == 1) {
                    instance.setStatus(String.valueOf(WorkflowExecutionStatus.SUCCESS.getCode()));
                    log.info("任务 {} 标记为成功，status={}", taskId, status);
                } else {
                    instance.setStatus(String.valueOf(WorkflowExecutionStatus.FAILURE.getCode()));
                    log.info("任务 {} 标记为失败，status={}", taskId, status);
                }
            } else {
                instance.setStatus(String.valueOf(WorkflowExecutionStatus.FAILURE.getCode()));
                log.warn("FLINKX任务 {} 完成回调未携带状态，默认标记为失败", taskId);
            }
            instance.setEndTime(now);
            if (instance.getStartTime() == null) {
                instance.setStartTime(now);
            }
            collectorEtlTaskInstanceService.updateById(instance);
        }
        collectorEtlTaskStatusPushService.pushTaskInstanceStatus(instance);
        triggerOpsIfFailed(instance);
        releaseRunningSlot(taskId, processInstanceId);
    }

    @Override
    public void releaseIncrementalTask(Long taskId, Long processInstanceId) {
        releaseRunningSlot(taskId, processInstanceId);
    }

    @Override
    public void forceReleaseIncrementalTask(Long taskId) {
        if (taskId != null) {
            redisService.delete(runningKey(taskId));
        }
    }

    private void triggerOpsIfFailed(CollectorEtlTaskInstanceDO instance) {
        if (instance == null || !String.valueOf(WorkflowExecutionStatus.FAILURE.getCode()).equals(instance.getStatus())) {
            return;
        }
        try {
            ProcessInstance processInstance = ProcessInstance.builder()
                    .id(instance.getDsId() == null ? instance.getId() : instance.getDsId())
                    .processDefinitionCode(instance.getTaskCode())
                    .projectCode(instance.getProjectCode())
                    .state(WorkflowExecutionStatus.FAILURE)
                    .startTime(instance.getStartTime())
                    .endTime(instance.getEndTime())
                    .runTimes(instance.getRunTimes())
                    .name(instance.getName())
                    .build();
            collectorEtlTaskOpsService.handleProcessInstanceFinished(processInstance, instance);
        } catch (Exception e) {
            log.warn("FLINKX失败回调触发AI运维异常，taskId={}，processInstanceId={}",
                    instance.getTaskId(), instance.getDsId(), e);
        }
    }

    private String resolveTaskCode(CollectorEtlTaskDO task, CollectorEtlTaskExtDO taskExt) {
        if (taskExt != null && StringUtils.isNotBlank(taskExt.getEtlTaskCode())) {
            return taskExt.getEtlTaskCode();
        }
        return task.getCode();
    }

    private Integer resolveTaskVersion(CollectorEtlTaskExtDO taskExt) {
        if (taskExt != null && taskExt.getEtlTaskVersion() != null) {
            return taskExt.getEtlTaskVersion();
        }
        return 0;
    }

    private void validateTask(CollectorEtlTaskDO task, CollectorEtlTaskExtDO taskExt) {
        if (task == null) {
            throw new ServiceException("增量同步任务不存在");
        }
        if (taskExt == null || StringUtils.isBlank(taskExt.getIncrementalType())) {
            throw new ServiceException("任务未配置增量同步");
        }
        if (StringUtils.isBlank(taskExt.getFlinkxJobTemplateJson())) {
            throw new ServiceException("任务缺少FlinkX基础JSON模板");
        }
        if (StringUtils.isBlank(taskExt.getIncrementalInitialValue())) {
            throw new ServiceException("任务缺少首次增量同步初始游标");
        }
        if (StringUtils.isBlank(taskExt.getEtlTaskCode())
                || StringUtils.isBlank(taskExt.getEtlNodeCode())
                || StringUtils.isBlank(taskExt.getPrepareNodeCode())
                || StringUtils.isBlank(taskExt.getCompleteNodeCode())) {
            throw new ServiceException("任务缺少DolphinScheduler节点信息");
        }
    }

    private String buildRuntimeJobJson(CollectorEtlTaskExtDO taskExt, String startValue, String endValue) {
        JSONObject root = JSON.parseObject(taskExt.getFlinkxJobTemplateJson());
        JSONArray contents = root.getJSONObject("job").getJSONArray("content");
        if (contents == null || contents.isEmpty()) {
            throw new ServiceException("FlinkX基础JSON缺少content配置");
        }
        JSONObject readerParameter = contents.getJSONObject(0).getJSONObject("reader").getJSONObject("parameter");
        if (readerParameter == null) {
            throw new ServiceException("FlinkX基础JSON缺少reader.parameter配置");
        }

        String dynamicWhere;
        if (endValue == null) {
            dynamicWhere = "1 = 0";
        } else {
            String column = taskExt.getSourceIncrementColumn();
            if (FlinkxIncrementalConfig.TYPE_TIME.equals(taskExt.getIncrementalType())) {
                dynamicWhere = column + " > '" + escapeSql(startValue) + "' AND "
                        + column + " <= '" + escapeSql(endValue) + "'";
            } else if (FlinkxIncrementalConfig.TYPE_ID.equals(taskExt.getIncrementalType())) {
                validateNumber(startValue);
                validateNumber(endValue);
                dynamicWhere = column + " > " + startValue + " AND " + column + " <= " + endValue;
            } else {
                throw new ServiceException("不支持的增量类型: " + taskExt.getIncrementalType());
            }
        }

        String baseWhere = readerParameter.getString("where");
        readerParameter.put("where", StringUtils.isBlank(baseWhere)
                ? dynamicWhere : "(" + baseWhere + ") AND (" + dynamicWhere + ")");
        return root.toJSONString();
    }

    private String formatBoundary(Object value, String incrementalType, String incrementalTimeFormat) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            throw new ServiceException("增量最大值和初始游标不能同时为空");
        }
        if (FlinkxIncrementalConfig.TYPE_TIME.equals(incrementalType) && value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(
                    DateTimeFormatter.ofPattern(resolveTimeFormat(incrementalTimeFormat)));
        }
        if (FlinkxIncrementalConfig.TYPE_TIME.equals(incrementalType) && value instanceof Date) {
            return new SimpleDateFormat(resolveTimeFormat(incrementalTimeFormat)).format((Date) value);
        }
        return String.valueOf(value);
    }

    private String resolveTimeFormat(String incrementalTimeFormat) {
        return StringUtils.isBlank(incrementalTimeFormat)
                ? FlinkxIncrementalConfig.DEFAULT_TIME_FORMAT : incrementalTimeFormat;
    }

    private void validateNumber(String value) {
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new ServiceException("ID增量边界不是有效数字: " + value);
        }
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private void validateProcessInstanceId(Long processInstanceId) {
        if (processInstanceId == null || processInstanceId <= 0) {
            throw new ServiceException("DolphinScheduler流程实例ID不能为空");
        }
    }

    private void acquireRunningSlot(Long taskId, Long processInstanceId) {
        String key = runningKey(taskId);
        String owner = String.valueOf(processInstanceId);
        if (redisService.setIfAbsent(key, owner, incrementalRunningTtlSeconds)) {
            return;
        }
        if (!owner.equals(redisService.get(key))) {
            throw new ServiceException("增量同步任务正在运行，禁止重叠执行");
        }
    }

    private void releaseRunningSlot(Long taskId, Long processInstanceId) {
        if (taskId == null) {
            return;
        }
        String key = runningKey(taskId);
        if (String.valueOf(processInstanceId).equals(redisService.get(key))) {
            redisService.delete(key);
        }
    }

    private String runningKey(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("增量同步任务ID不能为空");
        }
        return RUNNING_KEY_PREFIX + taskId;
    }

    private void unloadAfterFailure(CollectorEtlTaskDO task, CollectorEtlTaskExtDO taskExt) {
        if (task == null) {
            return;
        }
        String dsTaskCode = taskExt != null && StringUtils.isNotBlank(taskExt.getEtlTaskCode())
                ? taskExt.getEtlTaskCode() : task.getCode();
        try {
            DsStatusRespDTO response = dsEtlTaskService.releaseTask("OFFLINE", task.getProjectCode(), dsTaskCode);
            if (response == null || !response.getSuccess()) {
                log.warn("增量准备失败后下线DS任务失败，taskId={}，msg={}", task.getId(),
                        response == null ? "无响应" : response.getMsg());
            }
        } catch (Exception e) {
            log.warn("增量准备失败后下线DS任务异常，taskId={}", task.getId(), e);
        }
        try {
            CollectorEtlSchedulerPageReqVO query = new CollectorEtlSchedulerPageReqVO();
            query.setTaskId(task.getId());
            CollectorEtlSchedulerDO scheduler = collectorEtlSchedulerService.getCollectorEtlSchedulerById(query);
            if (scheduler != null && scheduler.getDsId() != null && scheduler.getDsId() > 0) {
                dsEtlSchedulerService.offlineScheduler(task.getProjectCode(), scheduler.getDsId());
            }
        } catch (Exception e) {
            log.warn("增量准备失败后下线DS调度器异常，taskId={}", task.getId(), e);
        }
        task.setStatus("0");
        collectorEtlTaskMapper.updateById(task);
    }
}
