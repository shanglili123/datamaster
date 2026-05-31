

package com.datamaster.module.modeling.controller.admin.dm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数仓分层-规范管理 Request VO 对象 Modeling_DATA_LAYER_SPECIFICATION
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "数仓分层-规范管理 Request VO")
@Data
public class ModelingDataLayerSpecificationPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "数仓分层ID", example = "")
    private Long dataLayerId;

    @Schema(description = "表前缀", example = "")
    private String prefixName;

    @Schema(description = "业务大类英文缩写", example = "")
    private String businessEngName;

    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Schema(description = "状态", example = "")
    private String status;

    @Schema(description = "描述", example = "")
    private String description;




}
