package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.utils.DatabaseUtil;
import com.datamaster.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
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
