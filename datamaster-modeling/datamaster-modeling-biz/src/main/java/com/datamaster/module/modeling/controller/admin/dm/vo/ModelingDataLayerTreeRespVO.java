

package com.datamaster.module.modeling.controller.admin.dm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数仓分层管理 Response VO 对象 Modeling_DATA_LAYER
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "数仓分层管理 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelingDataLayerTreeRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    /** parentId */
    private Long parentId;

    @Excel(name = "名称")
    @Schema(description = "名称", example = "")
    private String name;

    @Excel(name = "英文缩写")
    @Schema(description = "英文缩写", example = "")
    private String engName;

    @Excel(name = "负责人ID")
    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Excel(name = "分类")
    @Schema(description = "分类", example = "")
    private String category;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "子列表", example = "")
    private List<ModelingDataLayerTreeRespVO> children;
}
