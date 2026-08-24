package com.datamaster.module.ontology.controller.admin.concepttable.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "概念表绑定分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ConceptTablePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "概念ID", example = "1")
    private Long conceptId;
}
