package com.datamaster.module.catalog.service.metadata.dialect;

import com.datamaster.common.database.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * SQL Server数据库方言实现
 */
@Slf4j
public class SqlServerDialect implements DatabaseDialect {

    @Override
    public String getStorageEngine(CatalogDbDO CatalogDbDO) {
        return "SQL Server";
    }

    @Override
    public Long getTableRowCount(CatalogDbDO CatalogDbDO, String tableName) {
        try (Connection conn = getConnection(CatalogDbDO);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + qualifiedTableName(CatalogDbDO, tableName));
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("获取SQL Server表行数失败", e);
        }
        return 0L;
    }

    @Override
    public String getTableIndexes(CatalogDbDO CatalogDbDO, String tableName) {
        try (Connection conn = getConnection(CatalogDbDO);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT i.name FROM sys.indexes i "
                    + "JOIN sys.tables t ON t.object_id = i.object_id "
                    + "JOIN sys.schemas s ON s.schema_id = t.schema_id "
                    + "WHERE s.name = '" + schemaName(CatalogDbDO) + "' AND t.name = '" + tableName + "' "
                    + "AND i.is_primary_key = 0 AND i.name IS NOT NULL";
            ResultSet rs = stmt.executeQuery(sql);
            StringBuilder indexes = new StringBuilder();
            while (rs.next()) {
                if (indexes.length() > 0) {
                    indexes.append(", ");
                }
                indexes.append(rs.getString("name"));
            }
            return indexes.toString();
        } catch (Exception e) {
            log.error("获取SQL Server表索引信息失败", e);
        }
        return "";
    }

    @Override
    public String getTablePartitionFields(CatalogDbDO CatalogDbDO, String tableName) {
        try (Connection conn = getConnection(CatalogDbDO);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT DISTINCT c.name FROM sys.indexes i "
                    + "JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id "
                    + "JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id "
                    + "JOIN sys.tables t ON t.object_id = i.object_id "
                    + "JOIN sys.schemas s ON s.schema_id = t.schema_id "
                    + "WHERE s.name = '" + schemaName(CatalogDbDO) + "' AND t.name = '" + tableName + "' "
                    + "AND i.data_space_id IN (SELECT data_space_id FROM sys.partition_schemes)";
            ResultSet rs = stmt.executeQuery(sql);
            StringBuilder fields = new StringBuilder();
            while (rs.next()) {
                if (fields.length() > 0) {
                    fields.append(", ");
                }
                fields.append(rs.getString("name"));
            }
            return fields.toString();
        } catch (Exception e) {
            log.error("获取SQL Server表分区字段信息失败", e);
        }
        return "";
    }

    @Override
    public boolean isColumnAutoIncrement(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        try (Connection conn = getConnection(CatalogDbDO);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT COLUMNPROPERTY(OBJECT_ID('" + qualifiedTableName(CatalogDbDO, tableName) + "'), '"
                    + columnName + "', 'IsIdentity') AS is_identity";
            ResultSet rs = stmt.executeQuery(sql);
            return rs.next() && rs.getInt("is_identity") == 1;
        } catch (Exception e) {
            log.error("获取SQL Server字段自增信息失败", e);
        }
        return false;
    }

    @Override
    public boolean isPartitionField(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        String partitionFields = getTablePartitionFields(CatalogDbDO, tableName);
        if (StringUtils.isBlank(partitionFields)) {
            return false;
        }
        for (String field : partitionFields.split(",")) {
            if (columnName.equalsIgnoreCase(field.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DbMetadata getDbMetadata(CatalogDbDO CatalogDbDO) {
        DbMetadata metadata = new DbMetadata();
        try (Connection conn = getConnection(CatalogDbDO);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT SUM(size) * 8 / 1024 AS total_size_mb FROM sys.database_files";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                metadata.setStorageSize(rs.getInt("total_size_mb"));
            }
        } catch (Exception e) {
            log.error("批量获取SQL Server数据库元数据失败", e);
        }
        return metadata;
    }

    @Override
    public TableMetadata getTableMetadata(CatalogDbDO CatalogDbDO, String tableName) {
        TableMetadata metadata = new TableMetadata();
        metadata.setRowCount(getTableRowCount(CatalogDbDO, tableName));
        metadata.setIndexes(getTableIndexes(CatalogDbDO, tableName));
        metadata.setPartitionFields(getTablePartitionFields(CatalogDbDO, tableName));
        metadata.setStorageEngine(getStorageEngine(CatalogDbDO));

        try (Connection conn = getConnection(CatalogDbDO);
             Statement stmt = conn.createStatement()) {
            String tableSql = "SELECT CAST(SUM(a.total_pages) * 8 * 1024 AS BIGINT) AS table_size, "
                    + "CAST(ep.value AS NVARCHAR(4000)) AS table_comment "
                    + "FROM sys.tables t "
                    + "JOIN sys.schemas s ON s.schema_id = t.schema_id "
                    + "LEFT JOIN sys.indexes i ON i.object_id = t.object_id "
                    + "LEFT JOIN sys.partitions p ON p.object_id = i.object_id AND p.index_id = i.index_id "
                    + "LEFT JOIN sys.allocation_units a ON a.container_id = p.partition_id "
                    + "LEFT JOIN sys.extended_properties ep ON ep.major_id = t.object_id AND ep.minor_id = 0 AND ep.name = 'MS_Description' "
                    + "WHERE s.name = '" + schemaName(CatalogDbDO) + "' AND t.name = '" + tableName + "' "
                    + "GROUP BY ep.value";
            ResultSet tableRs = stmt.executeQuery(tableSql);
            if (tableRs.next()) {
                metadata.setTableSize(tableRs.getInt("table_size"));
                metadata.setTableComment(tableRs.getString("table_comment"));
            }

            String pkSql = "SELECT c.name FROM sys.indexes i "
                    + "JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id "
                    + "JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id "
                    + "JOIN sys.tables t ON t.object_id = i.object_id "
                    + "JOIN sys.schemas s ON s.schema_id = t.schema_id "
                    + "WHERE i.is_primary_key = 1 AND s.name = '" + schemaName(CatalogDbDO) + "' "
                    + "AND t.name = '" + tableName + "' ORDER BY ic.key_ordinal";
            ResultSet pkRs = stmt.executeQuery(pkSql);
            StringBuilder primaryKeys = new StringBuilder();
            while (pkRs.next()) {
                if (primaryKeys.length() > 0) {
                    primaryKeys.append(", ");
                }
                primaryKeys.append(pkRs.getString("name"));
            }
            metadata.setPrimaryKey(primaryKeys.toString());
        } catch (Exception e) {
            log.error("批量获取SQL Server表元数据失败", e);
        }
        return metadata;
    }

    @Override
    public ColumnMetadata getColumnMetadata(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.setAutoIncrement(isColumnAutoIncrement(CatalogDbDO, tableName, columnName));
        metadata.setPartitionField(isPartitionField(CatalogDbDO, tableName, columnName));
        metadata.setUnique(isColumnUnique(CatalogDbDO, tableName, columnName));
        return metadata;
    }

    private boolean isColumnUnique(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        try (Connection conn = getConnection(CatalogDbDO)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getIndexInfo(null, schemaName(CatalogDbDO), tableName, true, false);
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("判断SQL Server字段唯一性失败", e);
        }
        return false;
    }

    private Connection getConnection(CatalogDbDO CatalogDbDO) throws Exception {
        Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
        if (configMap == null) {
            throw new IllegalStateException("数据源配置为空");
        }
        String dbName = StringUtils.isNotBlank(CatalogDbDO.getDbName())
                ? CatalogDbDO.getDbName()
                : String.valueOf(configMap.get("dbname"));
        String url = "jdbc:sqlserver://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort()
                + ";databaseName=" + dbName + ";encrypt=false;trustServerCertificate=true";
        String username = (String) configMap.get("username");
        String password = decryptPassword((String) configMap.get("password"));
        return DriverManager.getConnection(url, username, password);
    }

    private Map<String, Object> parseDatasourceConfig(String datasourceConfig) {
        if (StringUtils.isBlank(datasourceConfig)) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(datasourceConfig, Map.class);
        } catch (Exception e) {
            log.error("解析datasourceConfig失败", e);
            return null;
        }
    }

    private String qualifiedTableName(CatalogDbDO CatalogDbDO, String tableName) {
        return schemaName(CatalogDbDO) + "." + tableName;
    }

    private String schemaName(CatalogDbDO CatalogDbDO) {
        return StringUtils.isNotBlank(CatalogDbDO.getSchemaName()) ? CatalogDbDO.getSchemaName() : "dbo";
    }

    private String decryptPassword(String password) {
        try {
            return AesEncryptUtil.desEncrypt(password).trim();
        } catch (Exception e) {
            log.error("解密密码失败", e);
            return password;
        }
    }
}
