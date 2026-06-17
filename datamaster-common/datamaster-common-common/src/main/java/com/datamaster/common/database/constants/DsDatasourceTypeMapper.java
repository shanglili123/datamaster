
package com.datamaster.common.database.constants;

import java.util.HashMap;
import java.util.Map;

public class DsDatasourceTypeMapper {

    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("MySql", "MYSQL");
        TYPE_MAP.put("2", "MARIADB");
        TYPE_MAP.put("Oracle11", "ORACLE");
        TYPE_MAP.put("Oracle", "ORACLE");
        TYPE_MAP.put("PostgreSQL", "POSTGRESQL");
        TYPE_MAP.put("SQL_Server2008", "SQLSERVER");
        TYPE_MAP.put("SQL_Server", "SQLSERVER");
        TYPE_MAP.put("DM8", "DAMENG");
        TYPE_MAP.put("Kingbase8", "KINGBASE");
        TYPE_MAP.put("Hive", "HIVE");
        TYPE_MAP.put("ClickHouse", "CLICKHOUSE");
        TYPE_MAP.put("DB2", "DB2");
        TYPE_MAP.put("MongoDB", "MONGODB");
        TYPE_MAP.put("Elasticsearch", "ELASTICSEARCH");
        TYPE_MAP.put("Redis", "REDIS");
        TYPE_MAP.put("Kafka", "KAFKA");
    }

    public static String toDsType(String platformType) {
        return TYPE_MAP.getOrDefault(platformType, platformType.toUpperCase());
    }

}
