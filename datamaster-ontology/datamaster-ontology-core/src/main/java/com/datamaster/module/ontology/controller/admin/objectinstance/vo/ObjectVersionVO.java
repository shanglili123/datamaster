package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 对象版本 VO —— 版本血缘的「快照时间线」条目。
 *
 * <p>
 * 由 ONT_ACTION_EXECUTION 的动作执行记录派生：每次执行的前/后数据快照
 * 构成对象某一行/批数据的一次变更版本。
 */
@Schema(description = "对象版本 VO（版本血缘快照时间线）")
@Data
public class ObjectVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "执行记录ID")
    private Long executionId;

    @Schema(description = "动作ID")
    private Long actionId;

    @Schema(description = "动作名称")
    private String actionName;

    @Schema(description = "动作类型：SELECT / CREATE / UPDATE / DELETE")
    private String actionType;

    @Schema(description = "执行状态：EXECUTED / FAILED / ROLLED_BACK 等")
    private String status;

    @Schema(description = "执行时间")
    private Date executeTime;

    @Schema(description = "执行前数据快照（JSON）")
    private Map<String, Object> beforeData;

    @Schema(description = "执行后数据快照（JSON）")
    private Map<String, Object> afterData;
}
