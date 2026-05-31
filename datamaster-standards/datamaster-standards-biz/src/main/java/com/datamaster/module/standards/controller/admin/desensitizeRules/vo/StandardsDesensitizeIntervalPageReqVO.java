

package com.datamaster.module.standards.controller.admin.desensitizeRules.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 脱敏区间 Request VO 对象 STD_DESENSITIZE_INTERVAL
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Schema(description = "脱敏区间 Request VO")
@Data
public class StandardsDesensitizeIntervalPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "脱敏规则ID", example = "")
    private Long desensitizeRuleId;

    @Schema(description = "区间号", example = "")
    private Long intervalNo;

    @Schema(description = "起始值", example = "")
    private Long startNum;

    @Schema(description = "末尾值", example = "")
    private Long endNum;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;



}
