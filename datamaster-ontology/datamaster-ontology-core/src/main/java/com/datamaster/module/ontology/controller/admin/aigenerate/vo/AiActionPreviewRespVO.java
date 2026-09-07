package com.datamaster.module.ontology.controller.admin.aigenerate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 生成动作预览 Response VO
 */
@Schema(description = "AI 生成动作预览 Response VO")
@Data
public class AiActionPreviewRespVO {

    @Schema(description = "AI 生成的动作预览列表")
    private List<GeneratedActionPreview> actions;

    @Schema(description = "quality warning（AI 调用失败等提示）")
    private String qualityWarning;

    /**
     * 单个生成动作预览
     */
    @Schema(description = "生成的动作预览")
    @Data
    public static class GeneratedActionPreview {

        @Schema(description = "动作名称", example = "扣减库存")
        private String actionName;

        @Schema(description = "动作描述", example = "检查库存充足后扣减库存数量")
        private String actionDescription;

        @Schema(description = "动作类型：COMPOSITE/FUNCTION", example = "COMPOSITE")
        private String actionType;

        @Schema(description = "动作触发概念编码", example = "person")
        private String triggerConceptCode;

        // ===== COMPOSITE 类型专属字段 =====

        @Schema(description = "多步骤执行步骤 JSON 数组（COMPOSITE 时返回，与 ActionSaveReqVO.executionSteps 格式一致）")
        private String executionSteps;

        // ===== FUNCTION 类型专属字段 =====

        @Schema(description = "函数代码体（FUNCTION 时返回）")
        private String functionBody;

        @Schema(description = "函数语言：TYPESCRIPT/PYTHON（FUNCTION 时返回）", example = "TYPESCRIPT")
        private String functionLang;

        @Schema(description = "函数参数声明 JSON 数组（FUNCTION 时返回）", example = "[\"name\",\"amount\"]")
        private String functionParams;

        @Schema(description = "函数数据来源概念编码（FUNCTION 时返回）", example = "inventory")
        private String sourceConceptCode;

        @Schema(description = "函数输出目标概念编码（FUNCTION 时返回）", example = "inventory")
        private String outputConceptCode;
    }
}
