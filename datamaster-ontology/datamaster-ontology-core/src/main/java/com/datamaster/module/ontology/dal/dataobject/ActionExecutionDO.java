package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 动作执行记录 DO
 *
 * 对应表 ONT_ACTION_EXECUTION
 */
@Data
@TableName("ONT_ACTION_EXECUTION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionExecutionDO extends BaseEntity {

    /** 动作ID */
    private Long actionId;

    /** 本体ID */
    private Long ontologyId;

    /** 编辑目标物理表（执行落库时打标，供数据接收工程按表做人为编辑冲突仲裁） */
    private String targetTable;

    /** 执行者ID */
    private Long executorId;

    /** 提交时的空间ID（执行阶段二次权限校验复用） */
    private Long spaceId;

    /** 提交时的空间编码（执行阶段二次权限校验复用） */
    private String spaceCode;

    /** 目标对象主键（对象实例级决策载体；空=批处理未按对象膨胀成逐条执行记录） */
    private String objectKey;

    /** 前置检查结果 (JSONB)：{"decision":"PASS|REJECT","passed":boolean,"detail":[...]} */
    private String criteriaResult;

    /** 人工确认兼容关卡：0=无需/未提交确认，1=唯一人工确认任务 */
    private Integer currentStage;

    /** 输入参数 (JSONB) */
    private String inputParams;

    /** 生成的SQL */
    private String generatedSql;

    /** 预览结果 (JSONB) */
    private String previewResult;

    /** 执行前数据快照 (JSONB) */
    private String beforeData;

    /** 执行后数据快照 (JSONB)：UPDATE=重查新值 / DELETE=空集 / CREATE=按主键回查新行 */
    private String afterData;

    /** 回退前数据快照 (JSONB)：回退动作执行前一刻的数据（= 原执行 afterData，即回退时的当前状态） */
    private String rollbackBeforeData;

    /** 回退后数据快照 (JSONB)：回退动作执行后的数据（UPDATE/DELETE 回退=还原的 beforeData / CREATE 回退=空集） */
    private String rollbackAfterData;

    /** 状态：DRAFT/PENDING_APPROVAL/APPROVED/RUNNING/REJECTED/EXECUTED/FAILED/ROLLED_BACK/RECONCILIATION_REQUIRED */
    private String status;

    /** 是否由后台 Worker 自动执行；人工预览确认保持 false */
    private Boolean autoExecute;

    /** 触发类型：MANUAL / DATA_ARRIVAL / API / WORKFLOW */
    private String triggerType;

    /** 触发器引用（如 Kafka topic 或业务事件类型） */
    private String triggerRef;

    /** 来源事件唯一编号；数据到达场景为 topic:partition:offset */
    private String eventId;

    /** 最大执行尝试次数；V1 写动作默认 1，不自动重放 */
    private Integer maxAttempts;

    /** 最早可执行时间；用于延迟执行/后续重试扩展 */
    private java.util.Date nextRunTime;

    /** 审批意见 */
    private String approvalReason;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private java.util.Date approveTime;

    /** 执行时间 */
    private java.util.Date executeTime;

    /** 回退SQL（ROLLED_BACK 留痕：回退动作实际执行的还原语句） */
    private String rollbackSql;

    /** 回退时间 */
    private java.util.Date rollbackTime;

    /** 错误信息 */
    private String errorMessage;

    /** 提交时冻结的动作定义版本（执行前复核：定义变更后需重新提交） */
    private Integer actionVersion;

    /** 提交时冻结的函数版本（函数类型动作） */
    private Integer functionVersion;

    /** 提交时冻结的解析函数体 SHA-256（执行前按当前解析体哈希复核一致性） */
    private String functionHash;

    /** 幂等键：同动作下唯一，重复提交直接返回首次执行记录 */
    private String idempotencyKey;

    /** 尝试次数：初始0，每次实际执行前置+1（CAS 抢占并发锁时递增） */
    private Integer attemptNo;

    /** 执行中锁标记：非空=正在执行；执行结束清空。仅 APPROVED 且 LOCK_TIME 为空可抢占 */
    private java.util.Date lockTime;

    /** 执行锁持有者（实例/进程标识），用于故障定位和对账 */
    private String lockOwner;

    /** 统一执行结果上下文 JSONB：{actionType,status,objectKey,actionVersion,targetTable,affectedRows,output,...}，作为 Workflow 后续节点标准输入 */
    private String resultContext;

    /** 失败分类错误码（EXECUTE_ERROR/FUNCTION_ERROR/TRIGGER_SUBMIT_ERROR/STALE_EXECUTION_LOCK/版本或条件错误） */
    private String errorCode;

    /** 所属工作流运行ID（阶段二引入，预留） */
    private Long workflowRunId;

    /** 所属工作流步骤ID（阶段二引入，预留） */
    private Long workflowStepId;

    /** 触发本次执行的上游执行记录ID（动作链衔接，预留） */
    private Long parentExecutionId;

    @TableLogic
    private Integer delFlag;
}
