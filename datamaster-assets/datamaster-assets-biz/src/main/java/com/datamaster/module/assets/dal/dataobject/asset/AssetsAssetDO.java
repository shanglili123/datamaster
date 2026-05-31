package com.datamaster.module.assets.dal.dataobject.asset;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;

import java.util.List;

/**
 *  DO  DA_ASSET
 *
 * @author lhs
 * @date 2025-01-21
 */
@Data
@TableName(value = "AST_ASSET")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**  */
    private String name;

    @Schema(description = "", example = "")
    private String type;

    /**  */
    private String catCode;

    @TableField(exist = false)
    private String catName;

    /**
     * ;1: 2: 3: 4:
     */
    @Schema(name = " 1: 2: 3: 4:")
    private String tableType;
    /**
     * id
     */
    @Schema(name = "id ")
    private Long dataLayerId;
    /**
     * id;
     */
    @Schema(name = "id ")
    private Long businessCategoryId;
    /**
     *
     */
    @Schema(name = " ")
    private String businessCategoryCode;
    /**
     * id;
     */
    @Schema(name = "id ")
    private Long dataDomainId;
    /**
     * id;
     */
    @Schema(name = "id ")
    private Long themeDomainId;
    /**
     *
     */
    @Schema(name = " ")
    private String themeDomainCode;
    /**
     * ;1 2
     */
    @Schema(description = " 1 2")
    private String tableCase;

    /**
     * id
     */
    @Schema(description = "id")
    private Long tableId;

    /**01 */
    @TableField(exist = false)
    private String sourceType;

    @TableField(exist = false)
    private List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList;

    /** id */
    private Long datasourceId;

    @TableField(exist = false)
    private String datasourceName;

    @TableField(exist = false)
    private String datasourceIp;

    @TableField(exist = false)
    private String datasourceType;

    /**  */
    private String tableName;

    /**  */
    private String tableComment;

    /**  */
    private Long dataCount;

    /**  */
    private Long fieldCount;

    /** ;1:2: */
    private String source;

    /**  */
    private String status;

    /**  */
    private String description;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

    //api
    @TableField(exist = false)
    private AssetsAssetApiRespVO AssetsAssetApi;
//    //api
//
    @TableField(exist = false)
//
    private List<AssetsAssetApiParamRespVO> AssetsAssetApiParamList;
//
//    /**
//     * 矢量
//     */
//
    @TableField(exist = false)
//
    private AssetsAssetGeoRespVO AssetsAssetGeo;
//
//    /**
//     * 地理空间服务
//     */
//
    @TableField(exist = false)
//
    private AssetsAssetGisRespVO AssetsAssetGis;
//
//    /**
//     * 视频数据
//     */
//
    @TableField(exist = false)
//
    private AssetsAssetVideoRespVO AssetsAssetVideo;

    @Schema(description = "id", example = "")
    @TableField(exist = false)
    private Long projectId;

    @Schema(description = "", example = "")
    @TableField(exist = false)
    private String projectCode;
    /**  */
    private String createType;

    @TableField(exist = false)
    private String tags;

    @Schema(name = "")
    @TableField(exist = false)
    private String dataLayerName;

    @Schema(name = "")
    @TableField(exist = false)
    private String dataLayerEngName;

    @Schema(name = "")
    @TableField(exist = false)
    private String businessCategoryName;

    @Schema(name = "")
    @TableField(exist = false)
    private String businessCategoryEngName;

    @Schema(name = "")
    @TableField(exist = false)
    private String dataDomainName;

    @Schema(name = "")
    @TableField(exist = false)
    private String dataDomainEngName;

    @Schema(name = "")
    @TableField(exist = false)
    private String themeDomainName;

    @Schema(name = "")
    @TableField(exist = false)
    private String themeDomainEngName;

    @Schema(name = "")
    @TableField(exist = false)
    private String createUserPhoneNumber;

    @Schema(name = "")
    @TableField(exist = false)
    private String updateUserPhoneNumber;
}
