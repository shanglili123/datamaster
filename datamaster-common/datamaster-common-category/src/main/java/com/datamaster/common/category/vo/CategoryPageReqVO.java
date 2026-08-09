package com.datamaster.common.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

@Schema(description = "类目管理 Request VO")
@Data
public class CategoryPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "类目类型", example = "ASSET")
    private String catType;

    @Schema(description = "类别名称", example = "")
    private String name;

    @Schema(description = "层级编码", example = "")
    private String code;

    private Boolean validFlag;

    @Schema(description = "空间ID")
    private Long spaceId;

    @Schema(description = "空间编码")
    private String spaceCode;

}
