package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.workflow.vo.ActionWorkflowRunPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionWorkflowRunDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;
import java.util.Date;
import java.util.Collection;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

public interface ActionWorkflowRunMapper extends BaseMapperX<ActionWorkflowRunDO> {
    default List<ActionWorkflowRunDO> selectByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getWorkflowId, workflowId)
                .orderByDesc(ActionWorkflowRunDO::getId));
    }

    default PageResult<ActionWorkflowRunDO> selectPageByWorkflowId(Long workflowId,
                                                                     ActionWorkflowRunPageReqVO reqVO) {
        LambdaQueryWrapperX<ActionWorkflowRunDO> wrapper = new LambdaQueryWrapperX<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getWorkflowId, workflowId)
                .eqIfPresent(ActionWorkflowRunDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ActionWorkflowRunDO::getIdempotencyKey, reqVO.getIdempotencyKey())
                .orderByDesc(ActionWorkflowRunDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default ActionWorkflowRunDO selectByWorkflowIdAndIdempotencyKey(Long workflowId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getWorkflowId, workflowId)
                .eq(ActionWorkflowRunDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    default List<ActionWorkflowRunDO> selectQueued(int limit) {
        LambdaQueryWrapperX<ActionWorkflowRunDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ActionWorkflowRunDO::getStatus, "QUEUED").orderByAsc(ActionWorkflowRunDO::getId);
        if (limit > 0) wrapper.last("LIMIT " + limit);
        return selectList(wrapper);
    }

    default List<ActionWorkflowRunDO> selectRunning(int limit) {
        LambdaQueryWrapperX<ActionWorkflowRunDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ActionWorkflowRunDO::getStatus, "RUNNING")
                .orderByAsc(ActionWorkflowRunDO::getUpdateTime);
        if (limit > 0) wrapper.last("LIMIT " + limit);
        return selectList(wrapper);
    }

    /** 查询最近运行样本，监控接口在服务层继续按 Workflow 空间权限过滤。 */
    default List<ActionWorkflowRunDO> selectRecent(int limit) {
        LambdaQueryWrapperX<ActionWorkflowRunDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.orderByDesc(ActionWorkflowRunDO::getId);
        if (limit > 0) wrapper.last("LIMIT " + limit);
        return selectList(wrapper);
    }

    default int claimQueued(Long runId, String worker) {
        return update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, runId)
                .eq(ActionWorkflowRunDO::getStatus, "QUEUED")
                .set(ActionWorkflowRunDO::getStatus, "RUNNING")
                .set(ActionWorkflowRunDO::getUpdateBy, worker)
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }

    default int requeueRunning(Long runId) {
        return update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getId, runId)
                .eq(ActionWorkflowRunDO::getStatus, "RUNNING")
                .set(ActionWorkflowRunDO::getStatus, "QUEUED")
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }

    default int touchRunning(Collection<Long> runIds, String worker) {
        if (runIds == null || runIds.isEmpty()) return 0;
        return update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .in(ActionWorkflowRunDO::getId, runIds)
                .eq(ActionWorkflowRunDO::getStatus, "RUNNING")
                .set(ActionWorkflowRunDO::getUpdateBy, worker)
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }

    default int requeueStaleRunning(Date staleBefore) {
        if (staleBefore == null) return 0;
        return update(null, new LambdaUpdateWrapper<ActionWorkflowRunDO>()
                .eq(ActionWorkflowRunDO::getStatus, "RUNNING")
                .eq(ActionWorkflowRunDO::getCancelRequested, 0)
                .and(wrapper -> wrapper.isNull(ActionWorkflowRunDO::getUpdateTime)
                        .or().lt(ActionWorkflowRunDO::getUpdateTime, staleBefore))
                .set(ActionWorkflowRunDO::getStatus, "QUEUED")
                .set(ActionWorkflowRunDO::getUpdateBy, "workflow-recovery")
                .set(ActionWorkflowRunDO::getUpdateTime, new Date()));
    }
}
