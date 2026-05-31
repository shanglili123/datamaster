package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;

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
}
