

package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.api.space.ISpaceApi;
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
    private ISpaceApi attProjectApi;

    @Resource
    private ICollectorEtlNodeInstanceLogService CollectorEtlNodeInstanceLogService;

    private void requireNodeSpace(Long spaceId, String spaceCode) {
        Long user = com.datamaster.common.utils.SecurityUtils.getUserId();
        if (com.datamaster.common.utils.SecurityUtils.isAdmin(user)) return;
        if ((spaceId == null && StringUtils.isBlank(spaceCode)) || !attProjectApi.hasUserSpace(user, spaceId, spaceCode)) {
            throw new ServiceException("无权访问该空间的节点实例", 403);
        }
    }

    private void requireManualAdministrator() {
        if (!com.datamaster.common.utils.SecurityUtils.isAdmin(com.datamaster.common.utils.SecurityUtils.getUserId())) {
            throw new ServiceException("节点运行记录由调度器同步，不允许普通用户手工修改", 403);
        }
    }

    @Override
    public PageResult<CollectorEtlNodeInstanceDO> getCollectorEtlNodeInstancePage(CollectorEtlNodeInstancePageReqVO pageReqVO) {
        if (pageReqVO.getId() != null) {
            CollectorEtlNodeInstanceDO node = getCollectorEtlNodeInstanceById(pageReqVO.getId());
            CollectorEtlTaskInstanceDO parent = CollectorEtlTaskInstanceService.getCollectorEtlTaskInstanceById(node.getTaskInstanceId());
            pageReqVO.setSpaceId(parent.getSpaceId()); pageReqVO.setSpaceCode(parent.getSpaceCode());
        }
        if (pageReqVO.getTaskInstanceId() != null) {
            CollectorEtlTaskInstanceDO parent = CollectorEtlTaskInstanceService.getCollectorEtlTaskInstanceById(pageReqVO.getTaskInstanceId());
            pageReqVO.setSpaceId(parent.getSpaceId()); pageReqVO.setSpaceCode(parent.getSpaceCode());
        }
        requireNodeSpace(pageReqVO.getSpaceId(), pageReqVO.getSpaceCode());
        if (pageReqVO.getSpaceId() != null) {
            String code = attProjectApi.getSpaceCodeBySpaceId(pageReqVO.getSpaceId());
            if (StringUtils.isBlank(code)) throw new ServiceException("节点查询空间不存在或未配置编码");
            if (StringUtils.isNotBlank(pageReqVO.getSpaceCode()) && !Objects.equals(code, pageReqVO.getSpaceCode())) {
                throw new ServiceException("节点查询空间ID与编码不一致");
            }
            pageReqVO.setSpaceCode(code);
        }
        return CollectorEtlNodeInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createCollectorEtlNodeInstance(CollectorEtlNodeInstanceSaveReqVO createReqVO) {
        requireManualAdministrator();
        CollectorEtlTaskInstanceDO parent = CollectorEtlTaskInstanceService.getCollectorEtlTaskInstanceById(createReqVO.getTaskInstanceId());
        createReqVO.setSpaceId(parent.getSpaceId()); createReqVO.setSpaceCode(parent.getSpaceCode());
        CollectorEtlNodeInstanceDO dictType = BeanUtils.toBean(createReqVO, CollectorEtlNodeInstanceDO.class);
        CollectorEtlNodeInstanceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateCollectorEtlNodeInstance(CollectorEtlNodeInstanceSaveReqVO updateReqVO) {
        requireManualAdministrator();
        CollectorEtlNodeInstanceDO existing = getCollectorEtlNodeInstanceById(updateReqVO.getId());
        if (existing.getIncrementalWindowStatus() != null) throw new ServiceException("增量阶段运行记录不允许手工编辑");
        if (updateReqVO.getTaskInstanceId() != null && !Objects.equals(existing.getTaskInstanceId(), updateReqVO.getTaskInstanceId())) {
            throw new ServiceException("不允许修改节点所属流程实例");
        }
        updateReqVO.setTaskInstanceId(existing.getTaskInstanceId());
        updateReqVO.setSpaceId(existing.getSpaceId()); updateReqVO.setSpaceCode(existing.getSpaceCode());
        updateReqVO.setDsId(existing.getDsId());
        // 相关校验

        // 更新数据集成节点实例
        CollectorEtlNodeInstanceDO updateObj = BeanUtils.toBean(updateReqVO, CollectorEtlNodeInstanceDO.class);
        return CollectorEtlNodeInstanceMapper.updateById(updateObj);
    }

    @Override
    public int removeCollectorEtlNodeInstance(Collection<Long> idList) {
        for (Long id : idList) {
            CollectorEtlNodeInstanceDO record = getCollectorEtlNodeInstanceById(id);
            if (record.getIncrementalWindowStatus() != null) throw new ServiceException("阶段记录承载成功水位或重放窗口，暂不允许单独删除");
        }
        return CollectorEtlNodeInstanceMapper.deleteBatchIds(idList);
    }

    @Override
    public CollectorEtlNodeInstanceDO getCollectorEtlNodeInstanceById(Long id) {
        CollectorEtlNodeInstanceDO node = CollectorEtlNodeInstanceMapper.selectById(id);
        if (node == null) throw new ServiceException("任务节点实例不存在");
        CollectorEtlTaskInstanceService.getCollectorEtlTaskInstanceById(node.getTaskInstanceId());
        return node;
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
        requireManualAdministrator();
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
                            if (existingCollectorEtlNodeInstance.getIncrementalWindowStatus() != null) throw new ServiceException("增量阶段记录不允许导入覆盖");
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
                .spaceId(attProjectApi.getSpaceIdBySpaceCode(String.valueOf(taskInstance.getProjectCode())))
                .spaceCode(String.valueOf(taskInstance.getProjectCode()))
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
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY, rollbackFor = Exception.class)
    public void synchronizeSchedulerNodes(CollectorEtlTaskInstanceDO parent, List<TaskInstance> nodes) {
        if (parent == null || parent.getId() == null || parent.getTaskId() == null
                || parent.getDsId() == null || nodes == null) {
            throw new ServiceException("缺少已登记运行的父实例或调度节点列表");
        }
        // 调用方在同一事务持有父任务 owner 行锁；不复用旧回调的 DS ID = 本地 ID 假设。
        for (TaskInstance node : nodes) {
            if (node == null || node.getId() == null || node.getState() == null
                    || !Objects.equals(parent.getDsId(), node.getProcessInstanceId())) {
                throw new ServiceException("调度节点身份、状态或所属工作流不一致");
            }
            if (StringUtils.isNotBlank(node.getProjectCode())
                    && !Objects.equals(parent.getSpaceCode(), node.getProjectCode())) {
                throw new ServiceException("调度节点与父实例空间不一致");
            }
            CollectorEtlNodeInstanceDO existing = getByDsId(node.getId());
            if (existing == null) existing = CollectorEtlNodeInstanceMapper.findByParentCode(parent.getId(), node.getTaskCode());
            if (existing != null && !Objects.equals(existing.getNodeCode(), node.getTaskCode())) throw new ServiceException("DS节点code与阶段记录不一致");
            if (existing != null && existing.getDsId() != null && !existing.getDsId().equals(node.getId())
                    && existing.getIncrementalWindowStatus() != null) {
                if (existing.getDsId() > node.getId()) continue;
                if (CollectorEtlNodeInstanceMapper.clearPreviousAttempt(existing.getId(), existing.getDsId()) != 1) {
                    throw new ServiceException("阶段重试身份已变化，请重新同步");
                }
            }
            if (existing != null && !Objects.equals(parent.getDsId(), existing.getDsTaskInstanceId())) {
                throw new ServiceException("已有节点调度身份属于其他工作流实例");
            }
            CollectorEtlNodeInstanceDO instance = CollectorEtlNodeInstanceDO.builder()
                    .id(existing == null ? com.datamaster.common.utils.uuid.IdUtils.generateArtificialId() : existing.getId())
                    .taskType(parent.getTaskType())
                    .name(node.getName())
                    .nodeType(node.getTaskType())
                    .nodeId(existing == null || existing.getNodeId() == null ? 0L : existing.getNodeId())
                    .parameters(existing == null || existing.getParameters() == null ? "{}" : existing.getParameters())
                    .nodeCode(node.getTaskCode())
                    .nodeVersion(node.getTaskDefinitionVersion())
                    .taskInstanceId(parent.getId())
                    .taskInstanceName(parent.getName())
                    .spaceId(parent.getSpaceId())
                    .spaceCode(parent.getSpaceCode())
                    .submitTime(node.getSubmitTime())
                    .startTime(node.getStartTime())
                    .endTime(node.getEndTime())
                    .executePath(node.getExecutePath())
                    .logPath(node.getLogPath())
                    .priority(node.getTaskInstancePriority() == null ? null : String.valueOf(node.getTaskInstancePriority().getCode()))
                    .retryTimes(node.getRetryTimes())
                    .delayTime(node.getDelayTime())
                    .cpuQuota(node.getCpuQuota())
                    .memoryMax(node.getMemoryMax())
                    .status(String.valueOf(node.getState().getCode()))
                    .dsId(node.getId())
                    .dsTaskInstanceId(parent.getDsId())
                    .stableStageKey(existing == null ? null : existing.getStableStageKey())
                    .build();
            // 新发布的辅助节点可能没有历史节点日志，仍保留其真实运行状态和日志路径。
            CollectorEtlNodeLogDO metadata = StringUtils.isBlank(node.getTaskCode()) ? null
                    : CollectorEtlNodeLogService.getByNodeCodeAndVersion(node.getTaskCode(), node.getTaskDefinitionVersion());
            if (metadata != null) {
                instance.setNodeId(metadata.getId());
                instance.setComponentType(metadata.getComponentType());
                instance.setParameters(metadata.getParameters());
            }
            boolean saved = existing == null ? save(instance) : updateById(instance);
            if (!saved) throw new ServiceException("调度节点运行记录同步失败");
        }
    }

    @Override
    public CollectorEtlNodeInstanceDO getByDsId(Long dsId) {
        return baseMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlNodeInstanceDO.class)
                .eq(CollectorEtlNodeInstanceDO::getDsId, dsId));
    }

    @Override
    public String getLogByNodeInstanceId(Long nodeInstanceId) {
        CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = this.getCollectorEtlNodeInstanceById(nodeInstanceId);
        String content = "";
        if (CollectorEtlNodeInstanceDO == null) {
            return content;
        }
        String logContent = CollectorEtlNodeInstanceLogService.getLog(CollectorEtlNodeInstanceDO.getId());
        if (logContent != null) {
            content += logContent + "\n";
        }
        return content;
    }
}
