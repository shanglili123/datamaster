package com.datamaster.metadata.service.discovery.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.*;
import com.datamaster.api.ds.api.etl.ds.ProcessDefinition;
import com.datamaster.api.ds.api.etl.ds.Schedule;
import com.datamaster.api.ds.api.etl.ds.TaskDefinition;
import com.datamaster.api.ds.api.service.etl.IDsEtlNodeService;
import com.datamaster.api.ds.api.service.etl.IDsEtlSchedulerService;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskRespVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskSaveReqVO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.metadata.dal.mapper.discovery.AssetsDiscoveryTaskMapper;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTableService;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTaskService;
import com.datamaster.metadata.utils.AssetsTaskConverter;
import com.datamaster.metadata.utils.model.TaskSaveReqInput;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datamaster.common.core.domain.AjaxResult.error;
import static com.datamaster.common.core.domain.AjaxResult.success;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsDiscoveryTaskServiceImpl extends ServiceImpl<AssetsDiscoveryTaskMapper, AssetsDiscoveryTaskDO> implements IAssetsDiscoveryTaskService {

    @Resource
    private IDatasourceApiService datasourceApiService;

    @Resource
    private IDsEtlSchedulerService iDsEtlSchedulerService;

    @Resource
    private AssetsDiscoveryTaskMapper AssetsDiscoveryTaskMapper;
    @Autowired
    @Lazy
    private IAssetsDiscoveryTableService IAssetsDiscoveryTableService;

    @Resource
    private IDsEtlNodeService dsEtlNodeService;
    @Resource
    private IDsEtlTaskService dsEtlTaskService;
    @Override
    public PageResult<AssetsDiscoveryTaskDO> getDaDiscoveryTaskPage(AssetsDiscoveryTaskPageReqVO pageReqVO) {
        return AssetsDiscoveryTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AssetsDiscoveryTaskRespVO> getDaDiscoveryTaskListPage(AssetsDiscoveryTaskPageReqVO pageReqVO) {
        PageResult<AssetsDiscoveryTaskDO> AssetsDiscoveryTaskDOPageResult = AssetsDiscoveryTaskMapper.selectPage(pageReqVO);
        PageResult<AssetsDiscoveryTaskRespVO> pageResult = BeanUtils.toBean(AssetsDiscoveryTaskDOPageResult, AssetsDiscoveryTaskRespVO.class);
        List<AssetsDiscoveryTaskRespVO> rows = (List<AssetsDiscoveryTaskRespVO>) pageResult.getRows();
        if (CollectionUtils.isEmpty(rows)) {
            return pageResult;
        }
        List<Long> datasourceIds = rows.stream()
                .map(AssetsDiscoveryTaskRespVO::getDatasourceId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DatasourceRespDTO> datasourceMap = datasourceApiService.getDatabaseListByIds(datasourceIds).stream()
                .collect(Collectors.toMap(DatasourceRespDTO::getId, item -> item, (left, right) -> left));
        for (AssetsDiscoveryTaskRespVO row : rows) {
            DatasourceRespDTO AssetsDatasourceById = this.getDatasourceById(row.getDatasourceId(), datasourceMap);
            AssetsDatasourceById = AssetsDatasourceById != null ? AssetsDatasourceById : new DatasourceRespDTO();
            row.setDatasourceName(AssetsDatasourceById.getDatasourceName());
            row.setDatasourceType(AssetsDatasourceById.getDatasourceType());

            //定时任务封装相关

        }
        pageResult.setRows(rows);
        return pageResult;
    }

    private DatasourceRespDTO getDatasourceById(Long datasourceId, Map<Long, DatasourceRespDTO> datasourceMap) {
        if (datasourceId == null || datasourceMap == null || datasourceMap.isEmpty()) {
            return new DatasourceRespDTO();
        }
        return datasourceMap.getOrDefault(datasourceId, new DatasourceRespDTO());
    }

    @Override
    public Long createDaDiscoveryTask(AssetsDiscoveryTaskSaveReqVO createReqVO) {
        AssetsDiscoveryTaskDO dictType = BeanUtils.toBean(createReqVO, AssetsDiscoveryTaskDO.class);
        MPJLambdaWrapper<AssetsDiscoveryTaskDO> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.eq(AssetsDiscoveryTaskDO::getName, createReqVO.getName());
        Long count = AssetsDiscoveryTaskMapper.selectCount(mpjLambdaWrapper);
        if (count != null && count > 0) {
            throw new RuntimeException("");
        }

        //TODO 存储数据，保证测试，对接任务时删除
        dictType.setNodeCode("0");
        dictType.setNodeId(0L);
        dictType.setTaskId(0L);
        dictType.setTaskCode("0");
        AssetsDiscoveryTaskMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaDiscoveryTask(AssetsDiscoveryTaskSaveReqVO updateReqVO) {
//        // 相关校验

        MPJLambdaWrapper<AssetsDiscoveryTaskDO> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.eq(AssetsDiscoveryTaskDO::getName, updateReqVO.getName());
        mpjLambdaWrapper.ne(AssetsDiscoveryTaskDO::getId, updateReqVO.getId());
        Long count = AssetsDiscoveryTaskMapper.selectCount(mpjLambdaWrapper);
        if (count != null && count > 0) {
            throw new RuntimeException("");
        }

        // 更新数据发现任务
        AssetsDiscoveryTaskDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDiscoveryTaskDO.class);
        AssetsDiscoveryTaskDO AssetsDiscoveryTaskDO = AssetsDiscoveryTaskMapper.selectById(updateReqVO.getId());
        if (StringUtils.equals(AssetsDiscoveryTaskDO.getCronExpression(), updateReqVO.getCronExpression())) {
            this.updateDaDiscoveryTaskCronExpression(updateReqVO);
            return 1;
        }

        return AssetsDiscoveryTaskMapper.updateById(updateObj);
    }

    @Override
    public boolean runDaDiscoveryTask(Long taskId) {
        datasourceApiService.detectTableSchemaUpdates(taskId);
        return true;
    }

    @Override
    public int updateDaDiscoveryTask(AssetsDiscoveryTaskRespVO updateReqVO) {
        // 相关校验

        // 更新数据发现任务
        AssetsDiscoveryTaskDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDiscoveryTaskDO.class);
        return AssetsDiscoveryTaskMapper.updateById(updateObj);
    }

    @Override
    public int removeDaDiscoveryTask(Collection<Long> idList) {
        // 遍历 idList 中的每个 id
        for (Long id : idList) {
            // 查询 AssetsDiscoveryTaskDO 详情
            AssetsDiscoveryTaskDO AssetsDiscoveryTaskDO = AssetsDiscoveryTaskMapper.selectById(id);
            if (AssetsDiscoveryTaskDO != null &&
                    (AssetsDiscoveryTaskDO.getSystemJobId() != null || !StringUtils.equals("0", AssetsDiscoveryTaskDO.getTaskCode()))) {
                // 提取 systemJobId
                if (StringUtils.equals("0", AssetsDiscoveryTaskDO.getStatus())) {
                    throw new ServiceException("");
                }
                String spaceCode = resolveSpaceCode(AssetsDiscoveryTaskDO.getDatasourceId());
                DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.deleteTask(spaceCode, AssetsDiscoveryTaskDO.getTaskCode());
            }
        }

        // 批量删除数据发现任务
        return AssetsDiscoveryTaskMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDiscoveryTaskRespVO getDaDiscoveryTaskById(Long id) {

        MPJLambdaWrapper<AssetsDiscoveryTaskDO> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.selectAll(AssetsDiscoveryTaskDO.class)
                .select("t2.name AS catName")
                .leftJoin("TAX_CATEGORY t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0' AND t2.CAT_TYPE = 'TASK'")
                .eq(AssetsDiscoveryTaskDO::getId, id);
        AssetsDiscoveryTaskDO AssetsDiscoveryTaskDO = AssetsDiscoveryTaskMapper.selectJoinOne(AssetsDiscoveryTaskDO.class, mpjLambdaWrapper);

        AssetsDiscoveryTaskRespVO bean = BeanUtils.toBean(AssetsDiscoveryTaskDO, AssetsDiscoveryTaskRespVO.class);

        DatasourceRespDTO AssetsDatasourceById = datasourceApiService.getDatasourceById(bean.getDatasourceId());
        AssetsDatasourceById = AssetsDatasourceById == null ? new DatasourceRespDTO() : AssetsDatasourceById;
        bean.setDatasourceName(AssetsDatasourceById.getDatasourceName());
        bean.setDatasourceType(AssetsDatasourceById.getDatasourceType());
        bean.setIp(AssetsDatasourceById.getIp());

        List<AssetsDiscoveryTableDO> AssetsDiscoveryTableDOList = fetchDiscoveryTableList(bean);
        AssetsDiscoveryTableDOList = AssetsDiscoveryTableDOList == null ? new ArrayList<>() : AssetsDiscoveryTableDOList;

        long countPending = AssetsDiscoveryTableDOList.stream()
                .filter(item -> StringUtils.equals("1", item.getStatus()))
                .count();

        long countSubmitted = AssetsDiscoveryTableDOList.stream()
                .filter(item -> StringUtils.equals("2", item.getStatus()))
                .count();

        //0:否，1：是
        long countIgnoreFlag = AssetsDiscoveryTableDOList.stream()
                .filter(item -> StringUtils.equals("1", item.getIgnoreFlag()))
                .count();
        bean.setCountPending(countPending);
        bean.setCountSubmitted(countSubmitted);
        bean.setCountIgnoreFlag(countIgnoreFlag);

        return bean;
    }

    private List<AssetsDiscoveryTableDO> fetchDiscoveryTableList(AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskDO) {
        AssetsDiscoveryTablePageReqVO AssetsDiscoveryTablePageReqVO = new AssetsDiscoveryTablePageReqVO();
        AssetsDiscoveryTablePageReqVO.setTaskId(AssetsDiscoveryTaskDO.getId());

        return IAssetsDiscoveryTableService.getDaDiscoveryTableList(AssetsDiscoveryTablePageReqVO);
    }

    @Override
    public List<AssetsDiscoveryTaskDO> getDaDiscoveryTaskList() {
        return AssetsDiscoveryTaskMapper.selectList();
    }

    @Override
    public Map<Long, AssetsDiscoveryTaskDO> getDaDiscoveryTaskMap() {
        List<AssetsDiscoveryTaskDO> AssetsDiscoveryTaskList = AssetsDiscoveryTaskMapper.selectList();
        return AssetsDiscoveryTaskList.stream()
                .collect(Collectors.toMap(
                        AssetsDiscoveryTaskDO::getId,
                        AssetsDiscoveryTaskDO -> AssetsDiscoveryTaskDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importDaDiscoveryTask(List<AssetsDiscoveryTaskRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsDiscoveryTaskRespVO respVO : importExcelList) {
            try {
                AssetsDiscoveryTaskDO AssetsDiscoveryTaskDO = BeanUtils.toBean(respVO, AssetsDiscoveryTaskDO.class);
                Long AssetsDiscoveryTaskId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsDiscoveryTaskId != null) {
                        AssetsDiscoveryTaskDO existingDaDiscoveryTask = AssetsDiscoveryTaskMapper.selectById(AssetsDiscoveryTaskId);
                        if (existingDaDiscoveryTask != null) {
                            AssetsDiscoveryTaskMapper.updateById(AssetsDiscoveryTaskDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDiscoveryTaskId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDiscoveryTaskId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsDiscoveryTaskDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsDiscoveryTaskId);
                    AssetsDiscoveryTaskDO existingDaDiscoveryTask = AssetsDiscoveryTaskMapper.selectOne(queryWrapper);
                    if (existingDaDiscoveryTask == null) {
                        AssetsDiscoveryTaskMapper.insert(AssetsDiscoveryTaskDO);
                        successNum++;
                        successMessages.add("ID " + AssetsDiscoveryTaskId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsDiscoveryTaskId + " ");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append(" ").append(failureNum).append(" ");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append(" ").append(successNum).append(" ");
        }
        return resultMsg.toString();
    }

    @Override
    public boolean updateDaDiscoveryTaskStatus(AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById = this.getDaDiscoveryTaskById(AssetsDiscoveryTask.getId());
        String AssetsDiscoveryTaskStatus = AssetsDiscoveryTask.getStatus();

        validateTaskStatus(AssetsDiscoveryTaskById, AssetsDiscoveryTaskStatus);
        String spaceCode = resolveSpaceCode(AssetsDiscoveryTaskById.getDatasourceId());

        AssetsDiscoveryTask.setCronExpression(AssetsDiscoveryTaskById.getCronExpression());
        Long systemJobId = AssetsDiscoveryTaskById.getSystemJobId();
        if (StringUtils.equals(AssetsDiscoveryTaskStatus, AssetsDiscoveryTaskById.getStatus())) {
            return true;
        }

        if (StringUtils.equals("1", AssetsDiscoveryTaskStatus)) {
            handleOfflineTask(spaceCode, AssetsDiscoveryTaskById, systemJobId, AssetsDiscoveryTask);
            return true;
        }

        handleOnlineTask(spaceCode, AssetsDiscoveryTaskById, systemJobId, AssetsDiscoveryTask);

        updateTaskStatusAndScheduler(spaceCode, AssetsDiscoveryTask, systemJobId);

        return true;
    }

    private void validateTaskStatus(AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, String AssetsDiscoveryTaskStatus) {
        if (AssetsDiscoveryTaskById == null || AssetsDiscoveryTaskStatus == null) {
            throw new ServiceException("");
        }
    }

    private void handleOfflineTask(String spaceCode, AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, Long systemJobId, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        if (AssetsDiscoveryTaskById.getSystemJobId() != null && systemJobId > 0) {
            DsStatusRespDTO respDTO = dsEtlTaskService.releaseTask("OFFLINE", spaceCode, AssetsDiscoveryTaskById.getTaskCode());
            if (!isDsStatusSuccess(respDTO)) {
                throw new ServiceException("");
            }

            DsStatusRespDTO offlined = iDsEtlSchedulerService.offlineScheduler(spaceCode, systemJobId);
            if (!isDsStatusSuccess(offlined)) {
                throw new ServiceException("");
            }
        }

        // 更新数据发现任务
        AssetsDiscoveryTaskDO updateObj = BeanUtils.toBean(AssetsDiscoveryTask, AssetsDiscoveryTaskDO.class);
        AssetsDiscoveryTaskMapper.updateById(updateObj);
    }

    private void handleOnlineTask(String spaceCode, AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, Long systemJobId, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        if (systemJobId == null || systemJobId < 1) {
            createNewProcessDefinition(spaceCode, AssetsDiscoveryTaskById, AssetsDiscoveryTask);
        } else if (AssetsDiscoveryTaskById.getTaskId() != null) {
            updateExistingProcessDefinition(spaceCode, AssetsDiscoveryTaskById, AssetsDiscoveryTask);
        }
    }

    private void createNewProcessDefinition(String spaceCode, AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName(AssetsDiscoveryTaskById.getName() + StringUtils.generateRandomString());
        input.addHttpParam("id", "PARAMETER", AssetsDiscoveryTaskById.getId());
        input.setId(AssetsDiscoveryTaskById.getId());
        ProcessDefinition definition = this.createProcessDefinition(spaceCode, input);
        TaskDefinition firstTaskDefinition = AssetsTaskConverter.getFirstTaskDefinition(definition);

        AssetsDiscoveryTask.setTaskId(definition.getId());
        AssetsDiscoveryTask.setTaskCode(String.valueOf(definition.getCode()));
        AssetsDiscoveryTask.setNodeId(firstTaskDefinition.getId());
        AssetsDiscoveryTask.setNodeCode(String.valueOf(firstTaskDefinition.getCode()));
    }

    private void updateExistingProcessDefinition(String spaceCode, AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        TaskSaveReqInput input = new TaskSaveReqInput();
        input.setName(AssetsDiscoveryTaskById.getName() + StringUtils.generateRandomString());
        input.addHttpParam("id", "PARAMETER", AssetsDiscoveryTaskById.getId());
        input.setId(AssetsDiscoveryTaskById.getId());

        input.setTaskId(AssetsDiscoveryTaskById.getTaskId());
        input.setTaskCode(String.valueOf(AssetsDiscoveryTaskById.getTaskCode()));
        input.setNodeId(AssetsDiscoveryTaskById.getNodeId());
        input.setNodeCode(String.valueOf(AssetsDiscoveryTaskById.getNodeCode()));

        ProcessDefinition definition = this.updateProcessDefinition(spaceCode, input);
        TaskDefinition firstTaskDefinition = AssetsTaskConverter.getFirstTaskDefinition(definition);

        AssetsDiscoveryTask.setTaskId(definition.getId());
        AssetsDiscoveryTask.setTaskCode(String.valueOf(definition.getCode()));
        AssetsDiscoveryTask.setNodeId(firstTaskDefinition.getId());
        AssetsDiscoveryTask.setNodeCode(String.valueOf(firstTaskDefinition.getCode()));
    }

    private void updateTaskStatusAndScheduler(String spaceCode, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask, Long systemJobId) {
        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.releaseTask("ONLINE", spaceCode, AssetsDiscoveryTask.getTaskCode());
        if (!isDsStatusSuccess(dsStatusRespDTO)) {
            throw new ServiceException("");
        }

        if (systemJobId != null && systemJobId > 0) {
            updateExistingScheduler(spaceCode, AssetsDiscoveryTask, systemJobId);
        } else {
            createNewScheduler(spaceCode, AssetsDiscoveryTask);
        }

        DsStatusRespDTO dsStatusRespDTO1 = iDsEtlSchedulerService.onlineScheduler(spaceCode, AssetsDiscoveryTask.getSystemJobId());
        if (!isDsStatusSuccess(dsStatusRespDTO1)) {
            throw new ServiceException("");
        }

        // 更新数据发现任务
        AssetsDiscoveryTaskDO updateObj = BeanUtils.toBean(AssetsDiscoveryTask, AssetsDiscoveryTaskDO.class);
        AssetsDiscoveryTaskMapper.updateById(updateObj);
    }

    private void updateExistingScheduler(String spaceCode, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask, Long systemJobId) {
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = AssetsTaskConverter.createSchedulerUpdateRequest(systemJobId, AssetsDiscoveryTask.getCronExpression(), AssetsDiscoveryTask.getTaskCode());
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, spaceCode);
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            createSchedulerIfNeeded(spaceCode, AssetsDiscoveryTask);
        } else {
            Schedule schedule = dsSchedulerRespDTO.getData();
            AssetsDiscoveryTask.setSystemJobId(schedule.getId());
        }
    }

    private void createNewScheduler(String spaceCode, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = AssetsTaskConverter.createSchedulerRequest(AssetsDiscoveryTask.getCronExpression(), AssetsDiscoveryTask.getTaskCode());
        DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, spaceCode);
        if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
            createSchedulerIfNeeded(spaceCode, AssetsDiscoveryTask);
        } else {
            Schedule schedule = dsSchedulerRespDTO.getData();
            AssetsDiscoveryTask.setSystemJobId(schedule.getId());
        }
    }

    private void createSchedulerIfNeeded(String spaceCode, AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        DsSchedulerRespDTO byTaskCode = iDsEtlSchedulerService.getByTaskCode(spaceCode, AssetsDiscoveryTask.getTaskCode());
        if (byTaskCode == null || !byTaskCode.getSuccess()) {
            //     * 创建调度器 (只有任务发布了才能调用该接口)
            DsSchedulerSaveReqDTO dsSchedulerSaveReqDTO = AssetsTaskConverter.createSchedulerRequest(AssetsDiscoveryTask.getCronExpression(), AssetsDiscoveryTask.getTaskCode());
            DsSchedulerRespDTO saveScheduler = iDsEtlSchedulerService.saveScheduler(dsSchedulerSaveReqDTO, spaceCode);
            if (saveScheduler == null || !saveScheduler.getSuccess()) {
                throw new ServiceException("");
            }
            Schedule schedule = saveScheduler.getData();
            AssetsDiscoveryTask.setSystemJobId(schedule.getId());
            return;
        }
        Schedule schedule = byTaskCode.getData();
        AssetsDiscoveryTask.setSystemJobId(schedule.getId());
        DsSchedulerUpdateReqDTO schedulerUpdateRequest = AssetsTaskConverter.createSchedulerUpdateRequest(schedule.getId(), AssetsDiscoveryTask.getCronExpression(), AssetsDiscoveryTask.getTaskCode());
        DsSchedulerRespDTO updated = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, spaceCode);
        if (updated == null || !updated.getSuccess()) {
            throw new ServiceException("");
        }
    }

    private boolean isDsStatusSuccess(DsStatusRespDTO response) {
        if (response == null || Boolean.FALSE.equals(response.getSuccess()) || Boolean.FALSE.equals(response.getData())) {
            return false;
        }
        return Boolean.TRUE.equals(response.getSuccess())
                || Boolean.TRUE.equals(response.getData())
                || StringUtils.equalsIgnoreCase(response.getMsg(), "success");
    }

    @Override
    public AjaxResult startDaDiscoveryTask(Long id) {
        AssetsDiscoveryTaskDO AssetsDiscoveryTaskDO = AssetsDiscoveryTaskMapper.selectById(id);
        if (AssetsDiscoveryTaskDO == null) {
            return error("");
        }
        if (!StringUtils.equals("0", AssetsDiscoveryTaskDO.getStatus())) {
            return error("");
        }

        String spaceCode = resolveSpaceCode(AssetsDiscoveryTaskDO.getDatasourceId());
        DsStartTaskReqDTO dsStartTaskReqDTO = AssetsTaskConverter.createDsStartTaskReqDTO(AssetsDiscoveryTaskDO.getTaskCode());

        DsStatusRespDTO dsStatusRespDTO = dsEtlTaskService.startTask(dsStartTaskReqDTO, spaceCode);

        return Boolean.TRUE.equals(dsStatusRespDTO == null ? null : dsStatusRespDTO.getSuccess())
                ? success() : error(dsStatusRespDTO == null ? "DolphinScheduler无响应" : dsStatusRespDTO.getMsg());
    }

    @Override
    public boolean updateDaDiscoveryTaskCronExpression(AssetsDiscoveryTaskSaveReqVO AssetsDiscoveryTask) {
        AssetsDiscoveryTaskRespVO AssetsDiscoveryTaskById = this.getDaDiscoveryTaskById(AssetsDiscoveryTask.getId());
        String spaceCode = resolveSpaceCode(AssetsDiscoveryTaskById.getDatasourceId());
        Long systemJobId = AssetsDiscoveryTaskById.getSystemJobId();
        if (systemJobId != null) {
            try {
                //     * 创建调度器 (只有任务发布了才能调用该接口)
                DsSchedulerUpdateReqDTO schedulerUpdateRequest = AssetsTaskConverter.createSchedulerUpdateRequest(systemJobId, AssetsDiscoveryTask.getCronExpression(), AssetsDiscoveryTaskById.getTaskCode());
                DsSchedulerRespDTO dsSchedulerRespDTO = iDsEtlSchedulerService.updateScheduler(schedulerUpdateRequest, spaceCode);
                if (dsSchedulerRespDTO == null || !dsSchedulerRespDTO.getSuccess()) {
                    AssetsDiscoveryTask.setTaskId(AssetsDiscoveryTaskById.getTaskId());
                    AssetsDiscoveryTask.setTaskCode(String.valueOf(AssetsDiscoveryTaskById.getTaskCode()));
                    AssetsDiscoveryTask.setNodeId(AssetsDiscoveryTaskById.getNodeId());
                    AssetsDiscoveryTask.setNodeCode(String.valueOf(AssetsDiscoveryTaskById.getNodeCode()));
                    createSchedulerIfNeeded(spaceCode, AssetsDiscoveryTask);
                } else {
                    Schedule schedule = dsSchedulerRespDTO.getData();
                    AssetsDiscoveryTask.setSystemJobId(schedule.getId());
                }
            } catch (Exception e) {
                throw new ServiceException("");

            }
        }

        // 更新数据发现任务
        AssetsDiscoveryTaskDO updateObj = BeanUtils.toBean(AssetsDiscoveryTask, AssetsDiscoveryTaskDO.class);
        AssetsDiscoveryTaskMapper.updateById(updateObj);
//
    this.updateDaDiscoveryTask(AssetsDiscoveryTask);
        return true;
    }

    public ProcessDefinition createProcessDefinition(String spaceCode, TaskSaveReqInput input) {
        if (StringUtils.isBlank(input.getNodeCode())) {
            Long nodeUniqueKey = this.getNodeUniqueKey(AssetsTaskConverter.stringToLong(spaceCode));
            input.setNodeCode(AssetsTaskConverter.longToString(nodeUniqueKey));
        }

        DsTaskSaveReqDTO dsTaskSaveReqDTO = AssetsTaskConverter.buildDsTaskSaveReq(input);
        DsTaskSaveRespDTO task = dsEtlTaskService.createTask(dsTaskSaveReqDTO, AssetsTaskConverter.stringToLong(spaceCode));

        if (task == null || !Boolean.TRUE.equals(task.getSuccess())) {
            throw new ServiceException("");// 抛出任务定义创建错误的异常
        }
        ProcessDefinition Data = task.getData();
        return Data; // 返回创建结果
    }

    public ProcessDefinition updateProcessDefinition(String spaceCode, TaskSaveReqInput input) {
        if (StringUtils.isBlank(input.getNodeCode())) {
            Long nodeUniqueKey = this.getNodeUniqueKey(AssetsTaskConverter.stringToLong(spaceCode));
            input.setNodeCode(AssetsTaskConverter.longToString(nodeUniqueKey));
        }

        DsTaskSaveReqDTO dsTaskSaveReqDTO = AssetsTaskConverter.buildDsTaskSaveReq(input);
        DsTaskSaveRespDTO task = dsEtlTaskService.updateTask(dsTaskSaveReqDTO, spaceCode, input.getTaskCode());

        if (task == null || !Boolean.TRUE.equals(task.getSuccess())) {
            throw new ServiceException("");// 抛出任务定义创建错误的异常
        }
        ProcessDefinition Data = task.getData();
        return Data; // 返回创建结果
    }

    public Long getNodeUniqueKey(Long spaceCode) {
        try {
            DsNodeGenCodeRespDTO dsNodeGenCodeRespDTO = dsEtlNodeService.genCode(spaceCode);
            return dsNodeGenCodeRespDTO.getData().get(0);
        } catch (Exception e) {
            throw new ServiceException("");// 抛出任务定义创建错误的异常
        }
    }

    private String resolveSpaceCode(Long datasourceId) {
        return datasourceApiService.getSpaceCodeByDatasourceId(datasourceId);
    }

}
