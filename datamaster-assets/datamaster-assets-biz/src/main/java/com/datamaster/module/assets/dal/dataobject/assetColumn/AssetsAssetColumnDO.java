package com.datamaster.module.assets.dal.dataobject.assetColumn;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.annotation.Excel;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.module.standards.api.dataElem.dto.StandardsDataElemRuleRelRespDTO;
import com.datamaster.module.catalog.api.column.dto.CatalogColumnRespDTO;

import java.util.List;
import java.util.Set;

/**
 * DO  DA_ASSET_COLUMN * * @author lhs * @date 2025-01-21
 */

@Data
@TableName(value = "AST_ASSET_COLUMN")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_COLUMN_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetColumnDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
/**
     * id
     */
    private Long assetId;
    /**
     * /
     */
    private String columnName;
    /**
     * /
     */
    private String columnComment;
    /**
     *
     */
    private String columnType;
    /**
     *
     */
    private Long columnLength;
    /**
     *
     */
    private Long columnScale;
    /**
     *
     */
    private String nullableFlag;
    /**
     *
     */
    private String pkFlag;
    /**
     *
     */
    private String defaultValue;
    /**
     *
     */
    private String dataElemCodeFlag;
    /**
     * id
     */
    private Long dataElemCodeId;
    /**
     * id
     */
    private Long sensitiveLevelId;
    /**
     *
     */
    private String relDataElmeFlag;
    /**
     *
     */
    private String relCleanFlag;
    /**
     *
     */
    private String relAuditFlag;
    /**
     *
     */
    private String description;
    /**
     *
     */
    private Boolean validFlag;
    /**
     *
     */

    @TableLogic
    private Boolean delFlag;
    @TableField(exist = false)
    private Set<Long> elementId;
    @Excel(name = "")
    @Schema(description = "", example = "")
    @TableField(exist = false)
    private String relDataElmeName;
    @Excel(name = "")
    @Schema(description = "", example = "")
    @TableField(exist = false)
    private String sensitiveLevelName;
    @Excel(name = "")
    @Schema(description = "", example = "")
    @TableField(exist = false)
    private String dataElemCodeName;
    /**
     *
     */

    @TableField(exist = false)
    private List<StandardsDataElemRuleRelRespDTO> cleanRuleList;

    public AssetsAssetColumnDO(CatalogColumnRespDTO column) {
        if (column != null) {
            this.columnLength = (column.getColumnLength() != null) ? Long.valueOf(column.getColumnLength()) : null;
            this.columnName = column.getColumnName();
            this.columnComment = column.getColumnComment();
            this.columnType = column.getColumnType();
            this.columnScale = (column.getColumnScale() != null) ? Long.valueOf(column.getColumnScale()) : null;
            this.defaultValue = column.getDefaultValue();
            this.pkFlag = column.getPkFlag();
            this.nullableFlag = column.getNullableFlag();
        }
    }

    public DbColumn toDbColumn() {
        return DbColumn.builder().colName(this.columnName).colComment(this.columnComment).dataType(this.columnType).dataLength(String.valueOf(this.columnLength)).dataPrecision(String.valueOf(this.columnLength)).dataScale(String.valueOf(this.columnScale)).colKey(this.pkFlag.equals("1")).nullable(this.nullableFlag.equals("0")).build();
    }
}