

package com.datamaster.module.standards.controller.admin.desensitizeList.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 脱敏清单关联关系 创建/修改 Request VO STD_DESENSITIZE_ASSETCOLUMN
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Schema(description = "脱敏清单关联关系 Response VO")
@Data
public class StandardsDesensitizeAssetcolumnSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "资产ID", example = "")
    private Long assetId;

    @Schema(description = "资产字段ID", example = "")
    private Long assetcolumnId;

    @Schema(description = "数据分类ID", example = "")
    private Long dataCategoryId;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;


}
