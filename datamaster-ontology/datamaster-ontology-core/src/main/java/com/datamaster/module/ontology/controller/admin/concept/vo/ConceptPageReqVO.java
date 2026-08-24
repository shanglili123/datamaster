package com.datamaster.module.ontology.controller.admin.concept.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 本体概念分页查询 Request VO
 */
@Schema(description = "本体概念分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ConceptPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "概念名称，模糊匹配", example = "客户")
    private String name;

    @Schema(description = "概念编码，模糊匹配", example = "customer")
    private String code;

    @Schema(description = "状态：0=草稿 1=已发布", example = "0")
    private Integer status;
}
