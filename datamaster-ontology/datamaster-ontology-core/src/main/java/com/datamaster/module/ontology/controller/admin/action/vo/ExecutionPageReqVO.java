package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(description = "执行记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "动作ID", example = "1")
    private Long actionId;

    /**
     * 动作ID集合（多选过滤），用于「按动作分页页内动作集合联动筛选执行记录」场景。
     * 与 actionId 共存时优先使用 actionIds（多选覆盖单选）；为空时退回 actionId 单选。
     */
    @Schema(description = "动作ID集合（多选），与 actionId 共存时优先使用", example = "[1,2,3]")
    private List<Long> actionIds;

    @Schema(description = "本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "状态", example = "PENDING_APPROVAL")
    private String status;
}
