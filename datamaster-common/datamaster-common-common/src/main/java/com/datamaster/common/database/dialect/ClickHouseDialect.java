package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTableMetadata;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.database.utils.DatabaseUtil;
import com.datamaster.common.utils.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ClickHouse metadata adapter backed by information_schema.
 */
public class ClickHouseDialect extends MySqlDialect {

    @Override
    public String columns(DbQueryProperty dbQueryProperty, String tableName) {
        return "select name AS COLNAME, position AS COLPOSITION, default_expression AS DATADEFAULT, " +
                "if(is_in_primary_key = 1, 'NO', 'YES') AS NULLABLE, type AS DATATYPE, " +
                "null AS DATALENGTH, null AS DATAPRECISION, null AS DATASCALE, " +
                "if(is_in_primary_key = 1, 'PRI', '') AS COLKEY, comment AS COLCOMMENT " +
                "from system.columns where database = '" + dbQueryProperty.getDbName() + "' and table = '" + tableName + "' order by position";
    }

    @Override
    public String getDbColumns(DbQueryProperty dbQueryProperty) {
        return "select table AS TABLENAME, name AS COLNAME, position AS COLPOSITION, default_expression AS DATADEFAULT, " +
                "if(is_in_primary_key = 1, 'NO', 'YES') AS NULLABLE, type AS DATATYPE, " +
                "null AS DATALENGTH, null AS DATAPRECISION, null AS DATASCALE, " +
                "if(is_in_primary_key = 1, 'PRI', '') AS COLKEY, comment AS COLCOMMENT " +
                "from system.columns where database = '" + dbQueryProperty.getDbName() + "' order by table, position";
    }

    @Override
    public String tables(DbQueryProperty dbQueryProperty) {
        return "select name AS TABLENAME, comment AS TABLECOMMENT from system.tables where database = '" + dbQueryProperty.getDbName() + "'";
    }

    @Override
    public String generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName) {
        return "select count(*) from system.tables where database = '" + dbQueryProperty.getDbName() + "' and name = '" + tableName + "'";
    }

    @Override
    public List<String> someInternalSqlGenerator(DbQueryProperty dbQueryProperty, String tableName, String tableComment, List<DbColumn> dbColumnList) {
        List<String> sqlList = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(qualifiedTableName(dbQueryProperty, tableName)).append(" (\n");

        for (DbColumn column : dbColumnList) {
            sql.append("  `").append(column.getColName()).append("` ");
            sql.append(clickHouseColumnType(column));
            if (StringUtils.isNotEmpty(column.getDataDefault())) {
                sql.append(" DEFAULT ").append(column.getDataDefault());
            }
            if (StringUtils.isNotEmpty(column.getColComment())) {
                sql.append(" COMMENT '").append(DatabaseUtil.escapeSingleQuotes(column.getColComment())).append("'");
            }
            if (Boolean.TRUE.equals(column.getColKey())) {
                primaryKeys.add(column.getColName());
            }
            sql.append(",\n");
        }

        sql.setLength(sql.length() - 2);
        sql.append("\n)");
        if (StringUtils.isNotEmpty(tableComment)) {
            sql.append(" COMMENT '").append(DatabaseUtil.escapeSingleQuotes(tableComment)).append("'");
        }
        sql.append("\nENGINE = MergeTree");
        if (!primaryKeys.isEmpty()) {
            String orderBy = primaryKeys.stream().map(col -> "`" + col + "`").collect(Collectors.joining(", "));
            sql.append("\nORDER BY (").append(orderBy).append(")");
        } else {
            sql.append("\nORDER BY tuple()");
        }
        sqlList.add(sql.toString());
        return sqlList;
    }

    @Override
    public DbTableMetadata tableMetadata(DbQueryProperty dbQueryProperty, String tableName, Connection conn) {
        DbTableMetadata metadata = new DbTableMetadata();
        try {
            metadata.setRowCount(getTableRowCount(dbQueryProperty, tableName, conn));
            metadata.setIndexes("");
            metadata.setPartitionFields(getTablePartitionFields(dbQueryProperty, tableName, conn));
            metadata.setStorageEngine("ClickHouse");
        } catch (Exception e) {
            throw new DataQueryException("采集ClickHouse表元数据失败: " + e.getMessage());
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT comment, engine, create_table_query FROM system.tables WHERE database = '"
                     + dbName(dbQueryProperty) + "' AND name = '" + tableName + "'")) {
            if (rs.next()) {
                metadata.setTableComment(rs.getString("comment"));
                metadata.setStorageEngine(rs.getString("engine"));
                metadata.setPrimaryKey(extractPrimaryKey(rs.getString("create_table_query")));
            }
        } catch (Exception e) {
            throw new DataQueryException("获取ClickHouse表元数据失败: " + e.getMessage());
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sum(bytes_on_disk) / 1024 / 1024 AS table_size "
                     + "FROM system.parts WHERE active = 1 AND database = '" + dbName(dbQueryProperty) + "' AND table = '" + tableName + "'")) {
            if (rs.next()) {
                metadata.setTableSize(rs.getInt("table_size"));
            }
        } catch (Exception e) {
            throw new DataQueryException("获取ClickHouse表大小失败: " + e.getMessage());
        }
        return metadata;
    }

    private Long getTableRowCount(DbQueryProperty dbQueryProperty, String tableName, Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + qualifiedTableName(dbQueryProperty, tableName))) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            throw new DataQueryException("获取ClickHouse表行数失败: " + e.getMessage());
        }
        return 0L;
    }

    private String getTablePartitionFields(DbQueryProperty dbQueryProperty, String tableName, Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT partition_key FROM system.tables WHERE database = '"
                     + dbName(dbQueryProperty) + "' AND name = '" + tableName + "'")) {
            if (rs.next()) {
                return normalizeClickHouseExpression(rs.getString("partition_key"));
            }
        } catch (Exception e) {
            throw new DataQueryException("获取ClickHouse表分区字段失败: " + e.getMessage());
        }
        return "";
    }

    private String dbName(DbQueryProperty dbQueryProperty) {
        return StringUtils.isNotBlank(dbQueryProperty.getDbName()) ? dbQueryProperty.getDbName() : "";
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
        Matcher matcher = Pattern.compile("PRIMARY KEY\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE)
                .matcher(createTableSql);
        return matcher.find() ? normalizeClickHouseExpression(matcher.group(1)) : "";
    }

    private String qualifiedTableName(DbQueryProperty property, String tableName) {
        if (StringUtils.isNotEmpty(property.getDbName())) {
            return "`" + property.getDbName() + "`.`" + tableName + "`";
        }
        return "`" + tableName + "`";
    }

    private String clickHouseColumnType(DbColumn column) {
        String dataType = column.getDataType() == null ? "String" : column.getDataType().toUpperCase();
        String type;
        switch (dataType) {
            case "VARCHAR":
            case "VARCHAR2":
            case "CHAR":
            case "TEXT":
            case "STRING":
            case "JSON":
                type = "String";
                break;
            case "TINYINT":
                type = "Int8";
                break;
            case "SMALLINT":
                type = "Int16";
                break;
            case "INT":
            case "INTEGER":
                type = "Int32";
                break;
            case "BIGINT":
                type = "Int64";
                break;
            case "FLOAT":
                type = "Float32";
                break;
            case "DOUBLE":
                type = "Float64";
                break;
            case "DECIMAL":
            case "NUMERIC":
            case "NUMBER":
                type = "Decimal(" + valueOrDefault(column.getDataLength(), "18") + ", " + valueOrDefault(column.getDataScale(), "2") + ")";
                break;
            case "DATE":
                type = "Date";
                break;
            case "DATETIME":
            case "TIMESTAMP":
                type = "DateTime";
                break;
            case "BOOLEAN":
            case "BOOL":
                type = "Bool";
                break;
            default:
                type = dataType;
                break;
        }
        if (Boolean.TRUE.equals(column.getNullable()) && !Boolean.TRUE.equals(column.getColKey())) {
            return "Nullable(" + type + ")";
        }
        return type;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }
}
