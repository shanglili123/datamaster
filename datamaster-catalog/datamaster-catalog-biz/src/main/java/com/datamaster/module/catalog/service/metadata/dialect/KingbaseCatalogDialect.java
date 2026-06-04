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
public class KingbaseCatalogDialect extends PostgreSqlDialect {

    @Override
    public String getStorageEngine(CatalogDbDO catalogDbDO) {
        return "KingbaseES";
    }

    @Override
    public Long getTableRowCount(CatalogDbDO catalogDbDO, String tableName) {
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + qualifiedTableName(catalogDbDO, tableName))) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("获取Kingbase表行数失败", e);
        }
        return 0L;
    }

    @Override
    public String getTableIndexes(CatalogDbDO catalogDbDO, String tableName) {
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT indexname FROM pg_indexes WHERE schemaname = '" + schemaName(catalogDbDO) + "' "
                     + "AND tablename = '" + tableName + "' AND indexname NOT LIKE 'pk_%'")) {
            StringBuilder indexes = new StringBuilder();
            while (rs.next()) {
                if (indexes.length() > 0) {
                    indexes.append(", ");
                }
                indexes.append(rs.getString("indexname"));
            }
            return indexes.toString();
        } catch (Exception e) {
            log.error("获取Kingbase表索引失败", e);
        }
        return "";
    }

    @Override
    public String getTablePartitionFields(CatalogDbDO catalogDbDO, String tableName) {
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT a.attname AS column_name FROM pg_partitioned_table pt "
                     + "JOIN pg_class c ON c.oid = pt.partrelid "
                     + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                     + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(pt.partattrs) "
                     + "WHERE n.nspname = '" + schemaName(catalogDbDO) + "' AND c.relname = '" + tableName + "'")) {
            StringBuilder fields = new StringBuilder();
            while (rs.next()) {
                if (fields.length() > 0) {
                    fields.append(", ");
                }
                fields.append(rs.getString("column_name"));
            }
            return fields.toString();
        } catch (Exception e) {
            log.error("获取Kingbase表分区字段失败", e);
        }
        return "";
    }

    @Override
    public boolean isColumnAutoIncrement(CatalogDbDO catalogDbDO, String tableName, String columnName) {
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT column_default FROM information_schema.columns WHERE table_schema = '"
                     + schemaName(catalogDbDO) + "' AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'")) {
            if (rs.next()) {
                String columnDefault = rs.getString("column_default");
                return columnDefault != null && columnDefault.contains("nextval");
            }
        } catch (Exception e) {
            log.error("获取Kingbase字段自增信息失败", e);
        }
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
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT pg_database_size('" + dbName(catalogDbDO) + "') / 1024 / 1024 AS storage_size")) {
            if (rs.next()) {
                metadata.setStorageSize(rs.getInt("storage_size"));
            }
        } catch (Exception e) {
            log.error("获取Kingbase数据库元数据失败", e);
        }
        return metadata;
    }

    @Override
    public TableMetadata getTableMetadata(CatalogDbDO catalogDbDO, String tableName) {
        TableMetadata metadata = new TableMetadata();
        metadata.setRowCount(getTableRowCount(catalogDbDO, tableName));
        metadata.setIndexes(getTableIndexes(catalogDbDO, tableName));
        metadata.setPartitionFields(getTablePartitionFields(catalogDbDO, tableName));
        metadata.setStorageEngine(getStorageEngine(catalogDbDO));
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT obj_description(c.oid) AS table_comment, "
                     + "pg_total_relation_size(c.oid) / 1024 / 1024 AS table_size "
                     + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                     + "WHERE n.nspname = '" + schemaName(catalogDbDO) + "' AND c.relname = '" + tableName + "'")) {
            if (rs.next()) {
                metadata.setTableComment(rs.getString("table_comment"));
                metadata.setTableSize(rs.getInt("table_size"));
            }
        } catch (Exception e) {
            log.error("获取Kingbase表基础元数据失败", e);
        }
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT a.attname AS column_name FROM pg_index i "
                     + "JOIN pg_class c ON c.oid = i.indrelid "
                     + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                     + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey) "
                     + "WHERE i.indisprimary = true AND n.nspname = '" + schemaName(catalogDbDO) + "' AND c.relname = '" + tableName + "'")) {
            StringBuilder primaryKeys = new StringBuilder();
            while (rs.next()) {
                if (primaryKeys.length() > 0) {
                    primaryKeys.append(", ");
                }
                primaryKeys.append(rs.getString("column_name"));
            }
            metadata.setPrimaryKey(primaryKeys.toString());
        } catch (Exception e) {
            log.error("获取Kingbase主键元数据失败", e);
        }
        return metadata;
    }

    @Override
    public ColumnMetadata getColumnMetadata(CatalogDbDO catalogDbDO, String tableName, String columnName) {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.setAutoIncrement(isColumnAutoIncrement(catalogDbDO, tableName, columnName));
        metadata.setPartitionField(isPartitionField(catalogDbDO, tableName, columnName));
        try (Connection conn = getKingbaseConnection(catalogDbDO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT i.indisunique FROM pg_index i "
                     + "JOIN pg_class c ON c.oid = i.indrelid "
                     + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                     + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey) "
                     + "WHERE i.indisunique = true AND n.nspname = '" + schemaName(catalogDbDO) + "' "
                     + "AND c.relname = '" + tableName + "' AND a.attname = '" + columnName + "'")) {
            metadata.setUnique(rs.next());
        } catch (Exception e) {
            log.error("获取Kingbase字段唯一性失败", e);
        }
        return metadata;
    }

    private Connection getKingbaseConnection(CatalogDbDO catalogDbDO) throws Exception {
        String url = "jdbc:kingbase8://" + catalogDbDO.getIp() + ":" + catalogDbDO.getPort() + "/" + dbName(catalogDbDO)
                + "?stringtype=unspecified";
        Map<String, Object> config = parseDatasourceConfig(catalogDbDO.getDatasourceConfig());
        String username = config == null ? null : (String) config.get("username");
        String password = config == null ? null : decryptPassword((String) config.get("password"));
        return DriverManager.getConnection(url, username, password);
    }

    private String qualifiedTableName(CatalogDbDO catalogDbDO, String tableName) {
        return schemaName(catalogDbDO) + "." + tableName;
    }

    private String schemaName(CatalogDbDO catalogDbDO) {
        if (StringUtils.isNotBlank(catalogDbDO.getSchemaName())) {
            return catalogDbDO.getSchemaName();
        }
        Map<String, Object> config = parseDatasourceConfig(catalogDbDO.getDatasourceConfig());
        Object sid = config == null ? null : config.get("sid");
        return sid == null || StringUtils.isBlank(String.valueOf(sid)) ? "public" : String.valueOf(sid);
    }

    private String dbName(CatalogDbDO catalogDbDO) {
        if (StringUtils.isNotBlank(catalogDbDO.getDbName())) {
            return catalogDbDO.getDbName();
        }
        Map<String, Object> config = parseDatasourceConfig(catalogDbDO.getDatasourceConfig());
        return config == null ? "" : String.valueOf(config.get("dbname"));
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
}
