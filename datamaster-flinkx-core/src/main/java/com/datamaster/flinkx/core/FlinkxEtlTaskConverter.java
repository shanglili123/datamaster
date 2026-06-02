package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                String transformSql = buildTransformSql(transitionList, readerMap);
                if (transformSql != null) {
                    JSONObject transformer = new JSONObject();
                    transformer.put("transformSql", transformSql);
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

    // ========================================================================
    // transformSql generation
    // ========================================================================

    private static String buildTransformSql(List<Map<String, Object>> transitionList,
                                            Map<String, Object> readerMap) {
        List<String> originalCols = extractColumns(readerMap);
        Map<String, String> colExprs = new LinkedHashMap<>();
        if (originalCols.isEmpty()) {
            colExprs.put("*", "*");
        } else {
            for (String col : originalCols) {
                colExprs.put(col, quoteId(col));
            }
        }

        List<String> orderByClauses = new ArrayList<>();
        List<String> dedupPartitions = new ArrayList<>();
        List<String> whereClauses = new ArrayList<>();
        boolean hasDedup = false;

        for (Map<String, Object> transition : transitionList) {
            String componentType = (String) transition.get("componentType");
            Map<String, Object> param = (Map<String, Object>) transition.get("parameter");
            if (param == null) continue;

            switch (componentType) {
                case "VALUE_MAP":
                    applyValueMap(colExprs, param);
                    break;
                case "FIELD_DERIVATION":
                    applyFieldDerivation(colExprs, param);
                    break;
                case "DATA_DEDUPLICATION":
                    hasDedup = true;
                    applyDataDeduplication(colExprs, dedupPartitions, param);
                    break;
                case "SELECT_FIELDS":
                    applySelectFields(colExprs, param);
                    break;
                case "ADD_CONSTANT":
                    applyAddConstant(colExprs, param);
                    break;
                case "SORT_RECORD":
                    applySortRecord(orderByClauses, param);
                    break;
                case "SPARK_CLEAN":
                    applyClean(colExprs, whereClauses, param);
                    break;
            }
        }

        if (colExprs.isEmpty()) return null;

        String whereSql = whereClauses.isEmpty() ? "" :
                " WHERE " + whereClauses.stream().collect(Collectors.joining(" AND "));

        if (hasDedup && !dedupPartitions.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            List<String> outerItems = new ArrayList<>();
            for (Map.Entry<String, String> e : colExprs.entrySet()) {
                String k = e.getKey();
                outerItems.add(quoteId(k));
            }
            sql.append(String.join(", ", outerItems));
            sql.append(" FROM (SELECT ");
            List<String> innerItems = new ArrayList<>();
            for (Map.Entry<String, String> e : colExprs.entrySet()) {
                innerItems.add(e.getValue() + " AS " + quoteId(e.getKey()));
            }
            sql.append(String.join(", ", innerItems));
            sql.append(", ROW_NUMBER() OVER (PARTITION BY ");
            sql.append(String.join(", ", dedupPartitions));
            sql.append(") AS \"__rn\" FROM source");
            sql.append(whereSql);
            sql.append(") WHERE \"__rn\" = 1");
            if (!orderByClauses.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
            }
            return sql.toString();
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> selectItems = new ArrayList<>();
        for (Map.Entry<String, String> e : colExprs.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if ("*".equals(k) && "*".equals(v)) {
                selectItems.add("*");
            } else if (v.equals(quoteId(k)) || v.equals(k)) {
                selectItems.add(quoteId(k));
            } else {
                selectItems.add(v + " AS " + quoteId(k));
            }
        }
        sql.append(String.join(", ", selectItems));
        sql.append(" FROM source");
        sql.append(whereSql);
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
        }
        return sql.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractColumns(Map<String, Object> readerMap) {
        List<String> cols = new ArrayList<>();
        if (readerMap == null) return cols;
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");
        if (param == null) return cols;
        Object column = param.get("column");
        if (column instanceof List) {
            for (Object item : (List<Object>) column) {
                if (item instanceof String) {
                    cols.add((String) item);
                } else if (item instanceof Map) {
                    Object name = ((Map<String, Object>) item).get("name");
                    if (name instanceof String) {
                        cols.add((String) name);
                    }
                }
            }
        } else if (column instanceof String && StringUtils.isNotBlank((String) column)) {
            cols.add((String) column);
        }
        return cols;
    }

    private static String quoteId(String name) {
        if (name == null || name.isEmpty() || "*".equals(name)) return name;
        if (name.startsWith("\"") && name.endsWith("\"")) return name;
        if (name.startsWith("`") && name.endsWith("`")) return name;
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    // ========================================================================
    // VALUE_MAP
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyValueMap(Map<String, String> colExprs, Map<String, Object> param) {
        String inputField = (String) param.get("inputField");
        String outputField = (String) param.get("outputField");
        String defaultValue = (String) param.get("defaultValue");
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (inputField == null || outputField == null || tableFields == null || tableFields.isEmpty()) return;

        StringBuilder caseExpr = new StringBuilder("CASE ");
        appendQuoted(caseExpr, inputField);
        for (Map<String, Object> mapping : tableFields) {
            String source = (String) mapping.get("source");
            String target = (String) mapping.get("target");
            if (source != null && target != null) {
                caseExpr.append(" WHEN '").append(escapeSql(source)).append("' THEN '").append(escapeSql(target)).append("'");
            }
        }
        if (defaultValue != null) {
            caseExpr.append(" ELSE '").append(escapeSql(defaultValue)).append("'");
        } else {
            caseExpr.append(" ELSE ").append(quoteId(inputField));
        }
        caseExpr.append(" END");
        colExprs.put(outputField, caseExpr.toString());
    }

    // ========================================================================
    // FIELD_DERIVATION
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyFieldDerivation(Map<String, String> colExprs, Map<String, Object> param) {
        String type = (String) param.get("fieldDerivationType");
        if (type == null) return;
        switch (type) {
            case "FIELD_DERIVE_CONCAT":
                applyFieldConcat(colExprs, param);
                break;
            case "FIELD_DERIVE_SUBSTRING":
                applyFieldSubstring(colExprs, param);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyFieldConcat(Map<String, String> colExprs, Map<String, Object> param) {
        String fieldDerivationName = (String) param.get("fieldDerivationName");
        String delimiter = (String) param.get("delimiter");
        if (delimiter == null) delimiter = "";
        String prefix = (String) param.get("fieldDerivationPrefix");
        if (prefix == null) prefix = "";
        String suffix = (String) param.get("fieldDerivationSuffix");
        if (suffix == null) suffix = "";
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;

        List<String> colParts = new ArrayList<>();
        for (Map<String, Object> field : tableFields) {
            String colName = (String) field.get("columnName");
            if (colName != null) {
                colParts.add("COALESCE(CAST(" + quoteId(colName) + " AS VARCHAR), 'null')");
            }
        }
        if (colParts.isEmpty()) return;

        StringBuilder expr = new StringBuilder();
        expr.append("CONCAT(");
        List<String> concatParts = new ArrayList<>();
        if (!prefix.isEmpty()) {
            concatParts.add("'" + escapeSql(prefix) + "'");
        }
        if (delimiter.isEmpty()) {
            concatParts.addAll(colParts);
        } else {
            StringBuilder ws = new StringBuilder("CONCAT_WS('");
            ws.append(escapeSql(delimiter)).append("'");
            for (String p : colParts) {
                ws.append(", ").append(p);
            }
            ws.append(")");
            concatParts.add(ws.toString());
        }
        if (!suffix.isEmpty()) {
            concatParts.add("'" + escapeSql(suffix) + "'");
        }
        expr.append(String.join(", ", concatParts));
        expr.append(")");
        colExprs.put(fieldDerivationName, expr.toString());
    }

    @SuppressWarnings("unchecked")
    private static void applyFieldSubstring(Map<String, String> colExprs, Map<String, Object> param) {
        String fieldDerivationName = (String) param.get("fieldDerivationName");
        String direction = (String) param.get("direction");
        Integer startIndex = param.get("startIndex") instanceof Number
                ? ((Number) param.get("startIndex")).intValue() : null;
        Integer endIndex = param.get("endIndex") instanceof Number
                ? ((Number) param.get("endIndex")).intValue() : null;
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;
        String columnName = (String) tableFields.get(0).get("columnName");
        if (columnName == null || startIndex == null) return;

        StringBuilder expr = new StringBuilder();
        if ("2".equalsIgnoreCase(direction)) {
            if (endIndex != null) {
                int length = endIndex - startIndex;
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1, ")
                        .append(length).append(")");
            } else {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1)");
            }
        } else {
            if (endIndex != null) {
                int length = endIndex - startIndex;
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", ").append(startIndex + 1).append(", ").append(length).append(")");
            } else {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", ").append(startIndex + 1).append(")");
            }
        }
        colExprs.put(fieldDerivationName, expr.toString());
    }

    // ========================================================================
    // DATA_DEDUPLICATION
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyDataDeduplication(Map<String, String> colExprs,
                                                List<String> dedupPartitions,
                                                Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFields == null || tableFields.isEmpty()) return;
        for (Map<String, Object> field : tableFields) {
            String columnName = (String) field.get("columnName");
            String ignoreCase = (String) field.get("ignoreCase");
            if (columnName == null) continue;
            if ("2".equals(ignoreCase)) {
                dedupPartitions.add("LOWER(" + quoteId(columnName) + ")");
            } else {
                dedupPartitions.add(quoteId(columnName));
            }
        }
    }

    // ========================================================================
    // SELECT_FIELDS
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applySelectFields(Map<String, String> colExprs, Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        List<Map<String, Object>> removeFields = (List<Map<String, Object>>) param.get("removeFields");
        if (tableFields != null) {
            for (Map<String, Object> field : tableFields) {
                String inputField = (String) field.get("inputField");
                String outputField = (String) field.get("outputField");
                String type = (String) field.get("type");
                Integer length = field.get("length") instanceof Number
                        ? ((Number) field.get("length")).intValue() : null;
                Integer precision = field.get("precision") instanceof Number
                        ? ((Number) field.get("precision")).intValue() : null;
                if (outputField == null) continue;
                if (inputField == null) continue;

                String expr = quoteId(inputField);
                if (type != null) {
                    switch (type.toLowerCase()) {
                        case "string":
                            expr = "CAST(" + expr + " AS VARCHAR)";
                            if (length != null && length > 0) {
                                expr = "SUBSTRING(" + expr + ", 1, " + length + ")";
                            }
                            break;
                        case "int":
                        case "integer":
                            expr = "CAST(" + expr + " AS INTEGER)";
                            if (length != null && length > 0) {
                                int maxVal = (int) Math.pow(10, length) - 1;
                                expr = "CASE WHEN " + expr + " > " + maxVal + " THEN " + maxVal
                                        + " WHEN " + expr + " < " + (-maxVal) + " THEN " + (-maxVal)
                                        + " ELSE " + expr + " END";
                            }
                            break;
                        case "long":
                            expr = "CAST(" + expr + " AS BIGINT)";
                            if (length != null && length > 0) {
                                long maxVal = (long) Math.pow(10, length) - 1;
                                expr = "CASE WHEN " + expr + " > " + maxVal + " THEN " + maxVal
                                        + " WHEN " + expr + " < " + (-maxVal) + " THEN " + (-maxVal)
                                        + " ELSE " + expr + " END";
                            }
                            break;
                        case "double":
                            expr = "CAST(" + expr + " AS DOUBLE)";
                            if (precision != null && precision >= 0) {
                                expr = "ROUND(" + expr + ", " + precision + ")";
                            }
                            break;
                        case "boolean":
                            expr = "CAST(" + expr + " AS BOOLEAN)";
                            break;
                        case "date":
                            expr = "CAST(" + expr + " AS DATE)";
                            break;
                        case "timestamp":
                            expr = "CAST(" + expr + " AS TIMESTAMP)";
                            break;
                    }
                }
                if (precision != null && precision >= 0 && type != null && type.equalsIgnoreCase("double")) {
                    expr = "ROUND(" + expr + ", " + precision + ")";
                }
                if (length != null && length > 0 && type != null && type.equalsIgnoreCase("string")) {
                    expr = "SUBSTRING(" + expr + ", 1, " + length + ")";
                }
                colExprs.put(outputField, expr);
            }
        }
        if (removeFields != null) {
            for (Map<String, Object> rf : removeFields) {
                String fn = (String) rf.get("inputField");
                if (fn != null) {
                    colExprs.remove(fn);
                }
            }
        }
    }

    // ========================================================================
    // ADD_CONSTANT
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyAddConstant(Map<String, String> colExprs, Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFields == null) return;
        for (Map<String, Object> field : tableFields) {
            String name = (String) field.get("name");
            String type = (String) field.get("type");
            String defaultValue = (String) field.get("defaultValue");
            Boolean emptyString = field.get("emptyString") instanceof Boolean
                    ? (Boolean) field.get("emptyString") : false;
            if (name == null || colExprs.containsKey(name)) continue;

            if (Boolean.TRUE.equals(emptyString)) {
                colExprs.put(name, "''");
            } else if (defaultValue == null) {
                colExprs.put(name, "NULL");
            } else {
                switch (type != null ? type.toLowerCase() : "string") {
                    case "int":
                    case "integer":
                    case "long":
                        colExprs.put(name, defaultValue);
                        break;
                    case "double":
                        colExprs.put(name, defaultValue);
                        break;
                    case "boolean":
                        colExprs.put(name, defaultValue);
                        break;
                    case "date":
                        colExprs.put(name, "CAST('" + escapeSql(defaultValue) + "' AS TIMESTAMP)");
                        break;
                    default:
                        colExprs.put(name, "'" + escapeSql(defaultValue) + "'");
                        break;
                }
            }
        }
    }

    // ========================================================================
    // SORT_RECORD
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applySortRecord(List<String> orderByClauses, Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFields == null) return;
        for (Map<String, Object> field : tableFields) {
            String columnName = (String) field.get("columnName");
            String order = (String) field.get("order");
            Boolean caseSensitive = field.get("caseSensitive") instanceof Boolean
                    ? (Boolean) field.get("caseSensitive") : false;
            if (columnName == null) continue;

            StringBuilder sortExpr = new StringBuilder();
            if (!Boolean.TRUE.equals(caseSensitive)) {
                sortExpr.append("LOWER(").append(quoteId(columnName)).append(")");
            } else {
                sortExpr.append(quoteId(columnName));
            }
            if ("desc".equalsIgnoreCase(order)) {
                sortExpr.append(" DESC");
            } else {
                sortExpr.append(" ASC");
            }
            orderByClauses.add(sortExpr.toString());
        }
    }

    // ========================================================================
    // SPARK_CLEAN
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyClean(Map<String, String> colExprs,
                                   List<String> whereClauses,
                                   Map<String, Object> param) {
        String where = (String) param.get("where");
        if (StringUtils.isNotBlank(where)) {
            whereClauses.add(where);
        }
        List<Map<String, Object>> tableFieldList = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFieldList == null) return;
        for (Map<String, Object> rule : tableFieldList) {
            String ruleType = (String) rule.get("ruleType");
            String ruleConfigStr = (String) rule.get("ruleConfig");
            if (ruleConfigStr == null) continue;
            JSONObject cfg;
            try {
                cfg = JSONObject.parseObject(ruleConfigStr);
            } catch (Exception e) {
                continue;
            }
            String whereClause = (String) rule.get("whereClause");
            if (StringUtils.isNotBlank(whereClause)) {
                whereClauses.add(whereClause);
            }
            switch (ruleType) {
                case "WITHIN_BOUNDARY":
                    applyCleanBoundary(colExprs, cfg);
                    break;
                case "REMOVE_WHITESPACE":
                    applyCleanTrim(colExprs, cfg);
                    break;
                case "SIMPLE_REPLACE":
                    applyCleanRegexReplace(colExprs, cfg);
                    break;
                case "REMOVE_EMPTY_COMBINATION":
                    applyCleanDeleteNullCombination(colExprs, whereClauses, cfg);
                    break;
                case "TO_UPPERCASE":
                    applyCleanCase(colExprs, cfg, true);
                    break;
                case "TO_LOWERCASE":
                    applyCleanCase(colExprs, cfg, false);
                    break;
                case "ADD_PREFIX_SUFFIX":
                    applyCleanPrefixSuffix(colExprs, cfg);
                    break;
                case "MENU_CUSTOM":
                    applyCleanEnumMap(colExprs, cfg);
                    break;
                case "KEEP_LATEST_OR_FIRST":
                    applyCleanDedupKeepFirst(colExprs, cfg);
                    break;
                case "CHECK_EXPIRATION":
                    applyCleanExpiration(colExprs, whereClauses, cfg);
                    break;
                case "FIX_TO_PRECISION":
                    applyCleanRound(colExprs, cfg);
                    break;
                case "DATE_FORMAT_STD":
                    applyCleanDateFormat(colExprs, cfg);
                    break;
                case "STRING_SUBSTR":
                    applyCleanSubstr(colExprs, cfg);
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> cleanColumns(JSONObject cfg) {
        List<String> cols = new ArrayList<>();
        if (cfg.containsKey("columns")) {
            JSONArray arr = cfg.getJSONArray("columns");
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    cols.add(arr.getString(i));
                }
            }
        } else if (cfg.containsKey("columnName")) {
            cols.add(cfg.getString("columnName"));
        }
        return cols;
    }

    private static void applyCleanBoundary(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        double min = cfg.getDoubleValue("min");
        double max = cfg.getDoubleValue("max");
        int handleType = cfg.getIntValue("handleType");
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String expr;
        switch (handleType) {
            case 1:
                expr = "CASE WHEN " + colExpr + " < " + min + " THEN " + min
                        + " WHEN " + colExpr + " > " + max + " THEN " + max
                        + " ELSE " + colExpr + " END";
                break;
            case 2:
                expr = "CASE WHEN " + colExpr + " < " + min + " OR " + colExpr + " > " + max
                        + " THEN " + min + " ELSE " + colExpr + " END";
                break;
            case 3:
                expr = "CASE WHEN " + colExpr + " < " + min + " OR " + colExpr + " > " + max
                        + " THEN " + max + " ELSE " + colExpr + " END";
                break;
            default:
                return;
        }
        colExprs.put(col, expr);
    }

    private static void applyCleanTrim(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        int handleType = cfg.getIntValue("handleType");
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        if (handleType == 1) {
            colExprs.put(col, "TRIM(" + colExpr + ")");
        } else if (handleType == 2) {
            colExprs.put(col, "REGEXP_REPLACE(" + colExpr + ", '\\\\s+', '')");
        }
    }

    private static void applyCleanRegexReplace(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String regex = cfg.getString("regex");
        String replacement = cfg.getString("replacement");
        if (regex == null) return;
        if (replacement == null) replacement = "";
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        colExprs.put(col, "REGEXP_REPLACE(" + colExpr + ", '" + escapeSql(regex) + "', '" + escapeSql(replacement) + "')");
    }

    private static void applyCleanDeleteNullCombination(Map<String, String> colExprs,
                                                        List<String> whereClauses,
                                                        JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        List<String> nullChecks = new ArrayList<>();
        for (String c : cols) {
            String colExpr = colExprs.getOrDefault(c, quoteId(c));
            nullChecks.add("(" + colExpr + " IS NULL OR TRIM(CAST(" + colExpr + " AS VARCHAR)) = '')");
        }
        whereClauses.add("NOT (" + String.join(" AND ", nullChecks) + ")");
    }

    private static void applyCleanCase(Map<String, String> colExprs, JSONObject cfg, boolean upper) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        colExprs.put(col, (upper ? "UPPER" : "LOWER") + "(" + colExpr + ")");
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanPrefixSuffix(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String stringValue = cfg.getString("stringValue");
        String handleType = cfg.getString("handleType");
        if (stringValue == null || handleType == null) return;
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String sv = escapeSql(stringValue);

        switch (handleType) {
            case "1":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '" + sv + "%' THEN " + colExpr
                        + " ELSE CONCAT('" + sv + "', " + colExpr + ") END");
                break;
            case "2":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '%" + sv + "' THEN " + colExpr
                        + " ELSE CONCAT(" + colExpr + ", '" + sv + "') END");
                break;
            case "3":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '" + sv + "%'"
                        + " THEN SUBSTRING(" + colExpr + ", " + (stringValue.length() + 1) + ")"
                        + " ELSE " + colExpr + " END");
                break;
            case "4":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '%" + sv + "'"
                        + " THEN SUBSTRING(" + colExpr + ", 1, LENGTH(" + colExpr + ") - " + stringValue.length() + ")"
                        + " ELSE " + colExpr + " END");
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanEnumMap(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) cfg.get("stringValue");
        if (mappings == null || mappings.isEmpty()) return;
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        StringBuilder expr = new StringBuilder("CASE");
        for (Map<String, Object> m : mappings) {
            String val = (String) m.get("value");
            String name = (String) m.get("name");
            if (val != null && name != null) {
                expr.append(" WHEN ").append(colExpr).append(" = '").append(escapeSql(val)).append("' THEN '").append(escapeSql(name)).append("'");
            }
        }
        expr.append(" ELSE ").append(colExpr).append(" END");
        colExprs.put(col, expr.toString());
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanDedupKeepFirst(Map<String, String> colExprs, JSONObject cfg) {
        // KEEP_LATEST_OR_FIRST is a dedup operation within CleanTransition
        // We'll use ROW_NUMBER for this as well, but since it's configured differently,
        // we don't modify colExprs here - this would need to be handled at the SQL level
        // which is complex. For now, we skip it as it overlaps with DATA_DEDUPLICATION.
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanExpiration(Map<String, String> colExprs,
                                             List<String> whereClauses,
                                             JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        Integer handleType = cfg.getInteger("handleType");
        String handleColumns = cfg.getString("handleColumns");
        String handleValue = cfg.getString("handleValue");
        Integer dataRange = cfg.getInteger("dataRange");
        Integer dataRangeType = cfg.getInteger("dataRangeType");
        Integer dataRangeValue = cfg.getInteger("dataRangeValue");

        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String dateExpr = "DATE_FORMAT(" + colExpr + ", 'yyyy-MM-dd')";

        String condition;
        if (dataRange != null && dataRange == 1) {
            if (dataRangeType != null && dataRangeType == 1) {
                condition = dateExpr + " > CAST('" + cfg.getString("dataRangeValue") + "' AS DATE)";
            } else {
                condition = dateExpr + " < CAST('" + cfg.getString("dataRangeValue") + "' AS DATE)";
            }
        } else {
            if (dataRangeType != null) {
                switch (dataRangeType) {
                    case 1:
                        condition = dateExpr + " < DATEADD(DAY, -" + (dataRangeValue != null ? dataRangeValue : 30) + ", CURRENT_DATE)";
                        break;
                    case 2:
                        condition = dateExpr + " < DATEADD(MONTH, -" + (dataRangeValue != null ? dataRangeValue : 1) + ", CURRENT_DATE)";
                        break;
                    case 3:
                        condition = dateExpr + " < DATEADD(YEAR, -" + (dataRangeValue != null ? dataRangeValue : 1) + ", CURRENT_DATE)";
                        break;
                    default:
                        return;
                }
            } else {
                return;
            }
        }

        if (handleType != null && handleType == 0 && handleColumns != null) {
            String hcExpr = colExprs.getOrDefault(handleColumns, quoteId(handleColumns));
            String setVal = handleValue != null ? "'" + escapeSql(handleValue) + "'" : "NULL";
            colExprs.put(handleColumns, "CASE WHEN " + condition + " THEN " + setVal + " ELSE " + hcExpr + " END");
        } else {
            whereClauses.add("NOT (" + condition + ")");
        }
    }

    private static void applyCleanRound(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        Integer precision = cfg.getInteger("stringValue");
        if (precision == null) return;
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String expr = "ROUND(" + colExpr + ", " + precision + ")";
        if (precision == 0) {
            expr = "CAST(" + expr + " AS INTEGER)";
        }
        colExprs.put(col, expr);
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanDateFormat(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String targetFormat = cfg.getString("targetFormat");
        JSONArray inputFormats = cfg.getJSONArray("inputFormats");
        if (targetFormat == null) return;

        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        // Build a COALESCE chain of TO_TIMESTAMP attempts
        StringBuilder tsExpr = new StringBuilder();
        if (inputFormats != null && !inputFormats.isEmpty()) {
            for (int i = 0; i < inputFormats.size(); i++) {
                String fmt = inputFormats.getString(i);
                String attempt;
                if ("timestamp".equalsIgnoreCase(fmt)) {
                    attempt = "TO_TIMESTAMP(FROM_UNIXTIME(CAST(REGEXP_REPLACE(CAST(" + colExpr + " AS VARCHAR), '[^0-9]', '') AS DOUBLE) / 1000.0))";
                } else {
                    attempt = "TO_TIMESTAMP(CAST(" + colExpr + " AS VARCHAR), '" + escapeSql(fmt) + "')";
                }
                if (tsExpr.length() == 0) {
                    tsExpr.append(attempt);
                } else {
                    tsExpr.insert(0, "COALESCE(");
                    tsExpr.append(", ").append(attempt).append(")");
                }
            }
        }
        if (tsExpr.length() == 0) return;

        String resultExpr;
        String tl = targetFormat.toLowerCase();
        if ("date".equals(tl)) {
            resultExpr = "CAST(" + tsExpr.toString() + " AS DATE)";
        } else if ("timestamp".equals(tl)) {
            resultExpr = tsExpr.toString();
        } else {
            resultExpr = "DATE_FORMAT(" + tsExpr.toString() + ", '" + escapeSql(targetFormat) + "')";
        }
        colExprs.put(col, "CASE WHEN " + tsExpr.toString() + " IS NOT NULL THEN " + resultExpr
                + " ELSE CAST(" + colExpr + " AS VARCHAR) END");
    }

    private static void applyCleanSubstr(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        Integer maxLength = cfg.getInteger("maxLength");
        if (maxLength == null) return;
        String direction = cfg.getString("direction");
        if (direction == null) direction = "1";
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String expr;
        if ("2".equals(direction)) {
            expr = "CASE WHEN LENGTH(" + colExpr + ") > " + maxLength
                    + " THEN SUBSTRING(" + colExpr + ", LENGTH(" + colExpr + ") - " + (maxLength - 1) + ", " + maxLength + ")"
                    + " ELSE " + colExpr + " END";
        } else {
            expr = "CASE WHEN LENGTH(" + colExpr + ") > " + maxLength
                    + " THEN SUBSTRING(" + colExpr + ", 1, " + maxLength + ")"
                    + " ELSE " + colExpr + " END";
        }
        colExprs.put(col, expr);
    }

    // ========================================================================
    // SQL helpers
    // ========================================================================

    private static void appendQuoted(StringBuilder sb, String name) {
        sb.append(quoteId(name));
    }

    private static String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
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
