

package com.datamaster.quality.utils.qualityDB;

import com.datamaster.common.database.constants.DbType;
import com.datamaster.quality.utils.qualityDB.dialect.*;

import java.util.HashMap;
import java.util.Map;

public class ComponentRegistry {

    private final Map<String, ComponentItem> componentItemMap = new HashMap<>();
    private final ComponentItem defaultImpl = new DefaultQuality();

    public ComponentRegistry() {
        this.componentItemMap.put(DbType.MYSQL.getDb(), new MySqlQuality());
        this.componentItemMap.put(DbType.ORACLE_12C.getDb(), new Oracle12cQuality());
        this.componentItemMap.put(DbType.ORACLE.getDb(), new OracleQuality());
        this.componentItemMap.put(DbType.POSTGRE_SQL.getDb(), new PostgreSqlQuality());
        this.componentItemMap.put(DbType.SQL_SERVER.getDb(), new SQLServerQuality());
        this.componentItemMap.put(DbType.SQL_SERVER2008.getDb(), new SQLServerQuality());
        this.componentItemMap.put(DbType.DM8.getDb(), new DM8Quality());
        this.componentItemMap.put(DbType.KINGBASE8.getDb(), new PostgreSqlQuality());
        this.componentItemMap.put(DbType.DORIS.getDb(), new MySqlQuality());
        this.componentItemMap.put(DbType.CLICK_HOUSE.getDb(), new ClickHouseQuality());
        this.componentItemMap.put(DbType.HIVE.getDb(), new HiveQuality());
    }

    public ComponentItem getComponentItem(String dbCode) {
        return componentItemMap.getOrDefault(dbCode, defaultImpl);
    }

}
