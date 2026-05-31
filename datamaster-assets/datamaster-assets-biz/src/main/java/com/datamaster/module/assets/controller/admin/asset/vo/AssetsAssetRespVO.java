package com.datamaster.module.assets.controller.admin.asset.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.common.database.core.FileInfo;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.theme.vo.AssetsAssetThemeRelRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Response VO  DA_ASSET * * @author lhs * @date 2025-01-21
 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String name;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String type;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String catCode;
    private String catName;
    /**
     * ;1: 2: 3: 4:
     */
    @Schema(description = " 1: 2: 3: 4:")
    private String tableType;
    /**
     * id
     */
    @Schema(description = "id ")
    private Long dataLayerId;
    /**
     * id;
     */
    @Schema(description = "id ")
    private Long businessCategoryId;
    /**
     *
     */
    @Schema(description = " ")
    private String businessCategoryCode;
    /**
     * id;
     */
    @Schema(description = "id ")
    private Long dataDomainId;
    /**
     * id;
     */
    @Schema(description = "id ")
    private Long themeDomainId;
    /**
     *
     */
    @Schema(description = " ")
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
    @TableField(exist = false)
    private List<AssetsAssetThemeRelRespVO> AssetsAssetThemeRelList;
    @Excel(name = "01")
    @Schema(description = "01", example = "")
    private String sourceType;
    @Excel(name = "id")
    @Schema(description = "id", example = "")
    private Long datasourceId;
    @TableField(exist = false)
    private String datasourceName;
    @TableField(exist = false)
    private String datasourceIp;
    @TableField(exist = false)
    private String datasourceType;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String tableName;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String tableComment;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private Long dataCount;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private Long fieldCount;
    /**
     * ;1:2:
     */
    @Schema(description = "", example = "")
    private String source;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String status;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String description;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private Boolean validFlag;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private Boolean delFlag;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String createBy;
    @Excel(name = "id")
    @Schema(description = "id", example = "")
    private Long creatorId;
    @Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String updateBy;
    @Excel(name = "id")
    @Schema(description = "id", example = "")
    private Long updaterId;
    @Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    @Excel(name = "")
    @Schema(description = "", example = "")
    private String remark;
    //api
    @TableField(exist = false)
    private AssetsAssetApiRespVO AssetsAssetApi;
    //api
    // @TableField(exist = false)
    private List<AssetsAssetApiParamRespVO> AssetsAssetApiParamList;
    /**     * 矢量     */
    @TableField(exist = false)
    private AssetsAssetGeoRespVO AssetsAssetGeo;
    /**
     *
     */
    @TableField(exist = false)
    private AssetsAssetGisRespVO AssetsAssetGis;
    /**
     *
     */
    @TableField(exist = false)
    private AssetsAssetVideoRespVO AssetsAssetVideo;
    /**
     *
     */
    @TableField(exist = false)
    private AssetsAssetFilesSaveReqVO AssetsAssetFiles;
    @Schema(description = "id", example = "")
    private Long projectId;
    @Schema(description = "", example = "")
    private String projectCode;
    @Schema(description = "", example = "")
    private String createType;
    private List<String> tagIds;
    private List<String> tagNames;
    private String tags;
    private FileInfo fileInfo;
    @Schema(description = "")
    private String dataLayerName;
    @Schema(description = "")
    private String dataLayerEngName;
    @Schema(description = "")
    private String businessCategoryName;
    @Schema(description = "")
    private String businessCategoryEngName;
    @Schema(description = "")
    private String dataDomainName;
    @Schema(description = "")
    private String dataDomainEngName;
    @Schema(description = "")
    private String themeDomainName;
    @Schema(description = "")
    private String themeDomainEngName;
    @Schema(name = "")
    private String createUserPhoneNumber;
    @Schema(name = "")
    private String updateUserPhoneNumber;
}
