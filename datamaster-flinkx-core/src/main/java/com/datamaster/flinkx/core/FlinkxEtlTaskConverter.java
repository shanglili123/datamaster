package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlinkxEtlTaskConverter {

    public static String convertToFlinkxJobJson(Map<String, Object> mainArgs) {
        Map<String, Object> readerMap = (Map<String, Object>) mainArgs.get("reader");
        Map<String, Object> writerMap = (Map<String, Object>) mainArgs.get("writer");
        List<Map<String, Object>> transitionList = (List<Map<String, Object>>) mainArgs.get("transition");
        JSONObject config = mainArgs.get("config") instanceof Map
                ? new JSONObject((Map<String, Object>) mainArgs.get("config"))
                : new JSONObject();
        Object taskInfo = config.get("taskInfo");
        String name = taskInfo instanceof Map
                ? (String) ((Map<String, Object>) taskInfo).get("name")
                : "flinkx-etl";

        JSONObject root = new JSONObject();
        JSONObject job = new JSONObject();

        JSONArray content = new JSONArray();
        if (readerMap != null || writerMap != null) {
            JSONObject contentItem = new JSONObject();
            if (readerMap != null) {
                if (isExcelOrCsvReader(readerMap)) {
                    contentItem.put("reader", buildFileReader(readerMap));
                } else {
                    contentItem.put("reader", buildReader(readerMap));
                }
            }
            if (writerMap != null) {
                contentItem.put("writer", buildWriter(writerMap));
            }
            if (transitionList != null && !transitionList.isEmpty()) {
                JSONArray transformer = buildTransformer(transitionList);
                if (transformer != null && !transformer.isEmpty()) {
                    contentItem.put("transformer", transformer);
                }
            }
            content.add(contentItem);
        }
        job.put("content", content);

        job.put("setting", buildSetting());
        root.put("job", job);
        return root.toJSONString();
    }

    private static boolean isExcelOrCsvReader(Map<String, Object> readerMap) {
        String componentType = (String) readerMap.get("componentType");
        return "EXCEL_READER".equals(componentType) || "CSV_READER".equals(componentType);
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildReader(Map<String, Object> readerMap) {
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");
        String dbType = param != null ? (String) param.get("dbType") : null;

        JSONObject reader = new JSONObject();
        reader.put("name", resolveReaderName(dbType));

        JSONObject rp = new JSONObject();
        if (param != null) {
            if (isMongo(dbType)) {
                reader.put("parameter", buildMongoParameter(param, true));
                return reader;
            }
            if (isElasticsearch(dbType)) {
                reader.put("parameter", buildElasticsearchParameter(param, true));
                return reader;
            }
            rp.put("username", param.get("username"));
            rp.put("password", param.get("password"));
            rp.put("where", param.getOrDefault("where", ""));
            rp.put("column", param.get("column"));
            rp.put("splitPk", param.getOrDefault("splitPk", ""));

            Map<String, Object> rawConn = (Map<String, Object>) param.get("connection");
            if (rawConn != null) {
                JSONArray conns = new JSONArray();
                JSONObject conn = new JSONObject();
                Object jdbcUrl = rawConn.get("jdbcUrl");
                conn.put("jdbcUrl", jdbcUrl instanceof List ? jdbcUrl : new String[]{String.valueOf(jdbcUrl)});
                Object table = rawConn.get("table");
                if (table instanceof String) {
                    conn.put("table", new String[]{(String) table});
                } else if (table instanceof List) {
                    conn.put("table", table);
                } else {
                    conn.put("table", new String[0]);
                }
                conns.add(conn);
                rp.put("connection", conns);
            }

            rp.put("batchSize", param.getOrDefault("batchSize", 1024));
        }
        reader.put("parameter", rp);
        return reader;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildWriter(Map<String, Object> writerMap) {
        Map<String, Object> param = (Map<String, Object>) writerMap.get("parameter");
        String dbType = param != null ? (String) param.get("dbType") : null;

        JSONObject writer = new JSONObject();
        writer.put("name", resolveWriterName(dbType));

        JSONObject wp = new JSONObject();
        if (param != null) {
            if (isMongo(dbType)) {
                writer.put("parameter", buildMongoParameter(param, false));
                return writer;
            }
            if (isElasticsearch(dbType)) {
                writer.put("parameter", buildElasticsearchParameter(param, false));
                return writer;
            }
            wp.put("username", param.get("username"));
            wp.put("password", param.get("password"));
            wp.put("batchSize", param.getOrDefault("batchSize", 1024));

            Object targetColumns = param.get("target_column");
            wp.put("column", targetColumns != null ? targetColumns : param.get("column"));

            String writeModeType = param.get("writeModeType") == null ? null : String.valueOf(param.get("writeModeType"));
            wp.put("writeMode", resolveWriteMode(writeModeType, param.get("selectedColumns"), dbType));

            Object preSql = param.get("preSql");
            if (preSql != null) {
                wp.put("preSql", preSql instanceof List ? preSql : String.valueOf(preSql).split(","));
            }
            Object postSql = param.get("postSql");
            if (postSql != null) {
                wp.put("postSql", postSql instanceof List ? postSql : String.valueOf(postSql).split(","));
            }

            Map<String, Object> rawConn = (Map<String, Object>) param.get("connection");
            if (rawConn != null) {
                JSONArray conns = new JSONArray();
                JSONObject conn = new JSONObject();
                conn.put("jdbcUrl", String.valueOf(rawConn.get("jdbcUrl")));
                Object table = rawConn.get("table");
                if (table instanceof String) {
                    conn.put("table", new String[]{(String) table});
                } else if (table instanceof List) {
                    conn.put("table", table);
                } else {
                    conn.put("table", new String[0]);
                }
                conns.add(conn);
                wp.put("connection", conns);
            }
        }
        writer.put("parameter", wp);
        return writer;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildMongoParameter(Map<String, Object> param, boolean reader) {
        JSONObject config = datasourceConfig(param);
        JSONObject mp = new JSONObject();
        mp.put("address", param.get("host") + ":" + param.get("port"));
        putIfPresent(mp, "username", param.get("username"));
        putIfPresent(mp, "password", param.get("password"));
        mp.put("database", firstPresent(param.get("dbName"), config.get("database"), config.get("dbname"), config.get("dbName")));
        mp.put("collectionName", firstPresent(connectionTable(param), config.get("collection"), config.get("collectionName")));
        mp.put("column", firstPresent(param.get("target_column"), param.get("column")));
        mp.put("batchSize", param.getOrDefault("batchSize", 1024));
        if (!reader) {
            String writeModeType = param.get("writeModeType") == null ? null : String.valueOf(param.get("writeModeType"));
            mp.put("writeMode", resolveWriteMode(writeModeType, param.get("selectedColumns"), (String) param.get("dbType")));
        }
        putIfPresent(mp, "query", connectionQuery(param));
        return mp;
    }

    private static JSONObject buildElasticsearchParameter(Map<String, Object> param, boolean reader) {
        JSONObject config = datasourceConfig(param);
        JSONObject ep = new JSONObject();
        ep.put("address", firstPresent(config.get("address"), "http://" + param.get("host") + ":" + param.get("port")));
        putIfPresent(ep, "username", param.get("username"));
        putIfPresent(ep, "password", param.get("password"));
        ep.put("index", firstPresent(connectionTable(param), config.get("index"), config.get("indexName")));
        putIfPresent(ep, "type", firstPresent(config.get("type"), config.get("docType"), "_doc"));
        ep.put("column", firstPresent(param.get("target_column"), param.get("column")));
        ep.put("batchSize", param.getOrDefault("batchSize", 1024));
        if (!reader) {
            String writeModeType = param.get("writeModeType") == null ? null : String.valueOf(param.get("writeModeType"));
            ep.put("writeMode", resolveWriteMode(writeModeType, param.get("selectedColumns"), (String) param.get("dbType")));
        }
        putIfPresent(ep, "query", connectionQuery(param));
        return ep;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject datasourceConfig(Map<String, Object> param) {
        Object raw = param.get("datasourceConfig");
        if (raw instanceof Map) {
            return new JSONObject((Map<String, Object>) raw);
        }
        return new JSONObject();
    }

    @SuppressWarnings("unchecked")
    private static Object connectionTable(Map<String, Object> param) {
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) rawConn).get("table");
    }

    @SuppressWarnings("unchecked")
    private static Object connectionQuery(Map<String, Object> param) {
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) rawConn).get("querySql");
    }

    private static Object firstPresent(Object... values) {
        for (Object value : values) {
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return value;
            }
        }
        return "";
    }

    private static void putIfPresent(JSONObject object, String key, Object value) {
        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            object.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildFileReader(Map<String, Object> readerMap) {
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");

        JSONObject reader = new JSONObject();
        reader.put("name", "txtfilereader");

        JSONObject rp = new JSONObject();
        if (param != null) {
            Object path = param.get("path");
            if (path instanceof String) {
                rp.put("path", new String[]{((String) path)});
            } else if (path instanceof List) {
                rp.put("path", path);
            } else {
                rp.put("path", new String[0]);
            }
            rp.put("encoding", "UTF-8");
            rp.put("column", param.get("column"));
            rp.put("fieldDelimiter", ",");
        }
        reader.put("parameter", rp);
        return reader;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray buildTransformer(List<Map<String, Object>> transitionList) {
        JSONArray transformerArr = new JSONArray();
        for (Map<String, Object> transition : transitionList) {
            String componentType = (String) transition.get("componentType");
            Map<String, Object> param = (Map<String, Object>) transition.get("parameter");
            if (param == null) {
                continue;
            }
            JSONObject tf = new JSONObject();
            switch (componentType) {
                case "VALUE_MAP":
                    tf.put("name", "dx_valuemap");
                    tf.put("parameter", new JSONObject(param));
                    break;
                case "FIELD_DERIVATION":
                    tf.put("name", "dx_fieldderivation");
                    tf.put("parameter", new JSONObject(param));
                    break;
                case "DATA_DEDUPLICATION":
                    tf.put("name", "dx_deduplication");
                    tf.put("parameter", new JSONObject(param));
                    break;
                case "SELECT_FIELDS":
                    tf.put("name", "dx_selectfields");
                    tf.put("parameter", new JSONObject(param));
                    break;
                case "ADD_CONSTANT":
                    tf.put("name", "dx_addconstant");
                    tf.put("parameter", new JSONObject(param));
                    break;
                case "SORT_RECORD":
                    tf.put("name", "dx_sort");
                    tf.put("parameter", new JSONObject(param));
                    break;
                case "SPARK_CLEAN":
                    tf.put("name", "dx_clean");
                    tf.put("parameter", new JSONObject(param));
                    break;
                default:
                    tf.put("name", "dx_" + componentType.toLowerCase());
                    tf.put("parameter", new JSONObject(param));
                    break;
            }
            transformerArr.add(tf);
        }
        return transformerArr;
    }

    private static JSONObject buildSetting() {
        JSONObject setting = new JSONObject();

        JSONObject speed = new JSONObject();
        speed.put("channel", 1);
        speed.put("bytes", 0);
        setting.put("speed", speed);

        JSONObject errorLimit = new JSONObject();
        errorLimit.put("record", 0);
        errorLimit.put("percentage", 0.0);
        setting.put("errorLimit", errorLimit);

        JSONObject restore = new JSONObject();
        restore.put("maxRowNumForCheckpoint", 0);
        restore.put("isRestore", false);
        restore.put("restoreColumnName", "");
        restore.put("restoreColumnIndex", 0);
        setting.put("restore", restore);

        JSONObject log = new JSONObject();
        log.put("isLogger", false);
        log.put("level", "debug");
        log.put("path", "");
        log.put("pattern", "");
        setting.put("log", log);

        return setting;
    }

    private static String resolveReaderName(String dbType) {
        if (StringUtils.isBlank(dbType)) return "defaultreader";
        switch (dbType.toUpperCase()) {
            case "MYSQL":    return "mysqlreader";
            case "ORACLE":
            case "ORACLE_12C": return "oraclereader";
            case "POSTGRE_SQL":
            case "POSTGRESQL": return "postgresqlreader";
            case "DORIS": return "dorisreader";
            case "CLICK_HOUSE":
            case "CLICKHOUSE": return "clickhousereader";
            case "HIVE": return "hivereader";
            case "MONGODB": return "mongodbreader";
            case "ELASTICSEARCH":
            case "ES": return "elasticsearchreader";
            case "SQL_SERVER":
            case "SQL_SERVER2008": return "sqlserverreader";
            case "DM8":       return "rdbmsreader";
            case "KINGBASE8": return "kingbaseesreader";
            default:          return dbType.toLowerCase() + "reader";
        }
    }

    private static String resolveWriterName(String dbType) {
        if (StringUtils.isBlank(dbType)) return "defaultwriter";
        switch (dbType.toUpperCase()) {
            case "MYSQL":    return "mysqlwriter";
            case "ORACLE":
            case "ORACLE_12C": return "oraclewriter";
            case "POSTGRE_SQL":
            case "POSTGRESQL": return "postgresqlwriter";
            case "DORIS": return "doriswriter";
            case "CLICK_HOUSE":
            case "CLICKHOUSE": return "clickhousewriter";
            case "HIVE": return "hivewriter";
            case "MONGODB": return "mongodbwriter";
            case "ELASTICSEARCH":
            case "ES": return "elasticsearchwriter";
            case "SQL_SERVER":
            case "SQL_SERVER2008": return "sqlserverwriter";
            case "DM8":       return "rdbmswriter";
            case "KINGBASE8": return "kingbaseeswriter";
            default:          return dbType.toLowerCase() + "writer";
        }
    }

    private static boolean isMongo(String dbType) {
        return "MONGODB".equalsIgnoreCase(dbType);
    }

    private static boolean isElasticsearch(String dbType) {
        return "ELASTICSEARCH".equalsIgnoreCase(dbType) || "ES".equalsIgnoreCase(dbType);
    }

    private static String resolveWriteMode(String writeModeType, Object selectedColumns, String dbType) {
        if ("3".equals(writeModeType)) {
            if (selectedColumns instanceof List && !((List<?>) selectedColumns).isEmpty()) {
                String cols = String.join(",", (List<CharSequence>) (List<?>) selectedColumns);
                if ("DM8".equalsIgnoreCase(dbType)) {
                    return "update-dm (" + cols + ")";
                }
                return "update (" + cols + ")";
            }
            return "update";
        }
        return "insert";
    }
}
