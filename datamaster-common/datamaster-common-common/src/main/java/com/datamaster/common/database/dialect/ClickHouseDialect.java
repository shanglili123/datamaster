package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;

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
}
