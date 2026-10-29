package com.datamaster.module.collector.dal.dataobject.etl;

import lombok.Data;

import java.util.Date;

/** 现有流程实例的运行资格查询投影；不映射独立run表。 */
@Data
public class CollectorEtlRunDO {

    /** Existing local workflow-instance ID, not a separate run-table primary key. */
    private Long id;
    private Long taskId;
    private Long workflowInstanceId;
    private Long workflowCode;
    /** 本次运行的所有权令牌，重试须复用，不可根据租约过期强制覆盖。 */
    private String ownerToken;
    /** ACTIVE / UNCERTAIN / PAUSED / SUCCESS / FAILURE / STOP。 */
    private String status;
    /** 已规范化的资源键 JSON 快照，不包含连接凭据。 */
    private String resourcesJson;
    /** 仅用于观测/恢复判断，不是资源锁 TTL。 */
    private Date leaseUpdatedAt;
    private Date createTime;
    private Date updateTime;
}
