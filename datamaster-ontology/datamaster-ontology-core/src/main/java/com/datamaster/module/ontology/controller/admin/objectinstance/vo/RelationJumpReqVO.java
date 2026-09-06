package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 跨对象关系跳转 Request VO（档位 B：同页内嵌展开关联对象）
 *
 * 沿关系字段绑定（RelationColumnDO.sourceColumn → targetColumn）在目标概念物理表上做值过滤查询，
 * 返回目标对象实例。源关联值来自源对象实例行上 sourceColumn 的取值。
 *
 * @author datamaster
 */
@Schema(description = "跨对象关系跳转 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RelationJumpReqVO extends PageParam {

    @Schema(description = "源概念ID（当前所在对象类型ID）", example = "1")
    private Long sourceConceptId;

    @Schema(description = "源表绑定ID（ONT_CONCEPT_TABLE.id）", example = "1")
    private Long sourceTableBindingId;

    @Schema(description = "关系ID（ONT_RELATION.id），用于解析关联字段绑定", example = "1")
    private Long relationId;

    @Schema(description = "源关联列值（源对象行上 sourceColumn 的取值，可能多个）")
    private List<Object> sourceValues;

    @Schema(description = "目标过滤条件 JSON（类型化结构化，可选，进一步收敛目标对象）",
            example = "{\"groups\":[{\"connector\":\"AND\",\"filters\":[{\"field\":\"status\",\"op\":\"eq\",\"value\":\"1\"}]}]}")
    private String filters;

    /** 当前空间ID：由前端拦截器自动注入，用于资产表级权限校验 */
    @Schema(description = "当前空间ID（前端自动注入）", example = "1")
    private Long spaceId;

    /** 当前空间编码：由前端拦截器自动注入 */
    @Schema(description = "当前空间编码（前端自动注入）", example = "space_demo")
    private String spaceCode;
}
