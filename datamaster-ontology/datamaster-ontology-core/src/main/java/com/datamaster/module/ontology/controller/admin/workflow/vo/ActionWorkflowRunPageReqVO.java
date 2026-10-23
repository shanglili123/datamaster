package com.datamaster.module.ontology.controller.admin.workflow.vo;

import com.datamaster.common.core.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Workflow 运行记录分页和筛选参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowRunPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;

    public ActionWorkflowRunPageReqVO() {
        setPageNum(1);
        setPageSize(10);
    }

    /** QUEUED/RUNNING/WAITING_APPROVAL/EXECUTED/FAILED/CANCELED 等运行状态。 */
    private String status;

    /** 精确匹配 Workflow 幂等键，便于排查重复触发。 */
    private String idempotencyKey;
}
