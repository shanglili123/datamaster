package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.ExecutionPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.ActionExecutionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 动作执行记录 Mapper
 */
public interface ActionExecutionMapper extends BaseMapperX<ActionExecutionDO> {

    default PageResult<ActionExecutionDO> selectPage(ExecutionPageReqVO reqVO) {
        // 动作过滤优先级：actionIds（多选）> actionId（单选）> 不过滤
        // 适配场景：「执行记录分页根据上面动作分页页内动作动态变化」——前端传 actionIds 实现按页联动
        List<Long> actionFilter;
        if (reqVO.getActionIds() != null && !reqVO.getActionIds().isEmpty()) {
            actionFilter = reqVO.getActionIds();
        } else if (reqVO.getActionId() != null) {
            actionFilter = Collections.singletonList(reqVO.getActionId());
        } else {
            actionFilter = null;
        }
        return selectPage(reqVO, new LambdaQueryWrapperX<ActionExecutionDO>()
                .inIfPresent(ActionExecutionDO::getActionId, actionFilter)
                .eq(reqVO.getOntologyId() != null, ActionExecutionDO::getOntologyId, reqVO.getOntologyId())
                .eq(reqVO.getStatus() != null, ActionExecutionDO::getStatus, reqVO.getStatus())
                .orderByDesc(ActionExecutionDO::getId));
    }

    default List<ActionExecutionDO> selectByActionId(Long actionId) {
        return selectList(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getActionId, actionId)
                .orderByDesc(ActionExecutionDO::getId));
    }

    /** 幂等查询：同动作 + 同幂等键的唯一执行记录（配合唯一索引 UK_ONT_AEXEC_IDEM 兜底），无则 null */
    default ActionExecutionDO selectByActionIdAndIdempotencyKey(Long actionId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getActionId, actionId)
                .eq(ActionExecutionDO::getIdempotencyKey, idempotencyKey)
                .last("limit 1"));
    }

    /** 查询某个 Workflow 运行生成的补偿动作执行记录。 */
    default List<ActionExecutionDO> selectWorkflowCompensations(Long workflowRunId) {
        if (workflowRunId == null) return Collections.emptyList();
        return selectList(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getWorkflowRunId, workflowRunId)
                .eq(ActionExecutionDO::getTriggerType, "WORKFLOW_COMPENSATION")
                .orderByAsc(ActionExecutionDO::getId));
    }

    default List<ActionExecutionDO> selectPendingApprovals(Long ontologyId) {
        return selectList(new LambdaQueryWrapperX<ActionExecutionDO>()
                .eq(ActionExecutionDO::getOntologyId, ontologyId)
                .eq(ActionExecutionDO::getStatus, "PENDING_APPROVAL")
                .orderByDesc(ActionExecutionDO::getId));
    }

    /** 后台自动执行候选；真正领取仍由 executeExecution 内的状态 CAS 保证单实例成功。 */
    default List<ActionExecutionDO> selectAutoExecuteCandidates(Date now, Integer limit) {
        // LambdaQueryWrapperX 未覆盖 apply/and 等全部父类方法的协变返回类型；
        // 若连续链式调用，表达式类型会退化为 MyBatis-Plus 原生 LambdaQueryWrapper。
        LambdaQueryWrapperX<ActionExecutionDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ActionExecutionDO::getStatus, "APPROVED");
        wrapper.eq(ActionExecutionDO::getAutoExecute, true);
        wrapper.isNull(ActionExecutionDO::getLockTime);
        wrapper.apply("COALESCE(ATTEMPT_NO, 0) < COALESCE(MAX_ATTEMPTS, 1)");
        wrapper.and(w -> w.isNull(ActionExecutionDO::getNextRunTime)
                .or().le(ActionExecutionDO::getNextRunTime, now));
        wrapper.orderByAsc(ActionExecutionDO::getLockTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    /** 查询超过锁超时时间仍处于 RUNNING 的执行记录，交由 Worker 标记为需人工对账。 */
    default List<ActionExecutionDO> selectStaleRunning(Date lockedBefore, Integer limit) {
        LambdaQueryWrapperX<ActionExecutionDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ActionExecutionDO::getStatus, "RUNNING");
        wrapper.isNotNull(ActionExecutionDO::getLockTime);
        wrapper.le(ActionExecutionDO::getLockTime, lockedBefore);
        wrapper.orderByAsc(ActionExecutionDO::getLockTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    /**
     * Workflow 节点超时后停止自动重试。动作可能已经在目标库产生副作用，
     * 因此只允许 RUNNING/APPROVED 记录转入人工对账状态。
     */
    default int markReconciliationRequired(Long executionId, String message) {
        if (executionId == null) return 0;
        return update(null, new LambdaUpdateWrapper<ActionExecutionDO>()
                .eq(ActionExecutionDO::getId, executionId)
                .in(ActionExecutionDO::getStatus, Arrays.asList("RUNNING", "APPROVED"))
                .set(ActionExecutionDO::getStatus, "RECONCILIATION_REQUIRED")
                .set(ActionExecutionDO::getErrorCode, "WORKFLOW_NODE_TIMEOUT")
                .set(ActionExecutionDO::getErrorMessage, message)
                .set(ActionExecutionDO::getLockTime, null)
                .set(ActionExecutionDO::getLockOwner, null)
                .set(ActionExecutionDO::getUpdateTime, new Date()));
    }

    /**
     * 查询某目标物理表最近的人为编辑登记（EXECUTED / ROLLED_BACK 落库记录），按执行时间倒序。
     * 供数据接收工程冲突仲裁：按表 + 主键匹配人为编辑做字段级裁决。
     */
    default List<ActionExecutionDO> selectLatestManualEditsByTable(String targetTable, Integer limit) {
        return selectLatestManualEditsByTable(targetTable, null, null, limit);
    }

    /** 按目标表及可选空间过滤，兼容执行记录中多目标表逗号拼接的历史数据。 */
    default List<ActionExecutionDO> selectLatestManualEditsByTable(String targetTable, Long spaceId, Integer limit) {
        return selectLatestManualEditsByTable(targetTable, null, spaceId, limit);
    }

    /** 按目标表、数据源及可选空间过滤人工编辑记录。 */
    default List<ActionExecutionDO> selectLatestManualEditsByTable(String targetTable, Long datasourceId,
                                                                    Long spaceId, Integer limit) {
        LambdaQueryWrapperX<ActionExecutionDO> wrapper = new LambdaQueryWrapperX<ActionExecutionDO>()
                .in(ActionExecutionDO::getStatus, Arrays.asList("EXECUTED", "ROLLED_BACK"))
                .orderByDesc(ActionExecutionDO::getExecuteTime);
        wrapper.and(w -> w.eq(ActionExecutionDO::getTargetTable, targetTable)
                .or().like(ActionExecutionDO::getTargetTable, targetTable + ",%")
                .or().like(ActionExecutionDO::getTargetTable, "%," + targetTable)
                .or().like(ActionExecutionDO::getTargetTable, "%," + targetTable + ",%"));
        wrapper.eq(spaceId != null, ActionExecutionDO::getSpaceId, spaceId);
        wrapper.eq(datasourceId != null, ActionExecutionDO::getDatasourceId, datasourceId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }
}
