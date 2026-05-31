package com.datamaster.module.assets.dal.dataobject.daAssetApply;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;

import java.util.List;

/**
 *  DO  DA_ASSET_APPLY
 *
 * @author shu
 * @date 2025-03-19
 */
@Data
@TableName(value = "AST_ASSET_APPLY")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_APPLY_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetApplyDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long assetId;

    /**  */
    @TableField(exist = false)
    private String assetName;

    /**  */
    @TableField(exist = false)
    private String assetTableName;

    /**  */
    @TableField(exist = false)
    private String catAssetName;

    /**  */
    @TableField(exist = false)
    private String catAssetCode;

    /** 01 */
    private String sourceType;

    /** id */
    private Long projectId;

    /**  */
    @TableField(exist = false)
    private String projectName;

    /**  */
    private String projectCode;

    /**  */
    @TableField(exist = false)
    private String themeName;

    @TableField(exist = false)
    private List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList;

    /**  */
    private String applyReason;

    /**  */
    private String approvalReason;

    /**  */
    private String status;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

    /**  */
    @TableField(exist = false)
    private String datasourceName;

    /** ip */
    @TableField(exist = false)
    private String datasourceIp;

    /**  */
    @TableField(exist = false)
    private String datasourceType;

    /**  */
    @TableField(exist = false)
    private String description;

    /**  */
    @TableField(exist = false)
    private String phonenumber;

    @TableField(exist = false)
    private String type;

}
