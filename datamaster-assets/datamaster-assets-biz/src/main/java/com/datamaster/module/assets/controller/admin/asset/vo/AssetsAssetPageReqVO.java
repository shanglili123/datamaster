package com.datamaster.module.assets.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.module.assets.dal.dataobject.discovery.AssetsDiscoveryTableDO;

import java.util.List;

/**
 *  Request VO  DA_ASSET
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = " Request VO")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AssetsAssetPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "", example = "")
    private String name;

    @Schema(description = "", example = "")
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

    @Schema(description = "", example = "")
    private String type;

    private List<String> themeIdList;

    @Schema(description = "01", example = "")
    private String sourceType;

    @Schema(description = "id", example = "")
    private String datasourceId;

    @Schema(description = "", example = "")
    private String tableName;

    @Schema(description = "", example = "")
    private String tableComment;

    @Schema(description = "", example = "")
    private Long dataCount;

    @Schema(description = "", example = "")
    private Long fieldCount;

    /** ;1:2: */
    @Schema(description = "", example = "")
    private String source;

    @Schema(description = "", example = "")
    private String status;

    @Schema(description = "", example = "")
    private String description;

    @Schema(description = "id", example = "")
    private Long projectId;

    @Schema(description = "", example = "")
    private String projectCode;

    @Schema(description = "id", example = "")
    private List<Long> assetIdList;

    //标签id
    private List<Long> tagIdList;

    @Schema(description = "id()", example = "")
    private List<Long> themeAssetIdList;

    @Schema(description = "", example = "")
    private String createType;
    /**
     *  AssetsDiscoveryTableDO  VO
     *
     * @param AssetsDiscoveryTableById
     */
    public AssetsAssetPageReqVO(AssetsDiscoveryTableDO AssetsDiscoveryTableById) {
        if (AssetsDiscoveryTableById != null) {
            // 这里将表名作为资产名称
//
    this.name = AssetsDiscoveryTableById.getTableComment();
            this.tableName = AssetsDiscoveryTableById.getTableName();
            this.tableComment = AssetsDiscoveryTableById.getTableComment();
            this.dataCount = AssetsDiscoveryTableById.getDataCount();
            this.fieldCount = AssetsDiscoveryTableById.getFieldCount();
            this.status = "1";
            // 可根据需要将表描述也赋值给描述字段
//
    this.description = AssetsDiscoveryTableById.getTableComment();
        }
    }
}
