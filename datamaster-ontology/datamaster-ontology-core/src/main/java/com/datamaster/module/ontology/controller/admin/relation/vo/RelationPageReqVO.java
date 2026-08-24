package com.datamaster.module.ontology.controller.admin.relation.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 本体关系分页查询 Request VO
 */
@Schema(description = "本体关系分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RelationPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "关系名称，模糊匹配", example = "拥有订单")
    private String name;

    @Schema(description = "关系编码，模糊匹配", example = "has_order")
    private String code;

    @Schema(description = "关系类型：one_to_one/one_to_many/many_to_one/many_to_many", example = "one_to_many")
    private String relationType;
}
