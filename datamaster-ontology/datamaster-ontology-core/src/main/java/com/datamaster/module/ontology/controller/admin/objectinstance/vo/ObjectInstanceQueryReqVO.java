package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对象实例分页查询 Request VO
 *
 * 查询对象集（概念 + 物理表绑定）下的对象实例。
 * 过滤为类型化结构化 JSON（字段+运算符+与或非逻辑组+排序+投影+关键字，见 service.query 包），
 * 列名须命中表元数据白名单防注入，值走 NamedParameter 占位符。
 */
@Schema(description = "对象实例分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ObjectInstanceQueryReqVO extends PageParam {

    @Schema(description = "本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "概念ID（对象类型ID）", example = "1")
    private Long conceptId;

    @Schema(description = "表绑定ID（ONT_CONCEPT_TABLE.id），为空时取概念第一张绑定表", example = "1")
    private Long tableBindingId;

    @Schema(description = "过滤条件 JSON（类型化结构化：groups[connector/negate/filters[field/op/value(s)]] + orderBy + columns + keyword），"
            + "列名须命中表元数据白名单防注入",
            example = "{\"groups\":[{\"connector\":\"AND\",\"filters\":[{\"field\":\"name\",\"op\":\"like\",\"value\":\"张\"}]}],"
            + "\"orderBy\":[{\"field\":\"id\",\"dir\":\"desc\"}],\"columns\":[\"id\",\"name\"]}")
    private String filters;

    /** 当前空间ID：由前端拦截器自动注入，用于资产表级权限校验 */
    @Schema(description = "当前空间ID（前端自动注入）", example = "1")
    private Long spaceId;

    /** 当前空间编码：由前端拦截器自动注入 */
    @Schema(description = "当前空间编码（前端自动注入）", example = "space_demo")
    private String spaceCode;
}
