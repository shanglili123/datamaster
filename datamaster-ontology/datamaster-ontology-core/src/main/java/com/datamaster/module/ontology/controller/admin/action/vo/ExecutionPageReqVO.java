package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "执行记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "动作ID", example = "1")
    private Long actionId;

    @Schema(description = "本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "状态", example = "PENDING_APPROVAL")
    private String status;
}
