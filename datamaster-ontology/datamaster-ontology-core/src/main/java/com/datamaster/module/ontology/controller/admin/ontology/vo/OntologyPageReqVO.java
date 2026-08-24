package com.datamaster.module.ontology.controller.admin.ontology.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 本体分页查询 Request VO
 */
@Schema(description = "本体分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class OntologyPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "本体名称，模糊匹配", example = "客户本体")
    private String name;

    @Schema(description = "本体编码，模糊匹配", example = "customer")
    private String code;

    @Schema(description = "状态：0=草稿 1=已发布 2=已归档", example = "0")
    private Integer status;
}
