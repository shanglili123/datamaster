package com.datamaster.common.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

@Schema(description = "类目管理 创建/修改 Request VO")
@Data
public class CategorySaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "类目类型", example = "ASSET")
    private String catType;

    @Schema(description = "类别名称", example = "")
    @Size(max = 256, message = "类别名称长度不能超过256个字符")
    private String name;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "类别排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "有效状态", example = "")
    private Boolean validFlag;

    @Schema(description = "层级编码", example = "")
    @Size(max = 256, message = "层级编码长度不能超过256个字符")
    private String code;

    @Schema(description = "空间ID")
    private Long spaceId;

    @Schema(description = "空间编码")
    private String spaceCode;

}
