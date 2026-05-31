

package com.datamaster.module.standards.controller.admin.desensitizeRules.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 脱敏规则 Request VO 对象 STD_DESENSITIZE_RULE
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Schema(description = "脱敏规则 Request VO")
@Data
public class StandardsDesensitizeRulePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "分级名称", example = "")
    private String name;

    @Schema(description = "数据分类ID", example = "")
    private Long dataCategoryId;

    @Schema(description = "应用场景;1：数据资产  2：数据查询  3：数据服务", example = "")
    private String applicationScene;

    @Schema(description = "脱敏方式;1：底层脱敏  2：展示脱敏", example = "")
    private String maskType;

    @Schema(description = "替换规则", example = "")
    private String replaceRule;

    @Schema(description = "替换内容", example = "")
    private String replaceContent;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;



}
