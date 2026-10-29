package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.flinkx.core.FlinkxEtlTaskConverter;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import com.datamaster.module.collector.service.etl.incremental.IncrementalBoundaryQuery;
import com.datamaster.module.collector.utils.StageIncrementalSql;
import com.datamaster.module.collector.utils.ds.component.TableMergeComponent;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 每个增量阶段在其前置任务结束后固定多源窗口，并返回本阶段运行 JSON。 */
@Service
public class CollectorEtlStageRuntimeService {
    public static final String KIND = "STAGE_RUNTIME_V1";
    @Resource private CollectorEtlTaskMapper taskMapper;
    @Resource private ICollectorEtlTaskExtService extService;
    @Resource private IDatasourceApiService datasourceApi;
    @Resource private IDsEtlTaskService dsService;
    @Resource private IncrementalBoundaryQuery boundaryQuery;
    @Resource private CollectorEtlIncrementalWindowService windowService;
    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper runMapper;
    @Value("${ds.incremental_fixed_window.enabled:false}") private boolean enabled;

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String prepareExecution(Long taskId, String stageKey, Long executionId) {
        if (executionId == null || runMapper.lockTask(taskId) == null) throw new ServiceException("本地执行记录不存在");
        CollectorEtlRunDO run = runMapper.lockById(executionId);
        if (run == null || !taskId.equals(run.getTaskId()) || !Objects.equals(executionId, runMapper.taskOwner(taskId))) {
            throw new ServiceException("本地执行记录不属于当前任务运行");
        }
        return prepare(taskId, stageKey, run.getWorkflowInstanceId());
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String prepare(Long taskId, String stageKey, Long instanceId) {
        if (!enabled) throw new ServiceException("阶段固定增量窗口未启用");
        if (taskId == null || instanceId == null || instanceId <= 0 || stageKey == null || stageKey.trim().isEmpty()) {
            throw new ServiceException("缺少有效的任务、阶段或运行实例");
        }
        if (runMapper.lockTask(taskId) == null) throw new ServiceException("增量任务不存在");
        CollectorEtlTaskDO task = taskMapper.selectById(taskId);
        CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
        if (task == null || ext == null) throw new ServiceException("增量任务不存在");
        if (!"1".equals(task.getStatus())) throw new ServiceException("组件任务已下线，禁止准备新的读取阶段");
        JSONObject envelope = snapshot(ext);
        JSONObject remote = dsService.getWorkflowInstance(task.getSpaceCode(), instanceId);
        if (!String.valueOf(envelope.get("workflowCode")).equals(remote.getString("workflowDefinitionCode"))
                || !String.valueOf(envelope.get("workflowVersion")).equals(remote.getString("workflowDefinitionVersion"))
                || !Arrays.asList("RUNNING_EXECUTION", "DELAY_EXECUTION").contains(remote.getString("state"))) {
            throw new ServiceException("增量运行与已发布快照的工作流/版本不一致或已结束");
        }
        CollectorEtlRunDO run = runMapper.getByWorkflowInstance(taskId, instanceId);
        if (run != null) run = runMapper.lockById(run.getId());
        if (run == null || !"ACTIVE".equals(run.getStatus()) || !Objects.equals(run.getId(), runMapper.taskOwner(taskId))
                || !Objects.equals(String.valueOf(run.getWorkflowCode()), envelope.getString("workflowCode"))) {
            throw new ServiceException("工作流尚未通过运行准入或已失去运行资格");
        }
        JSONObject stage = envelope.getJSONObject("stages").getJSONObject(stageKey);
        if (stage == null) throw new ServiceException("已发布工作流不包含该增量阶段");
        java.util.List<com.datamaster.api.ds.api.etl.ds.TaskInstance> prepareNodes = dsService.listTaskInstancesStrict(task.getSpaceCode(), instanceId);
        boolean activePrepare = prepareNodes.stream().anyMatch(node -> stage.getString("prepareNodeCode").equals(node.getTaskCode())
                && node.getState() != null && "RUNNING_EXECUTION".equals(node.getState().name()));
        if (!activePrepare) throw new ServiceException("当前阶段的准备节点未在运行，禁止提前查询下游增量边界");
        JSONObject config = stage.getJSONObject("config");
        com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO source = publishedDatasource(config.getJSONObject("readerDatasource"), task);
        publishedDatasource(config.getJSONObject("output").getJSONObject("writerDatasource"), task);
        List<JSONObject> cursors = config.getJSONObject("incremental").getJSONArray("sources").toJavaList(JSONObject.class);
        Map<String, String> initials = new TreeMap<>(), cursorTypes = new TreeMap<>();
        for (JSONObject cursor : cursors) {
            initials.put(cursor.getString("nodeId"), cursor.getString("initialValue"));
            cursorTypes.put(cursor.getString("nodeId"), cursor.getString("cursorType"));
        }
        CollectorEtlIncrementalWindowDO window = windowService.prepare(taskId, stageKey, run.getId(),
                stage.getString("configHash"), initials, cursorTypes, () -> {
                    Map<String, String> maxima = new TreeMap<>();
                    for (JSONObject cursor : cursors) {
                        Object maximum = boundaryQuery.queryComponentMaxValue(source,
                                cursor.getString("table"), cursor.getString("cursorColumn"));
                        maxima.put(cursor.getString("nodeId"), boundary(maximum));
                    }
                    return maxima;
                }, previous -> confirmedStopped(taskId, task.getSpaceCode(), previous));
        windowService.describeWindow(taskId, stageKey, run.getId(), stage.getString("dsNodeCode"), 0, JSON.toJSONString(cursors));
        Map<String, String> starts = CollectorEtlIncrementalWindowService.parse(window.getStartValue());
        Map<String, String> ends = CollectorEtlIncrementalWindowService.parse(window.getEndValue());
        JSONObject args = JSON.parseObject(stage.getJSONObject("args").toJSONString());
        JSONObject runtimeConfig = JSON.parseObject(config.toJSONString());
        String database = runtimeConfig.getJSONObject("readerDatasource").getString("datasourceType");
        if ("MERGE".equals(runtimeConfig.getString("mode"))) {
            List<Map<String, Object>> inputs = new ArrayList<>();
            List<Map<String, Object>> filters = new ArrayList<>();
            for (Object raw : runtimeConfig.getJSONArray("inputs")) {
                JSONObject input = JSON.parseObject(JSON.toJSONString(raw));
                String ref = input.getString("refNodeId");
                JSONObject cursor = cursors.stream().filter(item -> ref.equals(item.getString("nodeId"))).findFirst()
                        .orElseThrow(() -> new ServiceException("已发布游标与实际读取源不一致"));
                if (runtimeConfig.getJSONObject("joinConfig") != null) {
                    Map<String, Object> filter = new LinkedHashMap<>();
                    filter.put("nodeId", ref); filter.put("column", cursor.getString("cursorColumn"));
                    filter.put("cursorType", cursor.getString("cursorType"));
                    filter.put("start", starts.get(ref)); filter.put("end", ends.get(ref)); filters.add(filter);
                    List<Object> columns = new ArrayList<>((List<?>) input.get("columns"));
                    if (!columns.contains(cursor.getString("cursorColumn"))) columns.add(cursor.getString("cursorColumn"));
                    input.put("columns", columns);
                } else {
                    String predicate = StageIncrementalSql.predicate(cursor.getString("cursorColumn"), cursor.getString("cursorType"),
                            starts.get(ref), ends.get(ref), database, null);
                    String base = input.getString("where");
                    input.put("where", base == null || base.trim().isEmpty() ? predicate : "(" + base + ") AND (" + predicate + ")");
                }
                input.put("table_name", input.getString("table")); inputs.add(input);
            }
            if (runtimeConfig.getJSONObject("joinConfig") != null) runtimeConfig.getJSONObject("joinConfig").put("sourceFilters", filters);
            runtimeConfig.put("sourceTables", inputs);
            runtimeConfig.put("componentStage", true);
            Map<String, Object> parsed = new TableMergeComponent().parse2(stageKey, 1, TaskComponentTypeEnum.TABLE_MERGE,
                    runtimeConfig, "", Collections.emptyList());
            args.put("reader", parsed);
        } else {
            JSONObject cursor = cursors.get(0);
            JSONObject reader = args.getJSONObject("reader").getJSONObject("parameter");
            String predicate = StageIncrementalSql.predicate(cursor.getString("cursorColumn"), cursor.getString("cursorType"),
                    starts.get(cursor.getString("nodeId")), ends.get(cursor.getString("nodeId")), database, null);
            if (reader.getJSONObject("connection") == null || reader.getJSONObject("connection").get("querySql") != null) {
                throw new ServiceException("自定义读取SQL或非关系输入暂不支持自动增量窗口");
            }
            String base = reader.getString("where");
            reader.put("where", base == null || base.trim().isEmpty() ? predicate : "(" + base + ") AND (" + predicate + ")");
        }
        return FlinkxEtlTaskConverter.convertToFlinkxJobJson(args);
    }

    /** 由已确认整个工作流终态的对账器调用；同事务提交所有已准备阶段。 */
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY, rollbackFor = Exception.class)
    public void finish(Long taskId, Long instanceId, Long workflowCode, boolean success,
                       List<com.datamaster.api.ds.api.etl.ds.TaskInstance> nodes) {
        CollectorEtlTaskExtDO ext = extService.getByTaskId(taskId);
        if (ext == null || ext.getFlinkxJobTemplateJson() == null) throw new ServiceException("组件运行缺少发布快照，保留运行资格等待核对");
        JSONObject envelope = JSON.parseObject(ext.getFlinkxJobTemplateJson());
        if (!KIND.equals(envelope.getString("kind"))) throw new ServiceException("组件运行发布快照类型不一致");
        if (!String.valueOf(workflowCode).equals(envelope.getString("workflowCode"))) throw new ServiceException("运行水位与发布工作流不一致");
        CollectorEtlRunDO localRun = runMapper.getByWorkflowInstance(taskId, instanceId);
        if (localRun == null) throw new ServiceException("本地运行记录不存在");
        JSONObject stages = envelope.getJSONObject("stages");
        if (stages == null || nodes == null) throw new ServiceException("组件阶段快照或调度节点列表不完整");
        if (nodes.stream().anyMatch(node -> node == null || node.getId() == null || !instanceId.equals(node.getProcessInstanceId()))) {
            throw new ServiceException("阶段调度节点身份不完整或属于其他实例");
        }
        for (String stageKey : new TreeSet<>(stages.keySet())) {
            JSONObject stage = stages.getJSONObject(stageKey);
            if (success) {
                List<com.datamaster.api.ds.api.etl.ds.TaskInstance> executed = new ArrayList<>();
                for (com.datamaster.api.ds.api.etl.ds.TaskInstance node : nodes) {
                    if (stage.getString("dsNodeCode").equals(node.getTaskCode())) executed.add(node);
                }
                // 条件跳过的阶段没有窗口；强制成功不证明数据已写完，不能自动提交水位。
                CollectorEtlIncrementalWindowDO window = windowService.getWindow(taskId, stageKey, localRun.getId());
                if (executed.isEmpty()) {
                    if (window != null) throw new ServiceException("增量阶段已有窗口但未确认实际数据任务成功");
                    continue;
                }
                com.datamaster.api.ds.api.etl.ds.TaskInstance latest = null;
                for (com.datamaster.api.ds.api.etl.ds.TaskInstance attempt : executed) {
                    if (latest == null || (attempt.getId() != null && (latest.getId() == null || attempt.getId() > latest.getId()))) latest = attempt;
                }
                if (window == null || latest == null || latest.getState() == null
                        || !"SUCCESS".equals(latest.getState().name())) {
                    throw new ServiceException("增量阶段缺少固定窗口或最新数据任务未实际成功，禁止推进水位");
                }
            }
            windowService.finish(taskId, stageKey, localRun.getId(), stage.getString("configHash"), success);
        }
    }
    private JSONObject snapshot(CollectorEtlTaskExtDO ext) {
        if (ext.getFlinkxJobTemplateJson() == null) throw new ServiceException("缺少已发布阶段快照");
        JSONObject value = JSON.parseObject(ext.getFlinkxJobTemplateJson());
        if (!KIND.equals(value.getString("kind"))) throw new ServiceException("任务不是组件式增量发布快照");
        return value;
    }
    private boolean confirmedStopped(Long taskId, String space, Long localId) {
        CollectorEtlRunDO previous = runMapper.lockById(localId);
        if (previous == null || !taskId.equals(previous.getTaskId())) return false;
        if (Arrays.asList("FAILURE", "STOP").contains(previous.getStatus())) return true;
        JSONObject instance = dsService.getWorkflowInstance(space, previous.getWorkflowInstanceId());
        if (!Arrays.asList("FAILURE", "STOP").contains(instance.getString("state")) || instance.get("endTime") == null) return false;
        return dsService.listTaskInstancesStrict(space, previous.getWorkflowInstanceId()).stream().allMatch(task -> task.getState() != null
                && Arrays.asList("SUCCESS", "FAILURE", "STOP", "KILL", "FORCED_SUCCESS").contains(task.getState().name()));
    }
    private com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO publishedDatasource(JSONObject config, CollectorEtlTaskDO task) {
        if (config == null) throw new ServiceException("读取数据源快照缺失");
        Long id = datasourceId(config);
        if (!datasourceApi.hasDatasourceSpace(id, task.getSpaceId(), task.getSpaceCode())) throw new ServiceException("读取数据源空间授权已失效");
        com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO registered = datasourceApi.getDatasourceById(id);
        if (registered == null || Boolean.FALSE.equals(registered.getValidFlag())) throw new ServiceException("读取数据源已不存在或停用");
        com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO source =
                config.toJavaObject(com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO.class);
        source.setId(id);
        if (source.getIp() == null || source.getPort() == null || source.getDatasourceConfig() == null
                || source.getDatasourceType() == null) throw new ServiceException("读取数据源发布快照不完整，请重新发布");
        return source;
    }

    private Long datasourceId(JSONObject datasource) {
        Long id = datasource.getLong("datasourceId");
        if (id == null) id = datasource.getLong("id");
        if (id == null) throw new ServiceException("读取数据源缺少已登记标识");
        return id;
    }
    private String boundary(Object value) {
        if (value == null) return null;
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime().format(format);
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).format(format);
        if (value instanceof LocalDate) return value.toString();
        return String.valueOf(value);
    }
}
