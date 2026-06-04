package com.datamaster.module.catalog.service.metadata.dialect;

import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.catalog.dal.dataobject.metadata.CatalogDbDO;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库方言工厂
 * 用于根据数据库类型获取对应的方言实现
 */
public class DatabaseDialectFactory {

    private static final Map<String, DatabaseDialect> dialectMap = new HashMap<>();

    static {
        // 注册支持的数据库方言实现
        dialectMap.put("mysql", new MySqlDialect());
        dialectMap.put("hive", new HiveDialect());
        dialectMap.put("dm8", new DamengDialect());
        dialectMap.put("oracle", new OracleDialect());
        dialectMap.put("oracle11", new OracleDialect());
        dialectMap.put("postgresql", new PostgreSqlDialect());
        dialectMap.put("kingbase8", new KingbaseCatalogDialect());
        dialectMap.put("kingbase", new KingbaseCatalogDialect());
        dialectMap.put("sql_server", new SqlServerDialect());
        dialectMap.put("sql_server2008", new SqlServerDialect());
        dialectMap.put("sqlserver", new SqlServerDialect());
        dialectMap.put("clickhouse", new ClickHouseCatalogDialect());
        dialectMap.put("click_house", new ClickHouseCatalogDialect());
        dialectMap.put("doris", new DorisCatalogDialect());
    }

    /**
     * 根据数据库类型获取对应的方言实现
     */
    public static DatabaseDialect getDialect(CatalogDbDO CatalogDbDO) {
        if (CatalogDbDO == null || StringUtils.isBlank(CatalogDbDO.getDbType())) {
            return null;
        }
        return dialectMap.get(CatalogDbDO.getDbType().toLowerCase());
    }

    /**
     * 注册新的方言实现
     */
    public static void registerDialect(String dbType, DatabaseDialect dialect) {
        if (StringUtils.isNotBlank(dbType) && dialect != null) {
            dialectMap.put(dbType.toLowerCase(), dialect);
        }
    }
}
