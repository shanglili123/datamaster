

package com.datamaster.module.standards.controller.admin.desensitizeList.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 脱敏清单关联关系 Request VO 对象 STD_DESENSITIZE_ASSETCOLUMN
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Schema(description = "脱敏清单关联关系 Request VO")
@Data
public class StandardsDesensitizeAssetcolumnPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "资产ID", example = "")
    private Long assetId;

    @Schema(description = "资产字段ID", example = "")
    private Long assetcolumnId;

    @Schema(description = "资产名称", example = "")
    private String assetName;

    @Schema(description = "数据分类ID", example = "")
    private Long dataCategoryId;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "脱敏规则ID", example = "")
    private Long ruleId;

}
