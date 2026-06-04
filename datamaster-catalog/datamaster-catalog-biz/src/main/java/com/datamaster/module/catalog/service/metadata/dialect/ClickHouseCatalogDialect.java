package com.datamaster.module.catalog.service.metadata.dialect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.datamaster.common.database.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

@Slf4j
public class ClickHouseCatalogDialect implements DatabaseDialect {

    @Override
    public String getStorageEngine(CatalogDbDO catalogDbDO) {
        return "ClickHouse";
    }

    @Override
    public Long getTableRowCount(CatalogDbDO catalogDbDO, String tableName) {
        try (Connection conn = getConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + qualifiedTableName(catalogDbDO, tableName))) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("获取ClickHouse表行数失败", e);
        }
        return 0L;
    }

    @Override
    public String getTableIndexes(CatalogDbDO catalogDbDO, String tableName) {
        return "";
    }

    @Override
    public String getTablePartitionFields(CatalogDbDO catalogDbDO, String tableName) {
        try (Connection conn = getConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT partition_key FROM system.tables WHERE database = '"
                     + dbName(catalogDbDO) + "' AND name = '" + tableName + "'")) {
            if (rs.next()) {
                return normalizeClickHouseExpression(rs.getString("partition_key"));
            }
        } catch (Exception e) {
            log.error("获取ClickHouse表分区字段失败", e);
        }
        return "";
    }

    @Override
    public boolean isColumnAutoIncrement(CatalogDbDO catalogDbDO, String tableName, String columnName) {
        return false;
    }

    @Override
    public boolean isPartitionField(CatalogDbDO catalogDbDO, String tableName, String columnName) {
        String partitionFields = getTablePartitionFields(catalogDbDO, tableName);
        return partitionFields != null && java.util.Arrays.asList(partitionFields.split("\\s*,\\s*")).contains(columnName);
    }

    @Override
    public DbMetadata getDbMetadata(CatalogDbDO catalogDbDO) {
        DbMetadata metadata = new DbMetadata();
        try (Connection conn = getConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sum(bytes_on_disk) / 1024 / 1024 AS storage_size "
                     + "FROM system.parts WHERE active = 1 AND database = '" + dbName(catalogDbDO) + "'")) {
            if (rs.next()) {
                metadata.setStorageSize(rs.getInt("storage_size"));
            }
        } catch (Exception e) {
            log.error("获取ClickHouse数据库元数据失败", e);
        }
        return metadata;
    }

    @Override
    public TableMetadata getTableMetadata(CatalogDbDO catalogDbDO, String tableName) {
        TableMetadata metadata = new TableMetadata();
        metadata.setRowCount(getTableRowCount(catalogDbDO, tableName));
        metadata.setIndexes("");
        metadata.setPartitionFields(getTablePartitionFields(catalogDbDO, tableName));
        metadata.setStorageEngine("ClickHouse");
        try (Connection conn = getConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT comment, engine, create_table_query FROM system.tables WHERE database = '"
                     + dbName(catalogDbDO) + "' AND name = '" + tableName + "'")) {
            if (rs.next()) {
                metadata.setTableComment(rs.getString("comment"));
                metadata.setStorageEngine(rs.getString("engine"));
                metadata.setPrimaryKey(extractPrimaryKey(rs.getString("create_table_query")));
            }
        } catch (Exception e) {
            log.error("获取ClickHouse表元数据失败", e);
        }
        try (Connection conn = getConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sum(bytes_on_disk) / 1024 / 1024 AS table_size "
                     + "FROM system.parts WHERE active = 1 AND database = '" + dbName(catalogDbDO) + "' AND table = '" + tableName + "'")) {
            if (rs.next()) {
                metadata.setTableSize(rs.getInt("table_size"));
            }
        } catch (Exception e) {
            log.error("获取ClickHouse表大小失败", e);
        }
        return metadata;
    }

    @Override
    public ColumnMetadata getColumnMetadata(CatalogDbDO catalogDbDO, String tableName, String columnName) {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.setAutoIncrement(false);
        metadata.setPartitionField(isPartitionField(catalogDbDO, tableName, columnName));
        try (Connection conn = getConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT is_in_primary_key FROM system.columns WHERE database = '"
                     + dbName(catalogDbDO) + "' AND table = '" + tableName + "' AND name = '" + columnName + "'")) {
            if (rs.next()) {
                metadata.setUnique(rs.getInt("is_in_primary_key") == 1);
            }
        } catch (Exception e) {
            log.error("获取ClickHouse字段元数据失败", e);
        }
        return metadata;
    }

    private Connection getConnection(CatalogDbDO catalogDbDO) throws Exception {
        Map<String, Object> config = parseDatasourceConfig(catalogDbDO.getDatasourceConfig());
        String url = "jdbc:clickhouse://" + catalogDbDO.getIp() + ":" + catalogDbDO.getPort() + "/" + dbName(catalogDbDO);
        String username = config == null ? null : (String) config.get("username");
        String password = config == null ? null : decryptPassword((String) config.get("password"));
        return DriverManager.getConnection(url, username, password);
    }

    private Map<String, Object> parseDatasourceConfig(String datasourceConfig) {
        if (StringUtils.isBlank(datasourceConfig)) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(datasourceConfig, Map.class);
        } catch (Exception e) {
            log.error("解析datasourceConfig失败", e);
            return null;
        }
    }

    private String dbName(CatalogDbDO catalogDbDO) {
        if (StringUtils.isNotBlank(catalogDbDO.getDbName())) {
            return catalogDbDO.getDbName();
        }
        Map<String, Object> config = parseDatasourceConfig(catalogDbDO.getDatasourceConfig());
        return config == null ? "" : String.valueOf(config.get("dbname"));
    }

    private String qualifiedTableName(CatalogDbDO catalogDbDO, String tableName) {
        return "`" + dbName(catalogDbDO) + "`.`" + tableName + "`";
    }

    private String decryptPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return password;
        }
        try {
            return AesEncryptUtil.desEncrypt(password).trim();
        } catch (Exception e) {
            return password;
        }
    }

    private String normalizeClickHouseExpression(String expression) {
        if (StringUtils.isBlank(expression)) {
            return "";
        }
        return expression.replaceAll("[` ]", "").replaceAll("[()]", "");
    }

    private String extractPrimaryKey(String createTableSql) {
        if (StringUtils.isBlank(createTableSql)) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("PRIMARY KEY\\s*\\(([^)]*)\\)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(createTableSql);
        return matcher.find() ? normalizeClickHouseExpression(matcher.group(1)) : "";
    }
}
