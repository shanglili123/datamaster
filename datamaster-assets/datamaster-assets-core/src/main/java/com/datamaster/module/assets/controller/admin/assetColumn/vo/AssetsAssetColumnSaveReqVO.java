package com.datamaster.module.assets.controller.admin.assetColumn.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.Set;

/**
 *  / Request VO DA_ASSET_COLUMN
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = " Response VO")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AssetsAssetColumnSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "id", example = "")
    private Long assetId;

    @Schema(description = "/", example = "")
    @Size(max = 256, message = "/256")
    private String columnName;

    @Schema(description = "/", example = "")
    @Size(max = 256, message = "/256")
    private String columnComment;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String columnType;

    @Schema(description = "", example = "")
    private Long columnLength;

    @Schema(description = "", example = "")
    private Long columnScale;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String nullableFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String pkFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String defaultValue;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String dataElemCodeFlag;

    @Schema(description = "id", example = "")
    @Size(max = 256, message = "id256")
    private String dataElemCodeId;

    @Schema(description = "id", example = "")
    @Size(max = 256, message = "id256")
    private String sensitiveLevelId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String relDataElmeFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String relCleanFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String relAuditFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String description;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")

    /**
     * id
     */
    @TableField(exist = false)
    private Set<Long> elementId;

}
