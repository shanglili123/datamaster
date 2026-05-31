package com.datamaster.module.assets.controller.admin.asset.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.database.core.FileInfo;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiParamSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.gis.vo.AssetsAssetGisSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;

import javax.validation.constraints.Size;
import java.util.List;

/**
 *  / Request VO DA_ASSET
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String name;

    @Schema(description = "", example = "")
    private String type;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String catCode;

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

    @Schema(description = "01", example = "")
    private String sourceType;

    private List<String> themeIdList;

    @Schema(description = "id", example = "")
    @Size(max = 256, message = "id256")
    private String datasourceId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String tableName;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String tableComment;

    @Schema(description = "", example = "")
    private Long dataCount;

    @Schema(description = "", example = "")
    private Long fieldCount;

    /** ;1:2: */
    @Schema(description = "", example = "")
    private String source;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String status;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String description;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

    //api
    @TableField(exist = false)
    private AssetsAssetApiSaveReqVO AssetsAssetApi;
    //api
    @TableField(exist = false)
    private List<AssetsAssetApiParamSaveReqVO> AssetsAssetApiParamList;

    /**
     *
     */
    @TableField(exist = false)
    private AssetsAssetGeoSaveReqVO AssetsAssetGeo;

    /**
     *
     */
    @TableField(exist = false)
    private AssetsAssetGisSaveReqVO AssetsAssetGis;

    /**
     *
     */
    @TableField(exist = false)
    private AssetsAssetVideoSaveReqVO AssetsAssetVideo;

    private FileInfo fileInfo;

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
}
