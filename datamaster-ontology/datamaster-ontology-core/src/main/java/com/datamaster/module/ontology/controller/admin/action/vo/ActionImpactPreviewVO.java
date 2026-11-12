package com.datamaster.module.ontology.controller.admin.action.vo;

import lombok.Data;

/**
 * 只读影响概要，不是执行授权或写入成功预测。无 SQL、原始参数、对象行或前置条件明细。
 * digest 只覆盖本 VO 的稳定业务字段；定义、对象、参数及空间绑定须由上层独立冻结/校验。
 */
@Data
public class ActionImpactPreviewVO {
    /** 协议版本，摘要算法变更时升级。 */
    private String schemaVersion = "1";
    /** AVAILABLE / UNKNOWN / BLOCKED。UNKNOWN 不能视作已验证影响。 */
    private String status;
    /** 固定原因码，不返回底层数据库异常。 */
    private String reason;
    /** MATCHED_ROWS / PLANNED_INSERT / NONE。不是实际 affectedRows。 */
    private String summaryType;
    /** 当前匹配数或计划插入数；UNKNOWN/BLOCKED 为 null。 */
    private Long rowCount;
    /** true 表示仅能断言匹配行数至少 rowCount。 */
    private boolean truncated;
    private int rowLimit = 1000;
    /** PASS / REJECT / UNKNOWN / NOT_EVALUATED；不包含 actual/value 等敏感明细。 */
    private String preconditionStatus;
    /** SHA-256；不包含时间戳，也不证明匹配对象集合/字段值未变化。 */
    private String digest;
    /** 本次观测，不锁定源数据；提交/执行前必须重新校验，时效由上层管理。 */
    private String consistency = "OBSERVATION_ONLY_RECHECK_REQUIRED";
}
