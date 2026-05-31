package com.datamaster.module.assets.controller.admin.assetchild.api.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * -API- / Request VO DA_ASSET_API_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Schema(description = "-API- Response VO")
@Data
public class AssetsAssetApiParamSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "API id", example = "")
    private Long apiId;

    @Schema(description = "id", example = "")
    private Long parentId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String name;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String type;

    @Schema(description = "", example = "")
    private String defaultValue;
    @Schema(description = "", example = "")
    private String exampleValue;
    @Schema(description = "", example = "")
    private String description;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String requestFlag;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String columnType;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

    @TableField(exist = false)
    private List<AssetsAssetApiParamSaveReqVO> AssetsAssetApiParamList;

}
