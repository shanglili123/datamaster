package com.datamaster.module.ontology.controller.admin.property.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 本体属性分页查询 Request VO
 */
@Schema(description = "本体属性分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PropertyPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属概念ID", example = "1")
    private Long conceptId;

    @Schema(description = "属性名称，模糊匹配", example = "客户名称")
    private String name;

    @Schema(description = "属性编码，模糊匹配", example = "customer_name")
    private String code;

    @Schema(description = "数据类型：string/integer/decimal/date/boolean/text", example = "string")
    private String dataType;
}
