package com.datamaster.module.assets.dal.dataobject.assetchild.api;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * -API- DO  DA_ASSET_API_PARAM
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "AST_ASSET_API_PARAM")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_API_PARAM_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetApiParamDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** API id */
    private Long apiId;

    /** id */
    private Long parentId;

    /**  */
    private String name;

    /**  */
    private String type;

    /**  */
    private String requestFlag;

    /**  */
    private String columnType;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

    @Schema(description = "", example = "")
    private String defaultValue;
    @Schema(description = "", example = "")
    private String exampleValue;
    @Schema(description = "", example = "")
    private String description;

}
