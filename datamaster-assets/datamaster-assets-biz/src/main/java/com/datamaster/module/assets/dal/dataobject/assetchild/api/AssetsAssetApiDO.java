package com.datamaster.module.assets.dal.dataobject.assetchild.api;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * -API DO  DA_ASSET_API
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "AST_ASSET_API")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_API_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetApiDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long assetId;

    /** API */
    private String url;

    @Schema(description = "", example = "")
    private String developerName;

    @Schema(description = "", example = "")
    private String appName;

    /**  */
    private String httpMethod;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
