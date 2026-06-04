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
 * Oracle数据库方言实现
 */
@Slf4j
public class OracleDialect implements DatabaseDialect {
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
            // Oracle数据库使用表空间，这里返回Oracle Database
            return "Oracle Database";
        } catch (Exception e) {
            log.error("获取Oracle存储引擎失败", e);
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
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT COUNT(*) FROM " + tableName;
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            log.error("获取Oracle表行数失败", e);
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
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT INDEX_NAME FROM USER_INDEXES WHERE TABLE_NAME = '" + tableName.toUpperCase() + "' AND INDEX_NAME != 'PK_'";
                ResultSet rs = stmt.executeQuery(sql);
                StringBuilder indexes = new StringBuilder();
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexes.length() > 0) {
                        indexes.append(", ");
                    }
                    indexes.append(indexName);
                }
                return indexes.toString();
            }
        } catch (Exception e) {
            log.error("获取Oracle表索引信息失败", e);
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
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT COLUMN_NAME FROM USER_PART_KEY_COLUMNS WHERE NAME = '" + tableName.toUpperCase() + "'";
                ResultSet rs = stmt.executeQuery(sql);
                StringBuilder partitionFields = new StringBuilder();
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (partitionFields.length() > 0) {
                        partitionFields.append(", ");
                    }
                    partitionFields.append(columnName);
                }
                return partitionFields.toString();
            }
        } catch (Exception e) {
            log.error("获取Oracle表分区字段信息失败", e);
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
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT IDENTITY_COLUMN FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tableName.toUpperCase() + "' AND COLUMN_NAME = '" + columnName.toUpperCase() + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String identityColumn = rs.getString("IDENTITY_COLUMN");
                    return "YES".equals(identityColumn);
                }
            }
        } catch (Exception e) {
            log.error("获取Oracle字段自增信息失败", e);
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
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));

            // 连接数据库并执行查询
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT COLUMN_NAME FROM USER_PART_KEY_COLUMNS WHERE NAME = '" + tableName.toUpperCase() + "' AND COLUMN_NAME = '" + columnName.toUpperCase() + "'";
                ResultSet rs = stmt.executeQuery(sql);
                return rs.next();
            }
        } catch (Exception e) {
            log.error("判断Oracle字段是否为分区字段失败", e);
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
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String sql = "SELECT ROUND(SUM(bytes) / 1024 / 1024) AS total_size_mb FROM user_segments";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    metadata.setStorageSize(rs.getInt("total_size_mb"));
                }
            }
        } catch (Exception e) {
            log.error("批量获取Oracle数据库元数据失败", e);
        }
        return metadata;
    }

    private void fillTableMetadata(CatalogDbDO CatalogDbDO, String tableName, TableMetadata metadata) {
        try {
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return;
            }
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement()) {
                String tableSql = "SELECT t.NUM_ROWS, c.COMMENTS, s.BYTES FROM USER_TABLES t "
                        + "LEFT JOIN USER_TAB_COMMENTS c ON c.TABLE_NAME = t.TABLE_NAME "
                        + "LEFT JOIN (SELECT SEGMENT_NAME, SUM(BYTES) BYTES FROM USER_SEGMENTS GROUP BY SEGMENT_NAME) s "
                        + "ON s.SEGMENT_NAME = t.TABLE_NAME WHERE t.TABLE_NAME = '" + tableName.toUpperCase() + "'";
                ResultSet tableRs = stmt.executeQuery(tableSql);
                if (tableRs.next()) {
                    if (metadata.getRowCount() == null || metadata.getRowCount() == 0L) {
                        metadata.setRowCount(tableRs.getLong("NUM_ROWS"));
                    }
                    metadata.setTableComment(tableRs.getString("COMMENTS"));
                    metadata.setTableSize(tableRs.getInt("BYTES"));
                }

                String pkSql = "SELECT cols.COLUMN_NAME FROM USER_CONSTRAINTS cons "
                        + "JOIN USER_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME "
                        + "WHERE cons.CONSTRAINT_TYPE = 'P' AND cons.TABLE_NAME = '" + tableName.toUpperCase() + "' "
                        + "ORDER BY cols.POSITION";
                ResultSet pkRs = stmt.executeQuery(pkSql);
                StringBuilder primaryKeys = new StringBuilder();
                while (pkRs.next()) {
                    if (primaryKeys.length() > 0) {
                        primaryKeys.append(", ");
                    }
                    primaryKeys.append(pkRs.getString("COLUMN_NAME"));
                }
                metadata.setPrimaryKey(primaryKeys.toString());
            }
        } catch (Exception e) {
            log.error("批量获取Oracle表元数据失败", e);
        }
    }

    private boolean isColumnUnique(CatalogDbDO CatalogDbDO, String tableName, String columnName) {
        try {
            Map<String, Object> configMap = parseDatasourceConfig(CatalogDbDO.getDatasourceConfig());
            if (configMap == null) {
                return false;
            }
            String url = "jdbc:oracle:thin:@" + CatalogDbDO.getIp() + ":" + CatalogDbDO.getPort() + ":" + configMap.get("dbname");
            String username = (String) configMap.get("username");
            String password = decryptPassword((String) configMap.get("password"));
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = metaData.getIndexInfo(null, null, tableName.toUpperCase(), true, false);
                while (rs.next()) {
                    if (columnName.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("判断Oracle字段唯一性失败", e);
        }
        return false;
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
