package com.datamaster.module.ontology.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * AI 动作决策请求。
 *
 * <p>AI 只负责从当前本体已存在的动作中选择动作并补全参数，不能创建动作、生成 SQL，
 * 也不能绕过动作原有的前置条件和审批配置。</p>
 */
@Schema(description = "AI 本体动作决策 Request VO")
@Data
public class AiActionDecisionReqVO {

    @Schema(description = "本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;

    @Schema(description = "用户的自然语言意图", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "订单已支付就发货，扣减订单关联商品库存并把订单状态改为已发货")
    @NotBlank(message = "动作意图不能为空")
    private String prompt;

    @Schema(description = "触发对象主键，可选", example = "ORD202609100001")
    private String objectKey;

    @Schema(description = "调用方已有的上下文参数 JSON", example = "{\"orderNo\":\"ORD202609100001\"}")
    private String inputParams;

    @Schema(description = "当前空间ID")
    private Long spaceId;

    @Schema(description = "当前空间编码")
    private String spaceCode;
}
