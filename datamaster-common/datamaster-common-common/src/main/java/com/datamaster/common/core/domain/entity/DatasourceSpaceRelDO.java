package com.datamaster.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.datamaster.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Schema(description = "数据源与空间关系")
@Data
@TableName(value = "AST_DATASOURCE_SPACE_REL")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatasourceSpaceRelDO extends BaseEntity {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Schema(description = "空间ID")
    private Long spaceId;

    @Schema(description = "空间名称")
    @TableField(exist = false)
    private String spaceName;

    @Schema(description = "空间编码")
    private String spaceCode;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "数据源名称")
    @TableField(exist = false)
    private String datasourceName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否有效")
    private Boolean validFlag;

    @Schema(description = "是否已分配到数据加工")
    private Boolean dppAssigned;
}
