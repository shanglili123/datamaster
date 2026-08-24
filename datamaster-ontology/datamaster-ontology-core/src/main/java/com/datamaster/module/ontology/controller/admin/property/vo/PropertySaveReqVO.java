package com.datamaster.module.ontology.controller.admin.property.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 本体属性保存 Request VO
 */
@Schema(description = "本体属性保存 Request VO")
@Data
public class PropertySaveReqVO {

    @Schema(description = "编号，更新时必填", example = "1")
    private Long id;

    @Schema(description = "所属概念ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属概念不能为空")
    private Long conceptId;

    @Schema(description = "属性名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户名称")
    @NotBlank(message = "属性名称不能为空")
    @Size(max = 100, message = "属性名称长度不能超过100个字符")
    private String name;

    @Schema(description = "属性编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "customer_name")
    @NotBlank(message = "属性编码不能为空")
    @Size(max = 100, message = "属性编码长度不能超过100个字符")
    private String code;

    @Schema(description = "数据类型：string/integer/decimal/date/boolean/text", example = "string")
    @Size(max = 50, message = "数据类型长度不能超过50个字符")
    private String dataType;

    @Schema(description = "描述", example = "客户全称")
    private String description;

    @Schema(description = "是否主键属性", example = "false")
    private Boolean isPrimary;

    @Schema(description = "是否必填", example = "false")
    private Boolean isRequired;

    @Schema(description = "默认值", example = "未知")
    @Size(max = 500, message = "默认值长度不能超过500个字符")
    private String defaultValue;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;
}
