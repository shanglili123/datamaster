package com.datamaster.module.ontology.controller.admin.function.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "函数执行分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FunctionExecPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
    @Schema(description = "函数ID")
    private Long functionId;
    @Schema(description = "状态")
    private String status;
}
