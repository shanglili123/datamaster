package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoCreateTableHelper {

    private static final Logger log = LoggerFactory.getLogger(AutoCreateTableHelper.class);

    public static void ensureTargetTableExists(
            Map<String, Object> readerMap,
            Map<String, Object> writerMap,
            DataSourceFactory dataSourceFactory) {
        if (readerMap == null || writerMap == null || dataSourceFactory == null) return;
        Map<String, Object> writerParam = extractParamMap(writerMap);
        Map<String, Object> readerParam = extractParamMap(readerMap);
        if (writerParam == null || readerParam == null) return;
        try {
            doEnsureMap(writerParam, readerParam, dataSourceFactory);
        } catch (Exception e) {
            log.warn("auto create table failed (Map path), skip", e);
        }
    }

    public static void ensureTargetTableExists(
            JSONObject readerObj,
            JSONObject writerObj,
            DataSourceFactory dataSourceFactory) {
        if (readerObj == null || writerObj == null || dataSourceFactory == null) return;
        JSONObject writerParam = writerObj.getJSONObject("parameter");
        JSONObject readerParam = readerObj.getJSONObject("parameter");
        if (writerParam == null || readerParam == null) return;
        try {
            doEnsureJson(writerParam, readerParam, dataSourceFactory);
        } catch (Exception e) {
            log.warn("auto create table failed (JSON path), skip", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractParamMap(Map<String, Object> node) {
        Object param = node.get("parameter");
        return param instanceof Map ? (Map<String, Object>) param : null;
    }

    // ===== Map-based core logic =====

    private static void doEnsureMap(Map<String, Object> writerParam,
                                    Map<String, Object> readerParam,
                                    DataSourceFactory factory) {
        String dbType = stringVal(writerParam, "dbType");
        if (dbType == null || !isJdbcDestination(dbType)) return;

        Map<String, Object> writerConn = mapVal(writerParam, "connection");
        if (writerConn == null) return;
        String targetTable = stringVal(writerConn, "table");
        if (targetTable == null || targetTable.isEmpty()) return;

        DbQueryProperty writerProp = buildProperty(writerParam);
        if (writerProp == null) return;

        DbQuery writerQuery = factory.createDbQuery(writerProp);
        try {
            int exists = writerQuery.generateCheckTableExistsSQL(writerProp, targetTable);
            if (exists > 0) {
                log.info("table [{}] already exists, skip auto-create", targetTable);
                return;
            }

            Map<String, Object> readerConn = mapVal(readerParam, "connection");
            if (readerConn == null) return;
            String sourceTable = stringVal(readerConn, "table");
            if (sourceTable == null || sourceTable.isEmpty()) return;

            DbQueryProperty readerProp = buildProperty(readerParam);
            if (readerProp == null) return;

            DbQuery readerQuery = factory.createDbQuery(readerProp);
            try {
                List<DbColumn> columns = readerQuery.getTableColumns(readerProp, sourceTable);
                if (columns == null || columns.isEmpty()) {
                    log.warn("no columns found from source table [{}], skip auto-create", sourceTable);
                    return;
                }

                List<String> createSqls = writerQuery.generateCreateTableSQL(writerProp, targetTable, targetTable, columns);
                if (createSqls == null || createSqls.isEmpty()) return;

                Object existing = writerParam.get("preSql");
                List<String> preSqlList;
                if (existing instanceof List) {
                    preSqlList = (List<String>) existing;
                } else {
                    preSqlList = new ArrayList<>();
                }
                preSqlList.addAll(0, createSqls);
                writerParam.put("preSql", preSqlList);

                log.info("auto-created table [{}] with {} columns from [{}]",
                        targetTable, columns.size(), sourceTable);
            } finally {
                readerQuery.close();
            }
        } finally {
            writerQuery.close();
        }
    }

    // ===== JSONObject-based core logic =====

    private static void doEnsureJson(JSONObject writerParam,
                                     JSONObject readerParam,
                                     DataSourceFactory factory) {
        String dbType = writerParam.getString("dbType");
        if (dbType == null || !isJdbcDestination(dbType)) return;

        JSONObject writerConn = writerParam.getJSONObject("connection");
        if (writerConn == null) return;
        String targetTable = writerConn.getString("table");
        if (targetTable == null || targetTable.isEmpty()) return;

        DbQueryProperty writerProp = buildProperty(writerParam);
        if (writerProp == null) return;

        DbQuery writerQuery = factory.createDbQuery(writerProp);
        try {
            int exists = writerQuery.generateCheckTableExistsSQL(writerProp, targetTable);
            if (exists > 0) {
                log.info("table [{}] already exists, skip auto-create", targetTable);
                return;
            }

            JSONObject readerConn = readerParam.getJSONObject("connection");
            if (readerConn == null) return;
            String sourceTable = readerConn.getString("table");
            if (sourceTable == null || sourceTable.isEmpty()) return;

            DbQueryProperty readerProp = buildProperty(readerParam);
            if (readerProp == null) return;

            DbQuery readerQuery = factory.createDbQuery(readerProp);
            try {
                List<DbColumn> columns = readerQuery.getTableColumns(readerProp, sourceTable);
                if (columns == null || columns.isEmpty()) {
                    log.warn("no columns found from source table [{}], skip auto-create", sourceTable);
                    return;
                }

                List<String> createSqls = writerQuery.generateCreateTableSQL(writerProp, targetTable, targetTable, columns);
                if (createSqls == null || createSqls.isEmpty()) return;

                JSONArray existing = writerParam.getJSONArray("preSql");
                JSONArray preSqlArr;
                if (existing != null && !existing.isEmpty()) {
                    preSqlArr = existing;
                } else {
                    preSqlArr = new JSONArray();
                }
                for (int i = createSqls.size() - 1; i >= 0; i--) {
                    preSqlArr.add(0, createSqls.get(i));
                }
                writerParam.put("preSql", preSqlArr);

                log.info("auto-created table [{}] with {} columns from [{}]",
                        targetTable, columns.size(), sourceTable);
            } finally {
                readerQuery.close();
            }
        } finally {
            writerQuery.close();
        }
    }

    // ===== helpers =====

    private static DbQueryProperty buildProperty(Map<String, Object> param) {
        String dbType = stringVal(param, "dbType");
        String host = stringVal(param, "host");
        Integer port = intVal(param, "port");
        if (dbType == null || host == null || port == null) return null;
        return new DbQueryProperty(
                dbType, host,
                stringVal(param, "username"),
                stringVal(param, "password"),
                port,
                stringVal(param, "dbName"),
                stringVal(param, "sid")
        );
    }

    private static DbQueryProperty buildProperty(JSONObject param) {
        String dbType = param.getString("dbType");
        String host = param.getString("host");
        Integer port = param.getInteger("port");
        if (dbType == null || host == null || port == null) return null;
        return new DbQueryProperty(
                dbType, host,
                param.getString("username"),
                param.getString("password"),
                port,
                param.getString("dbName"),
                param.getString("sid")
        );
    }

    private static boolean isJdbcDestination(String dbType) {
        if (dbType == null) return false;
        switch (dbType.toUpperCase()) {
            case "MYSQL":
            case "ORACLE":
            case "ORACLE_12C":
            case "POSTGRE_SQL":
            case "POSTGRESQL":
            case "SQL_SERVER":
            case "SQL_SERVER2008":
            case "DM8":
            case "KINGBASE8":
            case "CLICK_HOUSE":
            case "CLICKHOUSE":
            case "DORIS":
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static String stringVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : null;
    }

    @SuppressWarnings("unchecked")
    private static Integer intVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }
}
