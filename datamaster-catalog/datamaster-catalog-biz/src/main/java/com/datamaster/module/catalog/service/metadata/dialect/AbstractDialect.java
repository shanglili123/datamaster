package com.datamaster.module.catalog.service.metadata.dialect;

import lombok.extern.slf4j.Slf4j;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;

/**
 * 抽象数据库方言实现
 * 作为其他数据库类型的占位实现
 */
@Slf4j
public class AbstractDialect implements DatabaseDialect {

    @Override
    public String getStorageEngine(CatalogDbDO CatalogDbDO) {
        log.info("获取数据库存储引擎（占位实现）");
        return null;
    }

    @Override
    public Long getTableRowCount(CatalogDbDO CatalogDbDO, String tableName) {
        log.info("获取表 {} 的行数（占位实现）", tableName);
        return 0L;
    }

    @Override
    public String getTableIndexes(CatalogDbDO CatalogDbDO, String tableName) {
        log.info("获取表 {} 的索引信息（占位实现）", tableName);
        return "";
    }

    @Override
    public String getTablePartitionFields(CatalogDbDO CatalogDbDO, String tableName) {
        log.info("获取表 {} 的分区字段信息（占位实现）", tableName);
        return "";
    }

    @Override
    public boolean isColumnAutoIncrement(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        log.info("获取表 {} 字段 {} 的自增信息（占位实现）", tableName, columnName);
        return false;
    }

    @Override
    public boolean isPartitionField(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        log.info("判断表 {} 字段 {} 是否为分区字段（占位实现）", tableName, columnName);
        return false;
    }

    @Override
    public DbMetadata getDbMetadata(CatalogDbDO CatalogDbDO) {
        return null;
    }

    @Override
    public TableMetadata getTableMetadata(CatalogDbDO CatalogDbDO, String tableName) {
        log.info("批量获取表 {} 的元数据信息（占位实现）", tableName);
        TableMetadata metadata = new TableMetadata();
        metadata.setRowCount(getTableRowCount(CatalogDbDO, tableName));
        metadata.setIndexes(getTableIndexes(CatalogDbDO, tableName));
        metadata.setPartitionFields(getTablePartitionFields(CatalogDbDO, tableName));
        metadata.setTableSize(0); // 默认为0
        metadata.setStorageEngine(getStorageEngine(CatalogDbDO));
        return metadata;
    }

    @Override
    public ColumnMetadata getColumnMetadata(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        log.info("批量获取字段 {} 的元数据信息（占位实现）", columnName);
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.setAutoIncrement(isColumnAutoIncrement(CatalogDbDO, tableName, columnName));
        metadata.setPartitionField(isPartitionField(CatalogDbDO, tableName, columnName));
        return metadata;
    }
}
