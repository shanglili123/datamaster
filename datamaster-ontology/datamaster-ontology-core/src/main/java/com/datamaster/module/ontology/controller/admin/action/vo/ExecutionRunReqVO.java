package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 执行已批准记录时可选覆盖执行参数（用于人工补选引用对象）。 */
@Data
@Schema(description = "执行已批准动作 Request VO")
public class ExecutionRunReqVO {
    @Schema(description = "人工补选后的执行参数 JSON；为空时沿用提交时参数")
    private String inputParams;
}
