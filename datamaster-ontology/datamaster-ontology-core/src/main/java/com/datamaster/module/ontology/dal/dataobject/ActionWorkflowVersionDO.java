package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 已发布 Workflow 的不可变定义快照。 */
@Data
@TableName("ONT_ACTION_WORKFLOW_VERSION")
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowVersionDO extends BaseEntity {
    private Long workflowId;
    private Integer version;
    private String status;
    private String snapshotJson;
}
