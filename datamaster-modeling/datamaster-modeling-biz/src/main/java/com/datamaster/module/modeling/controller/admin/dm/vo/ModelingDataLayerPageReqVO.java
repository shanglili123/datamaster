

package com.datamaster.module.modeling.controller.admin.dm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数仓分层管理 Request VO 对象 Modeling_DATA_LAYER
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "数仓分层管理 Request VO")
@Data
public class ModelingDataLayerPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "名称", example = "")
    private String name;

    @Schema(description = "英文缩写", example = "")
    private String engName;

    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Schema(description = "分类", example = "")
    private String category;

    @Schema(description = "描述", example = "")
    private String description;




}
