package com.datamaster.module.assets.service.assetchild.operate.enums;

/**
 * 数据资产操作申请的审批与执行状态。
 *
 * <p>状态独立于历史 executeFlag 字段：老数据仍按 0/1 兼容，新增申请使用本枚举。</p>
 */
public final class AssetOperateApplyStatus {

    private AssetOperateApplyStatus() {
    }

    /** 草稿，可继续修改。 */
    public static final String DRAFT = "DRAFT";
    /** 已提交，等待审批。 */
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    /** 审批通过，等待执行。 */
    public static final String APPROVED = "APPROVED";
    /** 审批驳回，可修改后重新提交。 */
    public static final String REJECTED = "REJECTED";
    /** 正在执行。 */
    public static final String EXECUTING = "EXECUTING";
    /** 执行成功。 */
    public static final String EXECUTED = "EXECUTED";
    /** 执行失败，可在修正后重新提交。 */
    public static final String FAILED = "FAILED";
}
