package com.datamaster.module.catalog.service.metadata.dialect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.datamaster.common.database.utils.AesEncryptUtil;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * PostgreSQL数据库方言实现
 */
@Slf4j
public class PostgreSqlDialect implements DatabaseDialect {
    @Override
    public ColumnMetadata getColumnMetadata(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.setAutoIncrement(isColumnAutoIncrement(CatalogDbDO, tableName, columnName));
        metadata.setPartitionField(isPartitionField(CatalogDbDO, tableName, columnName));
        metadata.setUnique(isColumnUnique(CatalogDbDO, tableName, columnName));
        return metadata;
    }

    @Override
    public TableMetadata getTableMetadata(CatalogDbDO CatalogDbDO, String tableName) {
        TableMetadata metadata = new TableMetadata();
        metadata.setRowCount(getTableRowCount(CatalogDbDO, tableName));
        metadata.setIndexes(getTableIndexes(CatalogDbDO, tableName));
        metadata.setPartitionFields(getTablePartitionFields(CatalogDbDO, tableName));
        metadata.setStorageEngine(getStorageEngine(CatalogDbDO));
        fillTableMetadata(CatalogDbDO, tableName, metadata);
        return metadata;
    }

    @Override
    public String getStorageEngine(CatalogDbDO CatalogDbDO) {
        try {
            // PostgreSQL使用表空间，这里返回PostgreSQL
            return "PostgreSQL";
        } catch (Exception e) {
            log.error("获取PostgreSQL存储引擎失败", e);
            return null;
        }
    }

    @Override
    public Long getTableRowCount(CatalogDbDO CatalogDbDO, String tableName) {
        try {
            // 解析datasourceConfig获取连接信息
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return 0L;
            }

            // 构建连接字符串
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT COUNT(*) FROM " + qualifiedTableName(CatalogDbDO, tableName);
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            log.error("获取PostgreSQL表行数失败", e);
        }
        return 0L;
    }

    @Override
    public String getTableIndexes(CatalogDbDO CatalogDbDO, String tableName) {
        try {
            // 解析datasourceConfig获取连接信息
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return "";
            }

            // 构建连接字符串
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT indexname FROM pg_indexes WHERE schemaname = '" + schemaName(CatalogDbDO) + "' "
                        + "AND tablename = '" + tableName + "' AND indexname NOT LIKE 'pk_%'";
                ResultSet rs = stmt.executeQuery(sql);
                StringBuilder indexes = new StringBuilder();
                while (rs.next()) {
                    String indexName = rs.getString("indexname");
                    if (indexes.length() > 0) {
                        indexes.append(", ");
                    }
                    indexes.append(indexName);
                }
                return indexes.toString();
            }
        } catch (Exception e) {
            log.error("获取PostgreSQL表索引信息失败", e);
        }
        return "";
    }

    @Override
    public String getTablePartitionFields(CatalogDbDO CatalogDbDO, String tableName) {
        try {
            // 解析datasourceConfig获取连接信息
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return "";
            }

            // 构建连接字符串
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT a.attname AS column_name FROM pg_partitioned_table pt "
                        + "JOIN pg_class c ON c.oid = pt.partrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(pt.partattrs) "
                        + "WHERE n.nspname = '" + schemaName(CatalogDbDO) + "' AND c.relname = '" + tableName + "'";
                ResultSet rs = stmt.executeQuery(sql);
                StringBuilder partitionFields = new StringBuilder();
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    if (partitionFields.length() > 0) {
                        partitionFields.append(", ");
                    }
                    partitionFields.append(columnName);
                }
                return partitionFields.toString();
            }
        } catch (Exception e) {
            log.error("获取PostgreSQL表分区字段信息失败", e);
        }
        return "";
    }

    @Override
    public boolean isColumnAutoIncrement(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        try {
            // 解析datasourceConfig获取连接信息
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return false;
            }

            // 构建连接字符串
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT column_default FROM information_schema.columns WHERE table_schema = '" + schemaName(CatalogDbDO) + "' "
                        + "AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String columnDefault = rs.getString("column_default");
                    return columnDefault != null && columnDefault.contains("nextval");
                }
            }
        } catch (Exception e) {
            log.error("获取PostgreSQL字段自增信息失败", e);
        }
        return false;
    }

    @Override
    public boolean isPartitionField(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        try {
            // 解析datasourceConfig获取连接信息
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return false;
            }

            // 构建连接字符串
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT a.attname AS column_name FROM pg_partitioned_table pt "
                        + "JOIN pg_class c ON c.oid = pt.partrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(pt.partattrs) "
                        + "WHERE n.nspname = '" + schemaName(CatalogDbDO) + "' AND c.relname = '" + tableName + "' "
                        + "AND a.attname = '" + columnName + "'";
                ResultSet rs = stmt.executeQuery(sql);
                return rs.next();
            }
        } catch (Exception e) {
            log.error("判断PostgreSQL字段是否为分区字段失败", e);
        }
        return false;
    }

    @Override
    public DbMetadata getDbMetadata(CatalogDbDO CatalogDbDO) {
        DbMetadata metadata = new DbMetadata();
        try {
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return metadata;
            }
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT pg_database_size(current_database()) / 1024 / 1024 AS total_size_mb";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    metadata.setStorageSize(rs.getInt("total_size_mb"));
                }
            }
        } catch (Exception e) {
            log.error("批量获取PostgreSQL数据库元数据失败", e);
        }
        return metadata;
    }

    private void fillTableMetadata(CatalogDbDO CatalogDbDO, String tableName, TableMetadata metadata) {
        try {
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return;
            }
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String tableSql = "SELECT obj_description(c.oid) AS table_comment, "
                        + "pg_total_relation_size(c.oid) AS table_size, "
                        + "to_char(GREATEST(st.last_vacuum, st.last_autovacuum), 'YYYY-MM-DD HH24:MI:SS') AS update_time "
                        + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "LEFT JOIN pg_stat_all_tables st ON st.relid = c.oid "
                        + "WHERE n.nspname = '" + schemaName(CatalogDbDO) + "' AND c.relname = '" + tableName + "'";
                ResultSet tableRs = stmt.executeQuery(tableSql);
                if (tableRs.next()) {
                    metadata.setTableComment(tableRs.getString("table_comment"));
                    metadata.setTableSize(tableRs.getInt("table_size"));
                    metadata.setUpdateTime(tableRs.getString("update_time"));
                }

                String pkSql = "SELECT a.attname AS column_name FROM pg_index i "
                        + "JOIN pg_class c ON c.oid = i.indrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey) "
                        + "WHERE i.indisprimary AND n.nspname = '" + schemaName(CatalogDbDO) + "' "
                        + "AND c.relname = '" + tableName + "' ORDER BY a.attnum";
                ResultSet pkRs = stmt.executeQuery(pkSql);
                StringBuilder primaryKeys = new StringBuilder();
                while (pkRs.next()) {
                    if (primaryKeys.length() > 0) {
                        primaryKeys.append(", ");
                    }
                    primaryKeys.append(pkRs.getString("column_name"));
                }
                metadata.setPrimaryKey(primaryKeys.toString());
            }
        } catch (Exception e) {
            log.error("批量获取PostgreSQL表元数据失败", e);
        }
    }

    private boolean isColumnUnique(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        try {
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return false;
            }
            String url = "jdbc:postgresql://" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + "/" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = metaData.getIndexInfo(null, schemaName(CatalogDbDO), tableName, true, false);
                while (rs.next()) {
                    if (columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("判断PostgreSQL字段唯一性失败", e);
        }
        return false;
    }

    private String qualifiedTableName(CatalogDbDO CatalogDbDO, String tableName) {
        return schemaName(CatalogDbDO) + "." + tableName;
    }

    private String schemaName(CatalogDbDO CatalogDbDO) {
        return StringUtils.isNotBlank(CatalogDbDO.getSchemaName()) ? CatalogDbDO.getSchemaName() : "public";
    }

    private String decryptPassword(String password) {
        try {
            return AesEncryptUtil.desEncrypt(password).trim();
        } catch (Exception e) {
            log.error("解密密码失败", e);
            return password;
        }
    }

    /**
     * 解析datasourceConfig获取连接信息
     */
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
}
