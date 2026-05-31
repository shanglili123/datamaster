

package com.datamaster.module.modeling.controller.admin.dm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数仓分层-规范管理 创建/修改 Request VO Modeling_DATA_LAYER_SPECIFICATION
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "数仓分层-规范管理 Response VO")
@Data
public class ModelingDataLayerSpecificationSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "数仓分层ID", example = "")
    private Long dataLayerId;

    @Schema(description = "表前缀", example = "")
    @Size(max = 256, message = "表前缀长度不能超过256个字符")
    private String prefixName;

    @Schema(description = "业务大类英文缩写", example = "")
    @Size(max = 256, message = "业务大类英文缩写长度不能超过256个字符")
    private String businessEngName;

    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Schema(description = "状态", example = "")
    @Size(max = 256, message = "状态长度不能超过256个字符")
    private String status;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;


}
