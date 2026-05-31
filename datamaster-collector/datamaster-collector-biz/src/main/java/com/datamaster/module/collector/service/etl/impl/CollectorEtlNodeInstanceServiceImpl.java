

package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.module.taxonomy.api.project.ITaxonomyProjectApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstancePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceSaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlNodeInstanceMapper;
import com.datamaster.module.collector.service.etl.*;
import com.datamaster.module.collector.utils.TaskConverter;
import com.datamaster.redis.service.IRedisService;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据集成节点实例Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlNodeInstanceServiceImpl extends ServiceImpl<CollectorEtlNodeInstanceMapper, CollectorEtlNodeInstanceDO> implements ICollectorEtlNodeInstanceService {
    @Resource
    private CollectorEtlNodeInstanceMapper CollectorEtlNodeInstanceMapper;

    @Resource
    private ICollectorEtlNodeService CollectorEtlNodeService;

    @Resource
    private ICollectorEtlNodeLogService CollectorEtlNodeLogService;

    @Resource
    private ICollectorEtlTaskInstanceService CollectorEtlTaskInstanceService;

    @Resource
    private ITaxonomyProjectApi attProjectApi;

    @Resource
    private IRedisService redisService;

    @Resource
    private ICollectorEtlTaskInstanceLogService CollectorEtlTaskInstanceLogService;

    @Resource
    private ICollectorEtlNodeInstanceLogService CollectorEtlNodeInstanceLogService;

    @Override
    public PageResult<CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstancePage(CollectorEtlNodeInstancePageReqVO pageReqVO) {
        return CollectorEtlNodeInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorEtlNodeInstance(CollectorEtlNodeInstanceSaveReqVO createReqVO) {
        CollectorEtlNodeInstanceDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlNodeInstanceDO.class);
        CollectorEtlNodeInstanceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlNodeInstance(CollectorEtlNodeInstanceSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成节点实例
        CollectorEtlNodeInstanceDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlNodeInstanceDO.class);
        return CollectorEtlNodeInstanceMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlNodeInstance(Collection<Long> idList) {
        // 批量删除数据集成节点实例
        return CollectorEtlNodeInstanceMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlNodeInstanceDO getCollectorEtlNodeInstanceById(Long id) {
        return CollectorEtlNodeInstanceMapper.selectById(id);
    }

    @Override
    public List<CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstanceList() {
        return CollectorEtlNodeInstanceMapper.selectList();
    }

    @Override
    public Map<Long, CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstanceMap() {
        List<CollectorEtlNodeInstanceDO> CollectorEtlNodeInstanceList = CollectorEtlNodeInstanceMapper.selectList();
        return CollectorEtlNodeInstanceList.stream()
                .collect(Collectors.toMap(
                        CollectorEtlNodeInstanceDO::getId,
                        CollectorEtlNodeInstanceDO -> CollectorEtlNodeInstanceDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据集成节点实例数据
     *
     * @param importExcelList 数据集成节点实例数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importCollectorEtlNodeInstance(List<CollectorEtlNodeInstanceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (CollectorEtlNodeInstanceRespVO respVO : importExcelList) {
            try {
                CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = BeanUtils.toBean(respVO, CollectorEtlNodeInstanceDO.class);
                Long CollectorEtlNodeInstanceId = respVO.getId();
                if (isUpdateSupport) {
                    if (CollectorEtlNodeInstanceId != null) {
                        CollectorEtlNodeInstanceDO existingCollectorEtlNodeInstance = CollectorEtlNodeInstanceMapper.selectById(CollectorEtlNodeInstanceId);
                        if (existingCollectorEtlNodeInstance != null) {
                            CollectorEtlNodeInstanceMapper.updateById(CollectorEtlNodeInstanceDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + CollectorEtlNodeInstanceId + " 的数据集成节点实例记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + CollectorEtlNodeInstanceId + " 的数据集成节点实例记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<CollectorEtlNodeInstanceDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", CollectorEtlNodeInstanceId);
                    CollectorEtlNodeInstanceDO existingCollectorEtlNodeInstance = CollectorEtlNodeInstanceMapper.selectOne(queryWrapper);
                    if (existingCollectorEtlNodeInstance == null) {
                        CollectorEtlNodeInstanceMapper.insert(CollectorEtlNodeInstanceDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + CollectorEtlNodeInstanceId + " 的数据集成节点实例记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + CollectorEtlNodeInstanceId + " 的数据集成节点实例记录已存在。");
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
    public Boolean createNodeInstance(TaskInstance taskInstance) {
        log.info(JSONObject.toJSONString(taskInstance));
        CollectorEtlNodeLogDO CollectorEtlNodeRespDTO = CollectorEtlNodeLogService.getByNodeCodeAndVersion(taskInstance.getTaskCode(), taskInstance.getTaskDefinitionVersion());
        if (CollectorEtlNodeRespDTO == null) {
            return true;
        }
        CollectorEtlNodeInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlNodeInstanceDO.builder()
                .id(taskInstance.getId())
                .taskType(CollectorEtlNodeRespDTO.getTaskType())
                .name(taskInstance.getName())
                .nodeType(taskInstance.getTaskType())
                .nodeId(CollectorEtlNodeRespDTO.getId())
                .nodeCode(taskInstance.getTaskCode())
                .nodeVersion(taskInstance.getTaskDefinitionVersion())
                .taskInstanceId(taskInstance.getProcessInstanceId())
                .taskInstanceName(taskInstance.getProcessInstanceName())
                .projectId(attProjectApi.getProjectIdByProjectCode(String.valueOf(taskInstance.getProjectCode())))
                .projectCode(String.valueOf(taskInstance.getProjectCode()))
                .submitTime(taskInstance.getSubmitTime())
                .startTime(taskInstance.getStartTime())
                .executePath(taskInstance.getExecutePath())
                .parameters(CollectorEtlNodeRespDTO.getParameters())
                .priority(String.valueOf(taskInstance.getTaskInstancePriority().getCode()))
                .retryTimes(taskInstance.getRetryTimes())
                .delayTime(taskInstance.getDelayTime())
                .cpuQuota(taskInstance.getCpuQuota())
                .memoryMax(taskInstance.getMemoryMax())
                .status(String.valueOf(taskInstance.getState().getCode()))
                .componentType(CollectorEtlNodeRespDTO.getComponentType())
                .dsId(taskInstance.getId())
                .dsTaskInstanceId(taskInstance.getProcessInstanceId())
                .executePath(taskInstance.getExecutePath())
                .logPath(taskInstance.getLogPath())
                .build();
        return this.save(CollectorEtlTaskInstanceDO);
    }

    @Override
    public Boolean updateNodeInstance(TaskInstance taskInstance) {
        log.info(JSONObject.toJSONString(taskInstance));
        CollectorEtlNodeInstanceDO old = this.getById(taskInstance.getId());
        if (old == null) {
            return true;
        }
        CollectorEtlNodeInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlNodeInstanceDO.builder()
                .id(old.getId())
                .startTime(taskInstance.getStartTime())
                .endTime(taskInstance.getEndTime())
                .executePath(taskInstance.getExecutePath())
                .logPath(taskInstance.getLogPath())
                .status(String.valueOf(taskInstance.getState().getCode()))
                .build();
        return this.saveOrUpdate(CollectorEtlTaskInstanceDO);
    }

    @Override
    public CollectorEtlNodeInstanceDO getByDsId(Long dsId) {
        return baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlNodeInstanceDO.class)
                .eq(CollectorEtlNodeInstanceDO::getDsId, dsId));
    }

    @Override
    public void taskInstanceLogInsert(String taskInstanceId, String processInstanceId, String logStr) {
        String taskInstanceLogKey = TaskConverter.TASK_INSTANCE_LOG_KEY + taskInstanceId;
        String processInstanceLogKey = TaskConverter.PROCESS_INSTANCE_LOG_KEY + processInstanceId;
        //判断当前任务实例是否存在
        if (processInstanceId == null || StringUtils.equals("null", processInstanceId) || (!redisService.hasKey(processInstanceLogKey) && CollectorEtlTaskInstanceService.count(Wrappers.lambdaQuery(CollectorEtlTaskInstanceDO.class)
                .eq(CollectorEtlTaskInstanceDO::getId, Long.parseLong(processInstanceId))) == 0)) {
            return;
        }
        String taskInstanceLog = redisService.get(taskInstanceLogKey);
        String processInstanceLog = redisService.get(processInstanceLogKey);
        if (taskInstanceLog == null) {
            taskInstanceLog = "";
        }
        if (processInstanceLog == null) {
            processInstanceLog = "";
        }
        taskInstanceLog += logStr + (logStr.matches(".*\r?\n.*") ? "" : "\n");
        processInstanceLog += logStr + (logStr.matches(".*\r?\n.*") ? "" : "\n");
        redisService.set(taskInstanceLogKey, taskInstanceLog);
        redisService.set(processInstanceLogKey, processInstanceLog);

        //判断会话是否结束
        if (StringUtils.indexOf(logStr, "FINALIZE_SESSION") > -1) {
            //判断当前任务实例是否结束
            CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlTaskInstanceService.getById(Long.parseLong(processInstanceId));
            //判断状态  5：停止 6：失败 7：成功
            if (CollectorEtlTaskInstanceDO != null && Arrays.asList("5", "6", "7").contains(CollectorEtlTaskInstanceDO.getStatus())) {
                //写入日志
                redisService.delete(processInstanceLogKey);
                //判断是否是数据集成
                if (StringUtils.equals("1", CollectorEtlTaskInstanceDO.getTaskType())) {
                    //写入日志
                    CollectorEtlTaskInstanceLogService.saveOrUpdate(CollectorEtlTaskInstanceLogDO.builder()
                            .taskInstanceId(CollectorEtlTaskInstanceDO.getId())
                            .tm(new Date())
                            .taskType(CollectorEtlTaskInstanceDO.getTaskType())
                            .taskId(CollectorEtlTaskInstanceDO.getTaskId())
                            .taskCode(CollectorEtlTaskInstanceDO.getTaskCode())
                            .logContent(processInstanceLog)
                            .build());
                }
            }

            //获取当前节点实例
            CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = this.getById(Long.parseLong(taskInstanceId));
            //写入日志,5分钟过期用于兼容节点状态未改变时可以正常查询日志
            redisService.delete(taskInstanceLogKey);
            redisService.set(taskInstanceLogKey, taskInstanceLog, 60 * 5);
            CollectorEtlNodeInstanceLogService.save(CollectorEtlNodeInstanceLogDO.builder()
                    .nodeInstanceId(CollectorEtlNodeInstanceDO.getId())
                    .tm(new Date())
                    .taskType(CollectorEtlNodeInstanceDO.getTaskType())
                    .nodeId(CollectorEtlNodeInstanceDO.getNodeId())
                    .nodeCode(CollectorEtlNodeInstanceDO.getNodeCode())
                    .taskInstanceId(CollectorEtlNodeInstanceDO.getTaskInstanceId())
                    .logContent(taskInstanceLog)
                    .build());
        }
    }

    @Override
    public String getLogByNodeInstanceId(Long nodeInstanceId) {
        CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = this.getCollectorEtlNodeInstanceById(nodeInstanceId);
        String content = "";
        String processInstanceLogKey = TaskConverter.PROCESS_INSTANCE_LOG_KEY + CollectorEtlNodeInstanceDO.getId();
        if (redisService.hasKey(processInstanceLogKey)) {
            content += redisService.get(processInstanceLogKey) + "\n";
        } else {
            //获取表中的日志
            String logContent = CollectorEtlNodeInstanceLogService.getLog(CollectorEtlNodeInstanceDO.getId());
            if (logContent != null) {
                content += logContent + "\n";
            }
        }
        return content;
    }
}
