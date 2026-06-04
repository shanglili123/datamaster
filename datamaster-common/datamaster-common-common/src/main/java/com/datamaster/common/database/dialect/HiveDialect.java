package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.database.utils.DatabaseUtil;
import com.datamaster.common.utils.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Hive is handled as a JDBC source for FlinkX and connection metadata.
 */
public class HiveDialect extends MySqlDialect {

    @Override
    public RowMapper<DbTable> tableMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbTable table = new DbTable();
            table.setTableName(rs.getString(1));
            table.setTableComment("");
            return table;
        };
    }

    @Override
    public RowMapper<DbColumn> columnMapper() {
        return (ResultSet rs, int rowNum) -> {
            String colName = rs.getString(1);
            if (colName == null || colName.trim().isEmpty() || colName.startsWith("#")) {
                return null;
            }
            DbColumn column = new DbColumn();
            column.setColName(colName);
            column.setDataType(rs.getString(2));
            column.setColComment(rs.getMetaData().getColumnCount() >= 3 ? rs.getString(3) : "");
            column.setColPosition(rowNum + 1);
            column.setColKey(false);
            column.setNullable(true);
            return column;
        };
    }

    @Override
    public String tables(DbQueryProperty dbQueryProperty) {
        return "SHOW TABLES IN `" + dbQueryProperty.getDbName() + "`";
    }

    @Override
    public String generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName) {
        return "SHOW TABLES IN `" + dbQueryProperty.getDbName() + "` LIKE '" + tableName + "'";
    }

    @Override
    public String columns(DbQueryProperty dbQueryProperty, String tableName) {
        return "DESCRIBE `" + dbQueryProperty.getDbName() + "`.`" + tableName + "`";
    }

    @Override
    public String tablesComment(DbQueryProperty dbQueryProperty, String tableName) {
        return "DESCRIBE FORMATTED `" + dbQueryProperty.getDbName() + "`.`" + tableName + "`";
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        return "jdbc:hive2://" + property.getHost() + ":" + property.getPort() + "/" + property.getDbName();
    }

    @Override
    public List<String> someInternalSqlGenerator(DbQueryProperty dbQueryProperty, String tableName, String tableComment, List<DbColumn> dbColumnList) {
        List<String> sqlList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(qualifiedTableName(dbQueryProperty, tableName)).append(" (\n");
        for (DbColumn column : dbColumnList) {
            sql.append("  `").append(column.getColName()).append("` ");
            sql.append(hiveColumnType(column));
            if (StringUtils.isNotEmpty(column.getColComment())) {
                sql.append(" COMMENT '").append(DatabaseUtil.escapeSingleQuotes(column.getColComment())).append("'");
            }
            sql.append(",\n");
        }
        sql.setLength(sql.length() - 2);
        sql.append("\n)");
        if (StringUtils.isNotEmpty(tableComment)) {
            sql.append("\nCOMMENT '").append(DatabaseUtil.escapeSingleQuotes(tableComment)).append("'");
        }
        sql.append("\nSTORED AS PARQUET");
        sqlList.add(sql.toString());
        return sqlList;
    }

    private String qualifiedTableName(DbQueryProperty property, String tableName) {
        if (StringUtils.isNotEmpty(property.getDbName())) {
            return "`" + property.getDbName() + "`.`" + tableName + "`";
        }
        return "`" + tableName + "`";
    }

    private String hiveColumnType(DbColumn column) {
        String dataType = column.getDataType() == null ? "STRING" : column.getDataType().toUpperCase();
        switch (dataType) {
            case "VARCHAR":
            case "VARCHAR2":
                if (StringUtils.isNotEmpty(column.getDataLength())) {
                    return "VARCHAR(" + column.getDataLength() + ")";
                }
                return "STRING";
            case "CHAR":
                if (StringUtils.isNotEmpty(column.getDataLength())) {
                    return "CHAR(" + column.getDataLength() + ")";
                }
                return "STRING";
            case "TEXT":
            case "STRING":
            case "JSON":
                return "STRING";
            case "INT":
            case "INTEGER":
                return "INT";
            case "BIGINT":
                return "BIGINT";
            case "TINYINT":
                return "TINYINT";
            case "SMALLINT":
                return "SMALLINT";
            case "FLOAT":
                return "FLOAT";
            case "DOUBLE":
                return "DOUBLE";
            case "DECIMAL":
            case "NUMERIC":
            case "NUMBER":
                return "DECIMAL(" + valueOrDefault(column.getDataLength(), "18") + "," + valueOrDefault(column.getDataScale(), "2") + ")";
            case "DATE":
                return "DATE";
            case "DATETIME":
            case "TIMESTAMP":
                return "TIMESTAMP";
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";
            default:
                return dataType;
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }
}
