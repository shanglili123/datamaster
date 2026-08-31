package com.datamaster.module.ontology.controller.admin.function.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Schema(description = "函数保存 Request VO")
@Data
public class FunctionSaveReqVO {
    @Schema(description = "编号")
    private Long id;
    @Schema(description = "本体ID（共享函数可空）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long ontologyId;
    @Schema(description = "函数名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "函数名称不能为空")
    private String name;
    @Schema(description = "函数编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "函数编码不能为空")
    private String code;
    @Schema(description = "语言：TYPESCRIPT/PYTHON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "语言不能为空")
    private String lang;
    @Schema(description = "函数代码体", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "代码体不能为空")
    private String body;
    @Schema(description = "参数声明(JSON数组)，如 [\"name\",\"amount\"]；绑定主概念后为属性 code 列表")
    private String params;
    @Schema(description = "数据来源主概念ID（可空）")
    private Long sourceConceptId;
    @Schema(description = "可选关联关系ID JSON数组（可空）")
    private String sourceRelationIds;
    @Schema(description = "输出目标概念ID（可空）")
    private Long outputConceptId;
    @Schema(description = "主概念/关系数据读取行数上限（默认 5000）")
    private Integer readLimit;
    @Schema(description = "是否需要审批")
    private Boolean needsApproval;
    @Schema(description = "描述")
    private String description;
}
