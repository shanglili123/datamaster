package com.datamaster.module.assets.dal.dataobject.discovery;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.utils.ConversionUtils;

/**
 *  DO  DA_DISCOVERY_COLUMN
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
@TableName(value = "AST_DISCOVERY_COLUMN")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_DISCOVERY_COLUMN_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsDiscoveryColumnDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long taskId;

    /** id */
    private Long tableId;

    /** / */
    private String columnName;

    /** / */
    private String columnComment;

    /**  */
    private String columnType;

    /**  */
    private Long columnLength;

    /**  */
    private Long columnScale;

    /**  */
    private String nullableFlag;

    /**  */
    private String pkFlag;

    /**  */
    private String defaultValue;

    /**  */
    private Boolean validFlag;

    /**  */
    @TableLogic
    private Boolean delFlag;

    public AssetsDiscoveryColumnDO(Long taskId, Long discoveryTableId, DbColumn column) {
        this.setTaskId(taskId);
        this.setTableId(discoveryTableId);
        this.setColumnName(column.getColName());
        this.setColumnComment(column.getColComment());
        this.setColumnType(column.getDataType());
        this.setColumnLength(ConversionUtils.getStringToLong(column.getDataLength()));
        this.setColumnScale(ConversionUtils.getStringToLong(column.getDataScale()));
        this.setPkFlag(column.getColKey() ? "1" : "0");
        this.setNullableFlag(column.getNullable() ? "1" : "0");
        this.setDefaultValue(column.getDataDefault());
    }

    public boolean isEqual(AssetsDiscoveryColumnDO other) {
        if (other == null) {
            return false;
        }

        return (this.columnName != null && this.columnName.equals(other.columnName) || (this.columnName == null && other.columnName == null)) &&
                (this.columnComment != null && this.columnComment.equals(other.columnComment) || (this.columnComment == null && other.columnComment == null)) &&
                (this.columnType != null && this.columnType.equals(other.columnType) || (this.columnType == null && other.columnType == null)) &&
                (this.columnLength != null && this.columnLength.equals(other.columnLength) || (this.columnLength == null && other.columnLength == null)) &&
                (this.columnScale != null && this.columnScale.equals(other.columnScale) || (this.columnScale == null && other.columnScale == null)) &&
                (this.nullableFlag != null && this.nullableFlag.equals(other.nullableFlag) || (this.nullableFlag == null && other.nullableFlag == null)) &&
                (this.pkFlag != null && this.pkFlag.equals(other.pkFlag) || (this.pkFlag == null && other.pkFlag == null)) &&
                (this.defaultValue != null && this.defaultValue.equals(other.defaultValue) || (this.defaultValue == null && other.defaultValue == null));
    }
}
