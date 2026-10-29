

package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DSExecuteDTO;
import com.datamaster.api.ds.api.etl.DsLogResultDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.ExecuteType;
import com.datamaster.common.enums.Flag;
import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.DateUtils;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeInstanceRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceLogStatusRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskInstanceService;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceLogDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlIncrementalWindowDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskInstanceMapper;
import com.datamaster.module.collector.service.etl.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.datamaster.common.core.domain.AjaxResult.error;
import static com.datamaster.common.core.domain.AjaxResult.success;

/**
 * 数据集成任务实例Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskInstanceServiceImpl extends ServiceImpl<CollectorEtlTaskInstanceMapper, CollectorEtlTaskInstanceDO> implements ICollectorEtlTaskInstanceService, CollectorEtlTaskInstanceService {
    @Resource
    private CollectorEtlTaskInstanceMapper CollectorEtlTaskInstanceMapper;

    @Resource
    private ISpaceApi attProjectApi;

    @Resource
    private ICollectorEtlTaskService CollectorEtlTaskService;

    @Resource
    private ICollectorEtlTaskLogService CollectorEtlTaskLogService;

    @Resource
    private IDsEtlExecutorService dsEtlExecutorService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;

    @Resource
    private ICollectorEtlNodeInstanceService CollectorEtlTNodeInstanceService;


    @Resource
    private ICollectorEtlTaskInstanceLogService CollectorEtlTaskInstanceLogService;

    @Resource
    private ICollectorEtlNodeInstanceLogService CollectorEtlNodeInstanceLogService;


    @Resource
    private ICollectorEtlNodeLogService CollectorEtlNodeLogService;

    @Resource
    private ICollectorEtlTaskNodeRelService iCollectorEtlTaskNodeRelService;
    @Resource
    @Lazy
    private ICollectorEtlTaskOpsService collectorEtlTaskOpsService;

    @Resource
    private CollectorEtlTaskStatusPushService collectorEtlTaskStatusPushService;

    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper ownerTaskMapper;
    @Resource private ICollectorEtlTaskExtService ownerTaskExtService;
    @Resource private CollectorEtlIncrementalWindowService stageWindowService;
    @Resource @Lazy private CollectorEtlRunGuardService componentRunGuard;
    @org.springframework.beans.factory.annotation.Value("${ds.incremental_fixed_window.enabled:false}") private boolean stageWindowsEnabled;

    @Override
    public List<CollectorEtlStageWindowRespVO> getStageWindows(Long taskInstanceId) {
        CollectorEtlTaskInstanceDO parent = requireInstance(taskInstanceId);
        if (!stageWindowsEnabled || parent.getDsId() == null) return Collections.emptyList();
        List<CollectorEtlStageWindowRespVO> result = new ArrayList<>();
        for (CollectorEtlIncrementalWindowDO window : stageWindowService.listWindows(parent.getTaskId(), parent.getId())) {
            Map<String, String> starts = CollectorEtlIncrementalWindowService.parse(window.getStartValue());
            Map<String, String> ends = CollectorEtlIncrementalWindowService.parse(window.getEndValue());
            Set<String> sources = new TreeSet<>(starts.keySet()); sources.addAll(ends.keySet());
            Map<String, JSONObject> descriptions = new HashMap<>();
            if (StringUtils.isNotBlank(window.getSourcesJson())) {
                for (JSONObject source : com.alibaba.fastjson2.JSON.parseArray(window.getSourcesJson(), JSONObject.class)) {
                    descriptions.put(source.getString("nodeId"), source);
                }
            }
            for (String source : sources) {
                CollectorEtlStageWindowRespVO row = new CollectorEtlStageWindowRespVO();
                row.setTaskId(parent.getTaskId()); row.setTaskInstanceId(parent.getId()); row.setWorkflowInstanceId(parent.getDsId());
                row.setStageKey(window.getStageKey()); row.setSourceNodeId(source);
                row.setReplayFromId(window.getReplayFromId());
                JSONObject description = descriptions.get(source);
                if (description != null) {
                    row.setSourceTable(description.getString("table")); row.setCursorColumn(description.getString("cursorColumn"));
                    row.setCursorType(description.getString("cursorType"));
                }
                row.setStartValue(starts.get(source)); row.setEndValue(ends.get(source)); row.setEmpty(ends.get(source) == null);
                row.setStatus(window.getStatus()); result.add(row);
            }
        }
        return result;
    }

    @Override
    public void synchronizeComponentRun(Long taskId, JSONObject remote, List<TaskInstance> nodes) {
        if (taskId == null || remote == null || remote.getLong("id") == null) throw new ServiceException("缺少组件运行身份");
        if (CollectorEtlTaskInstanceMapper.lockOwnerTask(taskId) == null) throw new ServiceException("运行所属任务不存在");
        com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO ext = ownerTaskExtService.getByTaskId(taskId);
        JSONObject snapshot = ext == null || ext.getFlinkxJobTemplateJson() == null ? null : JSONObject.parseObject(ext.getFlinkxJobTemplateJson());
        if (snapshot == null || !CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind"))
                || !Objects.equals(snapshot.getString("workflowCode"), remote.getString("workflowDefinitionCode"))
                || !Objects.equals(snapshot.getString("workflowVersion"), remote.getString("workflowDefinitionVersion"))) {
            throw new ServiceException("组件运行与已发布快照不一致");
        }
        synchronizeSchedulerInstance(taskId, remote);
        CollectorEtlTaskInstanceDO parent = CollectorEtlTaskInstanceMapper.lockBySchedulerId(remote.getLong("id"));
        CollectorEtlTNodeInstanceService.synchronizeSchedulerNodes(parent, nodes);
    }

    @Override
    public void synchronizeSchedulerInstance(Long taskId, JSONObject remote) {
        if (taskId == null || remote == null || remote.getLong("id") == null) throw new ServiceException("缺少调度实例身份");
        if (CollectorEtlTaskInstanceMapper.lockOwnerTask(taskId) == null) throw new ServiceException("实例所属任务不存在");
        CollectorEtlTaskDO task = ownerTaskMapper.selectById(taskId);
        com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO ext = ownerTaskExtService.getByTaskId(taskId);
        String workflowCode = remote.getString("workflowDefinitionCode");
        if (ext == null || workflowCode == null || !workflowCode.equals(ext.getEtlTaskCode())) throw new ServiceException("DS实例不属于该任务已发布工作流");
        String rawState = remote.getString("state");
        com.datamaster.common.enums.WorkflowExecutionStatus state;
        try { state = com.datamaster.common.enums.WorkflowExecutionStatus.valueOf(rawState); }
        catch (Exception ex) { throw new ServiceException("无法识别DS工作流状态，保留原状态等待核对"); }
        boolean terminal = Arrays.asList("SUCCESS", "FAILURE", "STOP").contains(rawState);
        if (terminal && remote.get("endTime") == null) throw new ServiceException("DS终态缺少结束时间");
        CollectorEtlTaskInstanceDO instance = CollectorEtlTaskInstanceMapper.lockBySchedulerId(remote.getLong("id"));
        if (instance != null) {
            if (!taskId.equals(instance.getTaskId())) throw new ServiceException("实例任务归属不一致");
            if (Arrays.asList("5", "6", "7").contains(instance.getStatus())) return;
        } else {
            instance = CollectorEtlTaskInstanceDO.builder()
                    .id(com.datamaster.common.utils.uuid.IdUtils.generateArtificialId()).taskId(taskId).dsId(remote.getLong("id"))
                    .catId(task.getCatId()).catCode(task.getCatCode()).taskType(task.getType())
                    .taskCode(workflowCode).taskVersion(remote.getInteger("workflowDefinitionVersion"))
                    .name(remote.getString("name")).spaceId(task.getSpaceId()).spaceCode(task.getSpaceCode())
                    .subTaskFlag(String.valueOf(Flag.NO.getCode()))
                    .personCharge(task.getPersonCharge()).contactNumber(task.getContactNumber()).build();
        }
        instance.setStatus(String.valueOf(state.getCode()));
        Date start = remote.getDate("startTime");
        if (start != null) instance.setStartTime(start);
        if (terminal) instance.setEndTime(remote.getDate("endTime"));
        Integer times = remote.getInteger("runTimes"); if (times != null) instance.setRunTimes(times);
        Date schedule = remote.getDate("scheduleTime"); if (schedule != null) instance.setScheduleTime(schedule);
        String command = remote.getString("commandType");
        if (command != null) {
            for (com.datamaster.common.enums.CommandType type : com.datamaster.common.enums.CommandType.values()) {
                if (type.name().equals(command)) instance.setCommandType(String.valueOf(type.getCode()));
            }
        }
        CollectorEtlTaskInstanceDO old = CollectorEtlTaskInstanceMapper.selectById(instance.getId());
        if (old == null ? !save(instance) : !updateById(instance)) {
            throw new ServiceException("调度实例记录保存失败");
        }
        CollectorEtlTaskInstanceDO committedInstance = instance;
        boolean component = isComponentRuntimeInstance(instance);
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() {
                            try { pushConfirmedInstance(committedInstance, component); }
                            catch (RuntimeException ex) { log.warn("调度状态已入库，页面推送失败 instanceId={}", committedInstance.getId()); }
                        }
                    });
        } else {
            pushConfirmedInstance(committedInstance, component);
        }
    }

    private void pushConfirmedInstance(CollectorEtlTaskInstanceDO instance, boolean component) {
        if (component) {
            collectorEtlTaskStatusPushService.pushComponentTaskInstanceStatus(instance.getTaskId(), instance.getDsId());
        } else {
            collectorEtlTaskStatusPushService.pushTaskInstanceStatus(instance);
        }
    }

    private void requireSpace(Long spaceId, String spaceCode) {
        Long userId = com.datamaster.common.utils.SecurityUtils.getUserId();
        if (com.datamaster.common.utils.SecurityUtils.isAdmin(userId)) return;
        if ((spaceId == null && StringUtils.isBlank(spaceCode))
                || !attProjectApi.hasUserSpace(userId, spaceId, spaceCode)) {
            throw new ServiceException("无权访问该空间的任务实例", 403);
        }
    }

    private CollectorEtlTaskDO requireOwnerTask(Long taskId) {
        CollectorEtlTaskDO task = taskId == null ? null : ownerTaskMapper.selectById(taskId);
        if (task == null) throw new ServiceException("实例所属任务不存在");
        requireSpace(task.getSpaceId(), task.getSpaceCode());
        return task;
    }

    private CollectorEtlTaskInstanceDO requireInstance(Long instanceId) {
        CollectorEtlTaskInstanceDO instance = instanceId == null ? null : baseMapper.selectById(instanceId);
        if (instance == null) throw new ServiceException("任务实例不存在");
        CollectorEtlTaskDO task = requireOwnerTask(instance.getTaskId());
        if (!Objects.equals(task.getSpaceId(), instance.getSpaceId())
                || !Objects.equals(task.getSpaceCode(), instance.getSpaceCode())) {
            throw new ServiceException("任务实例空间归属不一致");
        }
        return instance;
    }

    private void requireQueryScope(CollectorEtlTaskInstancePageReqVO request) {
        if (request.getId() != null) {
            CollectorEtlTaskInstanceDO instance = requireInstance(request.getId());
            request.setSpaceId(instance.getSpaceId()); request.setSpaceCode(instance.getSpaceCode());
        }
        if (request.getTaskId() != null) {
            CollectorEtlTaskDO task = requireOwnerTask(request.getTaskId());
            request.setSpaceId(task.getSpaceId()); request.setSpaceCode(task.getSpaceCode());
        }
        if (request.getDsId() != null) {
            CollectorEtlTaskInstanceDO instance = getByDsId(request.getDsId());
            if (instance == null) throw new ServiceException("调度实例不存在");
            requireInstance(instance.getId());
            request.setSpaceId(instance.getSpaceId()); request.setSpaceCode(instance.getSpaceCode());
        }
        requireSpace(request.getSpaceId(), request.getSpaceCode());
        if (request.getSpaceId() != null) {
            String resolved = attProjectApi.getSpaceCodeBySpaceId(request.getSpaceId());
            if (StringUtils.isBlank(resolved)) throw new ServiceException("查询空间不存在或未配置编码");
            if (StringUtils.isNotBlank(request.getSpaceCode()) && !Objects.equals(resolved, request.getSpaceCode())) {
                throw new ServiceException("查询空间编码与空间ID不一致");
            }
            request.setSpaceCode(resolved);
        }
    }

    @Override
    public PageResult<CollectorEtlTaskInstanceDO> getCollectorEtlTaskInstancePage(CollectorEtlTaskInstancePageReqVO pageReqVO) {
        requireQueryScope(pageReqVO);
        return CollectorEtlTaskInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public CollectorEtlTaskInstanceRespVO getCollectorEtlTaskInstanceById(CollectorEtlTaskInstancePageReqVO reqVO) {
        requireQueryScope(reqVO);
//        MPJLambdaWrapper<CollectorEtlTaskInstanceDO> wrapper = new MPJLambdaWrapper<>();
//        wrapper.selectAll(CollectorEtlTaskInstanceDO.class)
//                .eq(StringUtils.isNotBlank(reqVO.getTaskCode()), CollectorEtlTaskInstanceDO::getTaskCode, reqVO.getTaskCode())
//                .orderByStr(true,
//                       false,
//                        Arrays.asList( "create_time","id"));
//        List<CollectorEtlTaskInstanceDO> CollectorEtlTaskInstanceDOList = CollectorEtlTaskInstanceMapper.selectList(wrapper);
//        if (CollectionUtils.isNotEmpty(CollectorEtlTaskInstanceDOList)){
//            return BeanUtils.toBean(CollectorEtlTaskInstanceDOList.get(0), CollectorEtlTaskInstanceRespVO.class);
//
//        }
        CollectorEtlTaskInstanceDO dictType = BeanUtils.toBean(reqVO, CollectorEtlTaskInstanceDO.class);

        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlTaskInstanceMapper.selectOneNew(dictType);
        if (CollectorEtlTaskInstanceDO != null) requireInstance(CollectorEtlTaskInstanceDO.getId());

        return BeanUtils.toBean(CollectorEtlTaskInstanceDO, CollectorEtlTaskInstanceRespVO.class);
    }

    @Override
    public Long createCollectorEtlTaskInstance(CollectorEtlTaskInstanceSaveReqVO createReqVO) {
        if (!com.datamaster.common.utils.SecurityUtils.isAdmin(com.datamaster.common.utils.SecurityUtils.getUserId())) {
            throw new ServiceException("运行实例由调度器同步创建，不允许普通用户手工写入", 403);
        }
        CollectorEtlTaskDO task = requireOwnerTask(createReqVO.getTaskId());
        createReqVO.setSpaceId(task.getSpaceId()); createReqVO.setSpaceCode(task.getSpaceCode());
        CollectorEtlTaskInstanceDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlTaskInstanceDO.class);
        CollectorEtlTaskInstanceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlTaskInstance(CollectorEtlTaskInstanceSaveReqVO updateReqVO) {
        if (!com.datamaster.common.utils.SecurityUtils.isAdmin(com.datamaster.common.utils.SecurityUtils.getUserId())) {
            throw new ServiceException("运行状态只能由调度核对更新", 403);
        }
        CollectorEtlTaskInstanceDO existing = requireInstance(updateReqVO.getId());
        if (existing.getRunOwnerToken() != null) throw new ServiceException("组件运行记录不允许手工编辑");
        if (updateReqVO.getTaskId() != null && !Objects.equals(existing.getTaskId(), updateReqVO.getTaskId())) {
            throw new ServiceException("不允许修改运行实例的任务归属");
        }
        updateReqVO.setTaskId(existing.getTaskId());
        updateReqVO.setSpaceId(existing.getSpaceId()); updateReqVO.setSpaceCode(existing.getSpaceCode());
        updateReqVO.setDsId(existing.getDsId());
        // 相关校验

        // 更新数据集成任务实例
        CollectorEtlTaskInstanceDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskInstanceDO.class);
        return CollectorEtlTaskInstanceMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlTaskInstance(Collection<Long> idList) {
        for (Long id : idList) {
            CollectorEtlTaskInstanceDO instance = requireInstance(id);
            if (instance.getRunOwnerToken() != null) throw new ServiceException("组件运行记录承载阶段水位与重放历史，暂不允许单独删除");
        }
        return CollectorEtlTaskInstanceMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlTaskInstanceDO getCollectorEtlTaskInstanceById(Long id) {
        return requireInstance(id);
    }

    @Override
    public List<CollectorEtlTaskInstanceDO> getCollectorEtlTaskInstanceList() {
        return CollectorEtlTaskInstanceMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlTaskInstanceDO> getCollectorEtlTaskInstanceMap() {
        List<CollectorEtlTaskInstanceDO> CollectorEtlTaskInstanceList = CollectorEtlTaskInstanceMapper.selectList();
        return CollectorEtlTaskInstanceList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlTaskInstanceDO::getId,
                        CollectorEtlTaskInstanceDO -> CollectorEtlTaskInstanceDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成任务实例数据
     *
     * @param importExcelList 数据集成任务实例数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCollectorEtlTaskInstance(List<CollectorEtlTaskInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (!com.datamaster.common.utils.SecurityUtils.isAdmin(com.datamaster.common.utils.SecurityUtils.getUserId())) {
            throw new ServiceException("运行实例导入仅允许管理员操作", 403);
        }
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEtlTaskInstanceRespVO respVO : importExcelList) {
            try {
                CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = BeanUtils.toBean(respVO, CollectorEtlTaskInstanceDO.class);
                Long CollectorEtlTaskInstanceId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEtlTaskInstanceId != null) {
                        CollectorEtlTaskInstanceDO existingCollectorEtlTaskInstance = CollectorEtlTaskInstanceMapper.selectById(CollectorEtlTaskInstanceId);
                        if (existingCollectorEtlTaskInstance != null) {
                            if (existingCollectorEtlTaskInstance.getRunOwnerToken() != null) throw new ServiceException("组件运行记录不允许导入覆盖");
                            CollectorEtlTaskInstanceMapper.updateById(CollectorEtlTaskInstanceDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEtlTaskInstanceId + " 的数据集成任务实例记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEtlTaskInstanceId + " 的数据集成任务实例记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEtlTaskInstanceDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEtlTaskInstanceId);
                    CollectorEtlTaskInstanceDO existingCollectorEtlTaskInstance = CollectorEtlTaskInstanceMapper.selectOne(queryWrapper);
                    if (existingCollectorEtlTaskInstance == null) {
                        CollectorEtlTaskInstanceMapper.insert(CollectorEtlTaskInstanceDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEtlTaskInstanceId + " 的数据集成任务实例记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEtlTaskInstanceId + " 的数据集成任务实例记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }

    @Override
    public Boolean createTaskInstance(ProcessInstance processInstance) {
        log.info(JSONObject.toJSONString(processInstance));
        CollectorEtlTaskRespDTO CollectorEtlTaskRespDTO = CollectorEtlTaskService.getTaskByTaskCode(String.valueOf(processInstance.getProcessDefinitionCode()));
        if (CollectorEtlTaskRespDTO == null) {
            return true;
        }
        CollectorEtlTaskInstanceDO collectorEtlTaskInstanceDO = CollectorEtlTaskInstanceDO.builder()
                .id(processInstance.getId())
                .catId(CollectorEtlTaskRespDTO.getCatId())
                .catCode(CollectorEtlTaskRespDTO.getCatCode())
                .taskType(CollectorEtlTaskRespDTO.getType())
                .name(processInstance.getName())
                .taskId(CollectorEtlTaskRespDTO.getId())
                .taskCode(String.valueOf(processInstance.getProcessDefinitionCode()))
                .taskVersion(processInstance.getProcessDefinitionVersion())
                .spaceId(attProjectApi.getSpaceIdBySpaceCode(String.valueOf(processInstance.getProjectCode())))
                .spaceCode(String.valueOf(processInstance.getProjectCode()))
                .scheduleTime(processInstance.getCommandStartTime())
                .startTime(processInstance.getStartTime())
                .endTime(processInstance.getEndTime())
                .runTimes(processInstance.getRunTimes())
                .commandType(String.valueOf(processInstance.getCommandType().getCode()))
                .maxTryTimes(processInstance.getMaxTryTimes())
                .failureStrategy(String.valueOf(processInstance.getFailureStrategy().getCode()))
                .subTaskFlag(String.valueOf(processInstance.getIsSubProcess().getCode()))
                .status(String.valueOf(processInstance.getState().getCode()))
                .statusHistory(processInstance.getStateHistory())
                .personCharge(CollectorEtlTaskRespDTO.getPersonCharge())
                .contactNumber(CollectorEtlTaskRespDTO.getContactNumber())
                .dsId(processInstance.getId())
                .build();
        if (processInstance.getIsSubProcess().getCode() == Flag.YES.getCode() && StringUtils.isNotEmpty(processInstance.getCommandParam())) {
            JSONObject commandParam = JSONObject.parseObject(processInstance.getCommandParam());
            if (commandParam.containsKey("parentProcessInstanceId")) {
                collectorEtlTaskInstanceDO.setParentTaskInstanceId(commandParam.getLong("parentProcessInstanceId"));
            }
            if (commandParam.containsKey("parentTaskInstanceId")) {
                collectorEtlTaskInstanceDO.setParentNodeInstanceId(commandParam.getLong("parentTaskInstanceId"));
            }
        }
        boolean saved = this.save(collectorEtlTaskInstanceDO);
        triggerOpsIfFinished(processInstance, collectorEtlTaskInstanceDO);
        return saved;
    }

    @Override
    public Boolean updateTaskInstance(ProcessInstance processInstance) {
        log.info(JSONObject.toJSONString(processInstance));
        CollectorEtlTaskInstanceDO old = this.getByDsId(processInstance.getId());
        if (old == null) {
            return true;
        }
        Date endTime = processInstance.getEndTime();
        if (endTime == null && processInstance.getState() != null && processInstance.getState().isFinished()) {
            endTime = new Date();
        }
        CollectorEtlTaskInstanceDO collectorEtlTaskInstanceDO = CollectorEtlTaskInstanceDO.builder()
                .id(old.getId())
                .scheduleTime(processInstance.getCommandStartTime())
                .startTime(processInstance.getStartTime())
                .endTime(endTime)
                .status(String.valueOf(processInstance.getState().getCode()))
                .statusHistory(processInstance.getStateHistory())
                .subTaskFlag(String.valueOf(processInstance.getIsSubProcess().getCode()))
                .runTimes(processInstance.getRunTimes())
                .commandType(processInstance.getCommandType() != null ? String.valueOf(processInstance.getCommandType().getCode()) : null)
                .build();
        if (processInstance.getIsSubProcess().getCode() == Flag.YES.getCode() && StringUtils.isNotEmpty(processInstance.getCommandParam())) {
            JSONObject commandParam = JSONObject.parseObject(processInstance.getCommandParam());
            if (commandParam.containsKey("parentProcessInstanceId")) {
                collectorEtlTaskInstanceDO.setParentTaskInstanceId(commandParam.getLong("parentProcessInstanceId"));
            }
            if (commandParam.containsKey("parentTaskInstanceId")) {
                collectorEtlTaskInstanceDO.setParentNodeInstanceId(commandParam.getLong("parentTaskInstanceId"));
            }
        }
        boolean saved = this.saveOrUpdate(collectorEtlTaskInstanceDO);
        old.setStatus(collectorEtlTaskInstanceDO.getStatus());
        old.setStatusHistory(collectorEtlTaskInstanceDO.getStatusHistory());
        old.setEndTime(collectorEtlTaskInstanceDO.getEndTime());
        old.setRunTimes(collectorEtlTaskInstanceDO.getRunTimes());
        // 推送任务状态到前端 WebSocket
        try {
            collectorEtlTaskStatusPushService.pushTaskInstanceStatus(collectorEtlTaskInstanceDO);
        } catch (Exception e) {
            log.warn("推送ETL任务状态WebSocket消息失败，instanceId={}", old.getId(), e);
        }
        triggerOpsIfFinished(processInstance, old);
        return saved;
    }

    private void triggerOpsIfFinished(ProcessInstance processInstance, CollectorEtlTaskInstanceDO instance) {
        log.info("triggerOpsIfFinished: 开始调用，processInstanceId={}, state={}", 
            processInstance != null ? processInstance.getId() : "null",
            processInstance != null && processInstance.getState() != null ? processInstance.getState() : "null");
        try {
            collectorEtlTaskOpsService.handleProcessInstanceFinished(processInstance, instance);
            log.info("triggerOpsIfFinished: 调用完成");
        } catch (Exception e) {
            log.warn("任务运维托管处理异常，processInstanceId={}",
                    processInstance == null ? null : processInstance.getId(), e);
        }
    }

    @Override
    public CollectorEtlTaskInstanceDO getByDsId(Long dsId) {
        return baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlTaskInstanceDO.class)
                .eq(CollectorEtlTaskInstanceDO::getDsId, dsId));
    }

    @Override
    public Long getIdByDsId(Long dsId) {
        CollectorEtlTaskInstanceDO collectorEtlTaskInstanceDO = baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlTaskInstanceDO.class)
                .eq(CollectorEtlTaskInstanceDO::getDsId, dsId));
        if (collectorEtlTaskInstanceDO != null) {
            return collectorEtlTaskInstanceDO.getId();
        }
        return null;
    }

    @Override
    public PageResult<CollectorEtlTaskInstanceTreeListRespVO> treeList(CollectorEtlTaskInstanceTreeListReqVO reqVO) {
        requireSpace(null, reqVO.getSpaceCode());
        if (StringUtils.isBlank(reqVO.getTaskType())) {
            reqVO.setTaskType("1");
        }
        if (StringUtils.isNotEmpty(reqVO.getStartTime())) {
            reqVO.setStartTime(reqVO.getStartTime() + " 00:00:00");
        }
        if (StringUtils.isNotEmpty(reqVO.getEndTime())) {
            reqVO.setEndTime(reqVO.getEndTime() + " 23:59:59");
        }
        IPage<CollectorEtlTaskInstanceTreeListRespVO> page = baseMapper.treeList(new Page(reqVO.getPageNum(), reqVO.getPageSize()), reqVO);
        if (page != null && page.getRecords() != null && page.getRecords().size() > 0) {
            for (CollectorEtlTaskInstanceTreeListRespVO record : page.getRecords()) {
                record.setDataId("1_" + record.getId());
                if (record.getStartTime() != null && record.getEndTime() != null) {
                    record.setDuration(DateUtils.format2Duration(record.getEndTime().getTime() - record.getStartTime().getTime()));
                }
                if (record.getChildren() != null && record.getChildren().size() > 0) {
                    for (CollectorEtlTaskInstanceTreeListRespVO child : record.getChildren()) {
                        child.setDataId(child.getDataType() + "_" + child.getId());
                        if (child.getStartTime() != null && child.getEndTime() != null) {
                            child.setDuration(DateUtils.format2Duration(child.getEndTime().getTime() - child.getStartTime().getTime()));
                        }
                        //判断是否是子任务
                        if (StringUtils.equals(String.valueOf(TaskComponentTypeEnum.SUB_PROCESS), child.getNodeType())) {
                            child.setHasChildren(true);
                        }
                    }

                }
            }
        }
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public AjaxResult execute(Long taskInstanceId, ExecuteType executeType) {
        if (executeType == null || ExecuteType.NONE == executeType) return error("无效的运行控制命令");
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = requireInstance(taskInstanceId);
        if (CollectorEtlTaskInstanceDO == null) {
            return error("任务实例不存在，请刷新后重试！");
        }
        String status = CollectorEtlTaskInstanceDO.getStatus();
        boolean componentRuntime = isComponentRuntimeInstance(CollectorEtlTaskInstanceDO);
        if (ExecuteType.REPEAT_RUNNING.getCode() == executeType.getCode() && !StringUtils.equals(status, "3") &&
                !StringUtils.equals(status, "5") &&
                !StringUtils.equals(status, "6") &&
                !StringUtils.equals(status, "7")) {
            return error("当前状态无法重跑，请刷新后重试！");
        }
        if (ExecuteType.STOP.getCode() == executeType.getCode() &&
                !StringUtils.equals(status, "0") &&
                !StringUtils.equals(status, "1") &&
                !StringUtils.equals(status, "2") &&
                !StringUtils.equals(status, "3") &&
                !StringUtils.equals(status, "12") &&
                !StringUtils.equals(status, "14") &&
                !(componentRuntime && Arrays.asList("4", "15", "16", "17").contains(status))) {
            return error("当前状态无法停止，请刷新后重试！");
        }
        if (componentRuntime && (ExecuteType.START_FAILURE_TASK_PROCESS == executeType || ExecuteType.EXECUTE_TASK == executeType)) {
            return error("组件运行不允许跳过前置准入执行单节点或恢复失败节点，请重跑整个任务以创建新实例");
        }
        if (componentRuntime && ExecuteType.RECOVER_SUSPENDED_PROCESS == executeType) {
            if (CollectorEtlTaskInstanceDO.getDsId() == null) return error("组件实例缺少DS运行身份，不能恢复");
            DsStatusRespDTO response = componentRunGuard.resumePaused(CollectorEtlTaskInstanceDO.getTaskId(), CollectorEtlTaskInstanceDO.getDsId());
            return Boolean.TRUE.equals(response == null ? null : response.getSuccess())
                    ? success() : error(response == null ? "DolphinScheduler无响应" : response.getMsg());
        }
        if (componentRuntime && ExecuteType.REPEAT_RUNNING.getCode() == executeType.getCode()) {
            if ("3".equals(status)) return error("暂停实例请恢复或确认停止后再重跑，暂停期间仍保留运行资格");
            // DS原实例重跑可能复用instanceId且跳过根prepare，不能越过已结束的owner/窗口。
            // 启动新的大任务实例保留原历史，失败窗口由新实例在prepare中安全接管。
            return CollectorEtlTaskService.startCollectorEtlTask(CollectorEtlTaskInstanceDO.getTaskId());
        }
        DsStatusRespDTO dsStatusRespDTO = dsEtlExecutorService.execute(DSExecuteDTO.builder()
                .processInstanceId(CollectorEtlTaskInstanceDO.getDsId() == null ? taskInstanceId : CollectorEtlTaskInstanceDO.getDsId())
                .executeType(executeType)
                .build(), CollectorEtlTaskInstanceDO.getSpaceCode());
        return Boolean.TRUE.equals(dsStatusRespDTO == null ? null : dsStatusRespDTO.getSuccess())
                ? success() : error(dsStatusRespDTO == null ? "DolphinScheduler无响应" : dsStatusRespDTO.getMsg());
    }

    private boolean isComponentRuntimeInstance(CollectorEtlTaskInstanceDO instance) {
        com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO ext = ownerTaskExtService.getByTaskId(instance.getTaskId());
        if (ext == null || StringUtils.isBlank(ext.getFlinkxJobTemplateJson())) return false;
        JSONObject snapshot = JSONObject.parseObject(ext.getFlinkxJobTemplateJson());
        return CollectorEtlStageRuntimeService.KIND.equals(snapshot.getString("kind"))
                && StringUtils.isNotBlank(snapshot.getString("guardNodeCode"))
                && Objects.equals(instance.getTaskCode(), snapshot.getString("workflowCode"));
    }

    @Override
    public List<CollectorEtlTaskInstanceTreeListRespVO> subNodelist(Long taskInstanceId, Long nodeInstanceId) {
        requireInstance(taskInstanceId);
        CollectorEtlNodeInstanceDO parentNode = CollectorEtlTNodeInstanceService.getById(nodeInstanceId);
        if (parentNode == null || !Objects.equals(taskInstanceId, parentNode.getTaskInstanceId())) {
            throw new ServiceException("子任务节点不属于该流程实例");
        }
        List<CollectorEtlTaskInstanceTreeListRespVO> list = baseMapper.listSubNodeInstance(taskInstanceId, nodeInstanceId);
        if (list != null && list.size() > 0) {
            list.stream().forEach(e -> {
                e.setDataId("3_" + e.getId());
                if (e.getStartTime() != null && e.getEndTime() != null) {
                    e.setDuration(DateUtils.format2Duration(e.getEndTime().getTime() - e.getStartTime().getTime()));
                }
            });
        }
        return list;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CollectorEtlTaskInstanceLogStatusRespDTO getLogByTaskInstanceId(Long taskInstanceId) {
        return getLogByTaskInstanceId(taskInstanceId, null, null);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CollectorEtlTaskInstanceLogStatusRespDTO getLogByTaskInstanceId(Long taskInstanceId, Integer skipLineNum, Integer limit) {
        int fromLineNum = Math.max(skipLineNum == null ? 0 : skipLineNum, 0);
        int safeLimit = limit == null || limit <= 0 ? 2000 : Math.min(limit, 10000);
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = requireInstance(taskInstanceId);
        //获取节点数据
        List<CollectorEtlNodeInstanceDO> CollectorEtlNodeInstanceDOList = CollectorEtlTNodeInstanceService.list(Wrappers.lambdaQuery(CollectorEtlNodeInstanceDO.class)
                .select(CollectorEtlNodeInstanceDO::getId,
                        CollectorEtlNodeInstanceDO::getDsId,
                        CollectorEtlNodeInstanceDO::getNodeCode,
                        CollectorEtlNodeInstanceDO::getName,
                        CollectorEtlNodeInstanceDO::getStatus)
                .eq(CollectorEtlNodeInstanceDO::getTaskInstanceId, taskInstanceId));
        LogResult logResult = collectDsLogSlice(CollectorEtlTaskInstanceDO, CollectorEtlNodeInstanceDOList, fromLineNum, safeLimit);
        if (logResult == null) {
            String log = collectNodeLogs(taskInstanceId, CollectorEtlNodeInstanceDOList);
            logResult = sliceLog(log, fromLineNum, safeLimit);
        }

        return CollectorEtlTaskInstanceLogStatusRespDTO.builder()
                .log(logResult.getLogContent())
                .logContent(logResult.getLogContent())
                .fromLineNum(logResult.getFromLineNum())
                .toLineNum(logResult.getToLineNum())
                .isEnd(logResult.isEnd())
                .status(CollectorEtlTaskInstanceDO.getStatus())
                .nodeInstanceList(BeanUtils.toBean(CollectorEtlNodeInstanceDOList, CollectorEtlNodeInstanceRespDTO.class))
                .build();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String downloadLogByTaskInstanceId(Long taskInstanceId) {
        CollectorEtlTaskInstanceDO taskInstance = requireInstance(taskInstanceId);
        List<CollectorEtlNodeInstanceDO> nodeInstances = CollectorEtlTNodeInstanceService.list(
                Wrappers.lambdaQuery(CollectorEtlNodeInstanceDO.class)
                        .select(CollectorEtlNodeInstanceDO::getId,
                                CollectorEtlNodeInstanceDO::getDsId,
                                CollectorEtlNodeInstanceDO::getName,
                                CollectorEtlNodeInstanceDO::getNodeCode)
                        .eq(CollectorEtlNodeInstanceDO::getTaskInstanceId, taskInstanceId));
        String log = collectDsLogs(taskInstance, nodeInstances, true);
        if (StringUtils.isBlank(log)) {
            log = collectNodeLogs(taskInstanceId, nodeInstances);
        }
        return log;
    }

    private String collectDsLogs(CollectorEtlTaskInstanceDO taskInstance, List<CollectorEtlNodeInstanceDO> nodeInstanceList, boolean download) {
        List<Long> dsTaskInstanceIds = resolveDsTaskInstanceIds(taskInstance, nodeInstanceList);
        if (dsTaskInstanceIds.isEmpty()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        for (Long dsTaskInstanceId : dsTaskInstanceIds) {
            try {
                String nodeLog = download
                        ? dsEtlTaskService.downloadTaskInstanceLog(dsTaskInstanceId)
                        : dsEtlTaskService.getTaskInstanceLog(dsTaskInstanceId);
                if (StringUtils.isNotBlank(nodeLog)) {
                    if (content.length() > 0) {
                        content.append('\n');
                    }
                    content.append(nodeLog);
                }
            } catch (Exception e) {
                log.warn("读取DolphinScheduler任务实例日志失败，dsTaskInstanceId={}", dsTaskInstanceId, e);
            }
        }
        return content.toString();
    }

    private LogResult collectDsLogSlice(CollectorEtlTaskInstanceDO taskInstance, List<CollectorEtlNodeInstanceDO> nodeInstanceList, int skipLineNum, int limit) {
        List<Long> dsTaskInstanceIds = resolveDsTaskInstanceIds(taskInstance, nodeInstanceList);
        if (dsTaskInstanceIds.isEmpty()) {
            return null;
        }
        if (dsTaskInstanceIds.size() == 1) {
            Long dsTaskInstanceId = dsTaskInstanceIds.get(0);
            try {
                DsLogResultDTO dsLog = dsEtlTaskService.getTaskInstanceLog(dsTaskInstanceId, skipLineNum, limit);
                if (dsLog != null) {
                    return new LogResult(
                            dsLog.getFromLineNum() == null ? skipLineNum : dsLog.getFromLineNum(),
                            dsLog.getToLineNum() == null ? skipLineNum : dsLog.getToLineNum(),
                            dsLog.getLogContent() == null ? "" : dsLog.getLogContent(),
                            Boolean.TRUE.equals(dsLog.getEnd()));
                }
            } catch (Exception e) {
                log.warn("分段读取DolphinScheduler任务实例日志失败，dsTaskInstanceId={}", dsTaskInstanceId, e);
            }
            return null;
        }

        String log = collectDsLogs(taskInstance, nodeInstanceList, false);
        return sliceLog(log, skipLineNum, limit);
    }

    private LogResult sliceLog(String logContent, int skipLineNum, int limit) {
        String content = logContent == null ? "" : logContent;
        String[] lines = content.split("\\r?\\n", -1);
        int total = StringUtils.isBlank(content) ? 0 : lines.length;
        if (skipLineNum >= total) {
            return new LogResult(skipLineNum, skipLineNum, "", true);
        }
        int toLineNum = Math.min(skipLineNum + limit, total);
        StringBuilder part = new StringBuilder();
        for (int i = skipLineNum; i < toLineNum; i++) {
            if (part.length() > 0) {
                part.append('\n');
            }
            part.append(lines[i]);
        }
        return new LogResult(skipLineNum, toLineNum, part.toString(), toLineNum >= total);
    }

    private List<Long> resolveDsTaskInstanceIds(CollectorEtlTaskInstanceDO taskInstance, List<CollectorEtlNodeInstanceDO> nodeInstanceList) {
        List<Long> ids = new ArrayList<>();
        if (nodeInstanceList != null) {
            ids.addAll(nodeInstanceList.stream()
                    .map(CollectorEtlNodeInstanceDO::getDsId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()));
        }
        if (!ids.isEmpty() || taskInstance == null || StringUtils.isBlank(taskInstance.getSpaceCode())) {
            return ids;
        }
        Long dsProcessInstanceId = taskInstance.getDsId() == null ? taskInstance.getId() : taskInstance.getDsId();
        List<TaskInstance> dsTaskInstances = dsEtlTaskService.listTaskInstances(taskInstance.getSpaceCode(), dsProcessInstanceId);
        if (dsTaskInstances == null || dsTaskInstances.isEmpty()) {
            return ids;
        }
        return dsTaskInstances.stream()
                .map(TaskInstance::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String collectNodeLogs(Long taskInstanceId, List<CollectorEtlNodeInstanceDO> nodeInstanceList) {
        StringBuilder content = new StringBuilder();
        if (nodeInstanceList != null && !nodeInstanceList.isEmpty()) {
            for (CollectorEtlNodeInstanceDO nodeInstance : nodeInstanceList) {
                String nodeLog = getNodeLog(nodeInstance);
                if (StringUtils.isNotBlank(nodeLog)) {
                    content.append(nodeLog).append('\n');
                }
            }
            return content.toString();
        }
        List<CollectorEtlNodeInstanceLogDO> logs = CollectorEtlNodeInstanceLogService.list(
                Wrappers.lambdaQuery(CollectorEtlNodeInstanceLogDO.class)
                        .eq(CollectorEtlNodeInstanceLogDO::getTaskInstanceId, taskInstanceId)
                        .orderByAsc(CollectorEtlNodeInstanceLogDO::getTm));
        if (logs == null || logs.isEmpty()) {
            return "";
        }
        return logs.stream()
                .map(CollectorEtlNodeInstanceLogDO::getLogContent)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));
    }

    private String getNodeLog(CollectorEtlNodeInstanceDO nodeInstance) {
        if (nodeInstance == null || nodeInstance.getId() == null) {
            return null;
        }
        return CollectorEtlNodeInstanceLogService.getLog(nodeInstance.getId());
    }

    @Override
    public Long getRunTaskInstance(Long taskId) {
        requireOwnerTask(taskId);
        List<CollectorEtlTaskInstanceDO> collectorEtlTaskInstanceDOS = this.list(Wrappers.lambdaQuery(CollectorEtlTaskInstanceDO.class)
                .eq(CollectorEtlTaskInstanceDO::getTaskId, taskId)
                .in(CollectorEtlTaskInstanceDO::getStatus, "0", "1", "12")
                .orderByDesc(CollectorEtlTaskInstanceDO::getStartTime));
        if (collectorEtlTaskInstanceDOS.size() > 0) {
            return collectorEtlTaskInstanceDOS.get(0).getId();
        }
        return null;
    }

    @Override
    public CollectorEtlTaskUpdateQueryRespVO getTaskInfo(Long id) {
        requireInstance(id);
        //根据任务实例id获取任务信息
        MPJLambdaWrapper<CollectorEtlTaskInstanceDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(CollectorEtlTaskInstanceDO.class)
                .select("t3.NICK_NAME AS personChargeName")
                .leftJoin("SYSTEM_USER t3 on t.PERSON_CHARGE = " + ("mysql".equals(MasterDataSourceConfig.getDatabaseType()) ? "CAST(t3.USER_ID AS CHAR)" : "CAST(t3.USER_ID AS VARCHAR)") + " AND t3.DEL_FLAG = '0'")
                .eq(CollectorEtlTaskInstanceDO::getId, id);
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlTaskInstanceMapper.selectJoinOne(CollectorEtlTaskInstanceDO.class, lambdaWrapper);

        //获取任务信息
        CollectorEtlTaskLogRespVO CollectorEtlTaskLogRespVO = CollectorEtlTaskLogService.getCollectorEtlTaskLogById(CollectorEtlTaskLogPageReqVO.builder()
                .code(CollectorEtlTaskInstanceDO.getTaskCode())
                .version(CollectorEtlTaskInstanceDO.getTaskVersion())
                .build());
        if (CollectorEtlTaskLogRespVO == null) {
            throw new RuntimeException("任务不存在");
        }
        CollectorEtlTaskUpdateQueryRespVO bean = new CollectorEtlTaskUpdateQueryRespVO(BeanUtils.toBean(CollectorEtlTaskLogRespVO, CollectorEtlTaskDO.class));
        bean.setTaskInstance(CollectorEtlTaskInstanceDO);
        //获取关系数据
        List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList = iCollectorEtlTaskNodeRelService.getCollectorEtlTaskNodeRelRespVOList(CollectorEtlTaskNodeRelPageReqVO.builder()
                .taskCode(bean.getCode())
                .taskVersion(bean.getVersion())
                .build());
        bean.setTaskRelationJsonFromNodeRelList(CollectorEtlTaskNodeRelRespVOList);

        //获取捷信信息
        List<CollectorEtlNodeLogDO> CollectorEtlNodeLogDOList = CollectorEtlNodeLogService.listByTaskCode(CollectorEtlTaskInstanceDO.getTaskCode(), CollectorEtlTaskInstanceDO.getTaskVersion());
        bean.setTaskDefinitionList(BeanUtils.toBean(CollectorEtlNodeLogDOList, CollectorEtlNodeRespVO.class));
        bean.createTaskConfig();
        return bean;
    }

    @Override
    public CollectorEtlTaskInstanceDO getLastTaskInstanceByTaskCode(String code) {
        IPage<CollectorEtlTaskInstanceDO> page = this.page(new Page(1, 1), Wrappers.lambdaQuery(CollectorEtlTaskInstanceDO.class)
                .eq(CollectorEtlTaskInstanceDO::getTaskCode, code)
                .orderByDesc(CollectorEtlTaskInstanceDO::getStartTime));
        if (page.getRecords().size() > 0) {
            return page.getRecords().get(0);
        }
        return null;
    }
    @Override
    public List<CollectorEtlTaskInstanceRespDTO> getLastTaskInstance(List<Long> taskIdList) {
        List<CollectorEtlTaskInstanceDO> CollectorEtlTaskInstanceDO = CollectorEtlTaskInstanceMapper.getLastTaskInstance(taskIdList);
        return BeanUtils.toBean(CollectorEtlTaskInstanceDO, CollectorEtlTaskInstanceRespDTO.class);
    }

}
