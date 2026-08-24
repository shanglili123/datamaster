package com.datamaster.module.ontology.controller.admin.function.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "函数分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FunctionPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
    @Schema(description = "本体ID")
    private Long ontologyId;
    @Schema(description = "函数名称")
    private String name;
    @Schema(description = "语言：TYPESCRIPT/PYTHON")
    private String lang;
}
