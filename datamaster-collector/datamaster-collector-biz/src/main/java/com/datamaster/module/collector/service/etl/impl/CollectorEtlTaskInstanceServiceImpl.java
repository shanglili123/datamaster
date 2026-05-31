

package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DSExecuteDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
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
import com.datamaster.module.taxonomy.api.project.ITaxonomyProjectApi;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlNodeInstanceRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceLogStatusRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceRespDTO;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskRespDTO;
import com.datamaster.module.collector.api.service.etl.CollectorEtlTaskInstanceService;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskInstanceMapper;
import com.datamaster.module.collector.service.etl.*;
import com.datamaster.module.collector.utils.TaskConverter;
import com.datamaster.redis.service.IRedisService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private ITaxonomyProjectApi attProjectApi;

    @Resource
    private ICollectorEtlTaskService CollectorEtlTaskService;

    @Resource
    private ICollectorEtlTaskLogService CollectorEtlTaskLogService;

    @Resource
    private IDsEtlExecutorService dsEtlExecutorService;

    @Resource
    private ICollectorEtlNodeInstanceService CollectorEtlTNodeInstanceService;


    @Resource
    private IRedisService redisService;

    @Resource
    private ICollectorEtlTaskInstanceLogService CollectorEtlTaskInstanceLogService;

    @Resource
    private ICollectorEtlNodeInstanceLogService CollectorEtlNodeInstanceLogService;


    @Resource
    private ICollectorEtlNodeLogService CollectorEtlNodeLogService;

    @Resource
    private ICollectorEtlTaskNodeRelService iCollectorEtlTaskNodeRelService;

    @Override
    public PageResult<CollectorEtlTaskInstanceDO> getCollectorEtlTaskInstancePage(CollectorEtlTaskInstancePageReqVO pageReqVO) {
        return CollectorEtlTaskInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public CollectorEtlTaskInstanceRespVO getCollectorEtlTaskInstanceById(CollectorEtlTaskInstancePageReqVO reqVO) {
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

        return BeanUtils.toBean(CollectorEtlTaskInstanceDO, CollectorEtlTaskInstanceRespVO.class);
    }

    @Override
    public Long createCollectorEtlTaskInstance(CollectorEtlTaskInstanceSaveReqVO createReqVO) {
        CollectorEtlTaskInstanceDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlTaskInstanceDO.class);
        CollectorEtlTaskInstanceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlTaskInstance(CollectorEtlTaskInstanceSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务实例
        CollectorEtlTaskInstanceDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlTaskInstanceDO.class);
        return CollectorEtlTaskInstanceMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlTaskInstance(Collection<Long> idList) {
        // 批量删除数据集成任务实例
        return CollectorEtlTaskInstanceMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlTaskInstanceDO getCollectorEtlTaskInstanceById(Long id) {
        return CollectorEtlTaskInstanceMapper.selectById(id);
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
                .projectId(attProjectApi.getProjectIdByProjectCode(String.valueOf(processInstance.getProjectCode())))
                .projectCode(String.valueOf(processInstance.getProjectCode()))
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
        return this.save(collectorEtlTaskInstanceDO);
    }

    @Override
    public Boolean updateTaskInstance(ProcessInstance processInstance) {
        log.info(JSONObject.toJSONString(processInstance));
        CollectorEtlTaskInstanceDO old = this.getById(processInstance.getId());
        if (old == null) {
            return true;
        }
        CollectorEtlTaskInstanceDO collectorEtlTaskInstanceDO = CollectorEtlTaskInstanceDO.builder()
                .id(old.getId())
                .scheduleTime(processInstance.getCommandStartTime())
                .startTime(processInstance.getStartTime())
                .endTime(processInstance.getEndTime())
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
        return this.saveOrUpdate(collectorEtlTaskInstanceDO);
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
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = this.getById(taskInstanceId);
        if (CollectorEtlTaskInstanceDO == null) {
            return error("任务实例不存在，请刷新后重试！");
        }
        String status = CollectorEtlTaskInstanceDO.getStatus();
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
                !StringUtils.equals(status, "14")) {
            return error("当前状态无法停止，请刷新后重试！");
        }
        DsStatusRespDTO dsStatusRespDTO = dsEtlExecutorService.execute(DSExecuteDTO.builder()
                .processInstanceId(taskInstanceId)
                .executeType(executeType)
                .build(), CollectorEtlTaskInstanceDO.getProjectCode());
        return dsStatusRespDTO.getSuccess() ? success() : error(dsStatusRespDTO.getMsg());
    }

    @Override
    public List<CollectorEtlTaskInstanceTreeListRespVO> subNodelist(Long taskInstanceId, Long nodeInstanceId) {
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
    public CollectorEtlTaskInstanceLogStatusRespDTO getLogByTaskInstanceId(Long taskInstanceId) {
        String log = "";
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = this.getById(taskInstanceId);
        //获取任务信息
        CollectorEtlTaskLogRespVO CollectorEtlTaskLogRespVO = CollectorEtlTaskLogService.getCollectorEtlTaskLogById(CollectorEtlTaskLogPageReqVO.builder()
                .code(CollectorEtlTaskInstanceDO.getTaskCode())
                .version(CollectorEtlTaskInstanceDO.getTaskVersion())
                .build());
        if (CollectorEtlTaskLogRespVO == null) {
            throw new RuntimeException("任务不存在");
        }
        //获取节点关系数据
        JSONArray locations = JSONArray.parse(CollectorEtlTaskLogRespVO.getLocations());
        //获取节点数据
        List<CollectorEtlNodeInstanceDO> CollectorEtlNodeInstanceDOList = CollectorEtlTNodeInstanceService.list(Wrappers.lambdaQuery(CollectorEtlNodeInstanceDO.class)
                .select(CollectorEtlNodeInstanceDO::getId,
                        CollectorEtlNodeInstanceDO::getNodeCode,
                        CollectorEtlNodeInstanceDO::getName,
                        CollectorEtlNodeInstanceDO::getStatus)
                .eq(CollectorEtlNodeInstanceDO::getTaskInstanceId, taskInstanceId));

        String processInstanceLogKey = TaskConverter.PROCESS_INSTANCE_LOG_KEY + taskInstanceId;
        if (StringUtils.equals("1", CollectorEtlTaskInstanceDO.getTaskType())) {//判断是否是离线任务
            if (redisService.hasKey(processInstanceLogKey)) {
                log = redisService.get(processInstanceLogKey);
            } else {
                //获取表中的日志
                String logContent = CollectorEtlTaskInstanceLogService.getLog(taskInstanceId);
                if (logContent != null) {
                    log = logContent;
                }
            }
        } else {
            Map<String, CollectorEtlNodeInstanceDO> nodeInstanceMap = CollectorEtlNodeInstanceDOList.stream().collect(Collectors.toMap(key -> key.getNodeCode(), value -> value));

            for (int i = 0; i < locations.size(); i++) {
                JSONObject location = (JSONObject) locations.get(i);
                String code = String.valueOf(location.getLong("taskCode"));
                CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = nodeInstanceMap.get(code);

                if (CollectorEtlNodeInstanceDO != null) {
                    String taskInstanceLogKey = TaskConverter.TASK_INSTANCE_LOG_KEY + CollectorEtlNodeInstanceDO.getId();
                    if (redisService.hasKey(taskInstanceLogKey)) {
                        log += redisService.get(taskInstanceLogKey) + "\n";
                    } else {
                        //获取表中的日志
                        String logContent = CollectorEtlNodeInstanceLogService.getLog(CollectorEtlNodeInstanceDO.getId());
                        if (logContent != null) {
                            log += logContent + "\n";
                        }
                    }
                }
            }
        }


        return CollectorEtlTaskInstanceLogStatusRespDTO.builder()
                .log(log)
                .status(CollectorEtlTaskInstanceDO.getStatus())
                .nodeInstanceList(BeanUtils.toBean(CollectorEtlNodeInstanceDOList, CollectorEtlNodeInstanceRespDTO.class))
                .build();
    }

    @Override
    public Long getRunTaskInstance(Long taskId) {
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
