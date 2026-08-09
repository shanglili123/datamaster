package com.datamaster.module.assets.dal.dataobject.assetchild.video;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * - DO  DA_ASSET_VIDEO
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Data
@TableName(value = "AST_ASSET_VIDEO")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_VIDEO_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetVideoDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long assetId;

    /** IP */
    private String ip;

    /**  */
    private Long port;

    /**  */
    private String protocol;

    /**  */
    private String platform;

    /** JSON */
    private String config;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

}
