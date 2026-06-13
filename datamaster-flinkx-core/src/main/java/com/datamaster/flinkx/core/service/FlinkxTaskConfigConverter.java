package com.datamaster.flinkx.core.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.flinkx.core.model.FlinkxJobBundle;
import com.datamaster.flinkx.core.model.FlinkxJobConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlinkxTaskConfigConverter {

    public static FlinkxJobBundle convert(JSONObject taskParams) {
        JSONObject reader = taskParams.getJSONObject("reader");
        JSONObject writer = taskParams.getJSONObject("writer");
        JSONArray transitions = taskParams.getJSONArray("transition");

        Map<String, Object> flinkx = new LinkedHashMap<>();
        flinkx.put("job", buildJob(taskParams, reader, writer, transitions));
        flinkx.put("content", buildContent(reader, writer, transitions));
        flinkx.put("setting", buildSetting(taskParams));

        FlinkxJobConfig jobConfig = FlinkxJobConfig.builder()
                .jobName(taskParams.getString("name"))
                .jobType("FlinkX")
                .mode("standalone")
                .schedulerName("dolphinscheduler")
                .flinkx(flinkx)
                .build();

        return FlinkxJobBundle.builder()
                .jobConfig(jobConfig)
                .reader(reader == null ? null : reader.toJavaObject(Map.class))
                .writer(writer == null ? null : writer.toJavaObject(Map.class))
                .transitions(transitions)
                .build();
    }

    private static Map<String, Object> buildJob(JSONObject taskParams, JSONObject reader, JSONObject writer, JSONArray transitions) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("content", buildContent(reader, writer, transitions));
        job.put("setting", buildSetting(taskParams));
        return job;
    }

    private static Map<String, Object> buildContent(JSONObject reader, JSONObject writer, JSONArray transitions) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader == null ? new LinkedHashMap<>() : reader.toJavaObject(Map.class));
        content.put("writer", writer == null ? new LinkedHashMap<>() : writer.toJavaObject(Map.class));
        if (transitions != null && !transitions.isEmpty()) {
            String transformSql = buildTransformSql(transitions, reader);
            if (transformSql != null) {
                Map<String, Object> transformer = new LinkedHashMap<>();
                transformer.put("transformSql", transformSql);
                content.put("transformer", transformer);
            }
        }
        return content;
    }

    @SuppressWarnings("unchecked")
    private static String buildTransformSql(JSONArray transitions, JSONObject reader) {
        List<String> originalCols = extractColumns(reader);
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

        for (int i = 0; i < transitions.size(); i++) {
            JSONObject trans = transitions.getJSONObject(i);
            String componentType = trans.getString("componentType");
            JSONObject param = trans.getJSONObject("parameter");
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
                    applyDataDeduplication(dedupPartitions, param);
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
                outerItems.add(quoteId(e.getKey()));
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

    private static List<String> extractColumns(JSONObject reader) {
        List<String> cols = new ArrayList<>();
        if (reader == null) return cols;
        JSONObject param = reader.getJSONObject("parameter");
        if (param == null) return cols;
        Object column = param.get("column");
        if (column instanceof JSONArray) {
            JSONArray arr = (JSONArray) column;
            for (int i = 0; i < arr.size(); i++) {
                Object item = arr.get(i);
                if (item instanceof String) {
                    cols.add((String) item);
                } else if (item instanceof JSONObject) {
                    String name = ((JSONObject) item).getString("name");
                    if (name != null) cols.add(name);
                }
            }
        } else if (column instanceof String && org.apache.commons.lang3.StringUtils.isNotBlank((String) column)) {
            cols.add((String) column);
        }
        return cols;
    }

    // ========================================================================
    // SQL generation helpers
    // ========================================================================

    private static String quoteId(String name) {
        if (name == null || name.isEmpty() || "*".equals(name)) return name;
        if (name.startsWith("\"") && name.endsWith("\"")) return name;
        if (name.startsWith("`") && name.endsWith("`")) return name;
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    private static String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
    }

    // ========================================================================
    // VALUE_MAP
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyValueMap(Map<String, String> colExprs, JSONObject param) {
        String inputField = param.getString("inputField");
        String outputField = param.getString("outputField");
        String defaultValue = param.getString("defaultValue");
        JSONArray tableFields = param.getJSONArray("tableFields");
        if (inputField == null || outputField == null || tableFields == null || tableFields.isEmpty()) return;

        StringBuilder caseExpr = new StringBuilder("CASE ");
        caseExpr.append(quoteId(inputField));
        for (int i = 0; i < tableFields.size(); i++) {
            JSONObject mapping = tableFields.getJSONObject(i);
            String source = mapping.getString("source");
            String target = mapping.getString("target");
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
    private static void applyFieldDerivation(Map<String, String> colExprs, JSONObject param) {
        String type = param.getString("fieldDerivationType");
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

    private static void applyFieldConcat(Map<String, String> colExprs, JSONObject param) {
        String fieldDerivationName = param.getString("fieldDerivationName");
        String delimiter = param.getString("delimiter");
        if (delimiter == null) delimiter = "";
        String prefix = param.getString("fieldDerivationPrefix");
        if (prefix == null) prefix = "";
        String suffix = param.getString("fieldDerivationSuffix");
        if (suffix == null) suffix = "";
        JSONArray tableFields = param.getJSONArray("tableFields");
        if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;

        List<String> colParts = new ArrayList<>();
        for (int i = 0; i < tableFields.size(); i++) {
            String colName = tableFields.getJSONObject(i).getString("columnName");
            if (colName != null) {
                colParts.add("COALESCE(CAST(" + quoteId(colName) + " AS VARCHAR), 'null')");
            }
        }
        if (colParts.isEmpty()) return;

        StringBuilder expr = new StringBuilder("CONCAT(");
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

    private static void applyFieldSubstring(Map<String, String> colExprs, JSONObject param) {
        String fieldDerivationName = param.getString("fieldDerivationName");
        String direction = param.getString("direction");
        Integer startIndex = param.getInteger("startIndex");
        Integer endIndex = param.getInteger("endIndex");
        JSONArray tableFields = param.getJSONArray("tableFields");
        if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;
        String columnName = tableFields.getJSONObject(0).getString("columnName");
        if (columnName == null || startIndex == null) return;

        StringBuilder expr = new StringBuilder();
        if ("2".equalsIgnoreCase(direction)) {
            if (endIndex != null) {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1, ")
                        .append(endIndex - startIndex).append(")");
            } else {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1)");
            }
        } else {
            if (endIndex != null) {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", ").append(startIndex + 1).append(", ").append(endIndex - startIndex).append(")");
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

    private static void applyDataDeduplication(List<String> dedupPartitions, JSONObject param) {
        JSONArray tableFields = param.getJSONArray("tableFields");
        if (tableFields == null) return;
        for (int i = 0; i < tableFields.size(); i++) {
            JSONObject field = tableFields.getJSONObject(i);
            String columnName = field.getString("columnName");
            String ignoreCase = field.getString("ignoreCase");
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
    private static void applySelectFields(Map<String, String> colExprs, JSONObject param) {
        JSONArray tableFields = param.getJSONArray("tableFields");
        JSONArray removeFields = param.getJSONArray("removeFields");
        if (tableFields != null) {
            for (int i = 0; i < tableFields.size(); i++) {
                JSONObject field = tableFields.getJSONObject(i);
                String inputField = field.getString("inputField");
                String outputField = field.getString("outputField");
                String type = field.getString("type");
                Integer length = field.getInteger("length");
                Integer precision = field.getInteger("precision");
                if (outputField == null || inputField == null) continue;

                String expr = quoteId(inputField);
                if (type != null) {
                    switch (type.toLowerCase()) {
                        case "string":
                            expr = "CAST(" + expr + " AS VARCHAR)";
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
                if (precision != null && precision >= 0 && "double".equalsIgnoreCase(type)) {
                    expr = "ROUND(" + expr + ", " + precision + ")";
                }
                if (length != null && length > 0 && "string".equalsIgnoreCase(type)) {
                    expr = "SUBSTRING(" + expr + ", 1, " + length + ")";
                }
                colExprs.put(outputField, expr);
            }
        }
        if (removeFields != null) {
            for (int i = 0; i < removeFields.size(); i++) {
                String fn = removeFields.getJSONObject(i).getString("inputField");
                if (fn != null) colExprs.remove(fn);
            }
        }
    }

    // ========================================================================
    // ADD_CONSTANT
    // ========================================================================

    private static void applyAddConstant(Map<String, String> colExprs, JSONObject param) {
        JSONArray tableFields = param.getJSONArray("tableFields");
        if (tableFields == null) return;
        for (int i = 0; i < tableFields.size(); i++) {
            JSONObject field = tableFields.getJSONObject(i);
            String name = field.getString("name");
            String type = field.getString("type");
            String defaultValue = field.getString("defaultValue");
            boolean emptyString = field.getBooleanValue("emptyString");
            if (name == null || colExprs.containsKey(name)) continue;

            if (emptyString) {
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

    private static void applySortRecord(List<String> orderByClauses, JSONObject param) {
        JSONArray tableFields = param.getJSONArray("tableFields");
        if (tableFields == null) return;
        for (int i = 0; i < tableFields.size(); i++) {
            JSONObject field = tableFields.getJSONObject(i);
            String columnName = field.getString("columnName");
            String order = field.getString("order");
            boolean caseSensitive = field.getBooleanValue("caseSensitive");
            if (columnName == null) continue;

            StringBuilder sortExpr = new StringBuilder();
            if (!caseSensitive) {
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
    private static void applyClean(Map<String, String> colExprs, List<String> whereClauses, JSONObject param) {
        String where = param.getString("where");
        if (org.apache.commons.lang3.StringUtils.isNotBlank(where)) {
            whereClauses.add(where);
        }
        JSONArray tableFieldList = param.getJSONArray("tableFields");
        if (tableFieldList == null) return;
        for (int i = 0; i < tableFieldList.size(); i++) {
            JSONObject rule = tableFieldList.getJSONObject(i);
            String ruleType = rule.getString("ruleType");
            String ruleConfigStr = rule.getString("ruleConfig");
            if (ruleConfigStr == null) continue;
            JSONObject cfg;
            try {
                cfg = JSONObject.parseObject(ruleConfigStr);
            } catch (Exception e) {
                continue;
            }
            String whereClause = rule.getString("whereClause");
            if (org.apache.commons.lang3.StringUtils.isNotBlank(whereClause)) {
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
                    applyCleanDeleteNullCombination(whereClauses, cfg);
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

    private static void applyCleanDeleteNullCombination(List<String> whereClauses, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        List<String> nullChecks = new ArrayList<>();
        for (String c : cols) {
            nullChecks.add("(" + quoteId(c) + " IS NULL OR TRIM(CAST(" + quoteId(c) + " AS VARCHAR)) = '')");
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
                condition = dateExpr + " > CAST('" + escapeSql(cfg.getString("dataRangeValue")) + "' AS DATE)";
            } else {
                condition = dateExpr + " < CAST('" + escapeSql(cfg.getString("dataRangeValue")) + "' AS DATE)";
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
        colExprs.put(col, expr.toString());
    }

    private static Map<String, Object> buildSetting(JSONObject taskParams) {
        Map<String, Object> setting = new LinkedHashMap<>();
        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("channel", 1);
        setting.put("speed", speed);

        Map<String, Object> errorLimit = new LinkedHashMap<>();
        JSONObject settingConfig = nestedObject(taskParams, "setting");
        JSONObject errorLimitConfig = nestedObject(settingConfig, "errorLimit");
        JSONObject restoreConfig = nestedObject(settingConfig, "restore");
        JSONObject logConfig = nestedObject(settingConfig, "log");

        errorLimit.put("record", intValue(firstPresent(errorLimitConfig.get("record"), taskParams.get("errorLimitRecord")), 100));
        errorLimit.put("percentage", doubleValue(firstPresent(errorLimitConfig.get("percentage"), taskParams.get("errorLimitPercentage")), 0.1));
        setting.put("errorLimit", errorLimit);

        Map<String, Object> restore = new LinkedHashMap<>();
        restore.put("maxRowNumForCheckpoint", intValue(firstPresent(restoreConfig.get("maxRowNumForCheckpoint"), taskParams.get("maxRowNumForCheckpoint")), 10000));
        restore.put("isRestore", booleanValue(firstPresent(restoreConfig.get("isRestore"), taskParams.get("isRestore")), true));
        restore.put("restoreColumnName", stringValue(firstPresent(restoreConfig.get("restoreColumnName"), taskParams.get("restoreColumnName")), ""));
        restore.put("restoreColumnIndex", intValue(firstPresent(restoreConfig.get("restoreColumnIndex"), taskParams.get("restoreColumnIndex")), 0));
        setting.put("restore", restore);

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("isLogger", booleanValue(firstPresent(logConfig.get("isLogger"), taskParams.get("isLogger")), true));
        log.put("level", stringValue(firstPresent(logConfig.get("level"), taskParams.get("logLevel")), "info"));
        log.put("path", stringValue(firstPresent(logConfig.get("path"), taskParams.get("logPath")), ""));
        log.put("pattern", stringValue(firstPresent(logConfig.get("pattern"), taskParams.get("logPattern")), ""));
        setting.put("log", log);

        setting.put("resourceMode", taskParams.getString("resourceMode"));
        return setting;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject nestedObject(JSONObject object, String key) {
        if (object == null) {
            return new JSONObject();
        }
        Object value = object.get(key);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof Map) {
            return new JSONObject((Map<String, Object>) value);
        }
        return new JSONObject();
    }

    private static Object firstPresent(Object... values) {
        for (Object value : values) {
            if (value != null && !"".equals(String.valueOf(value).trim())) {
                return value;
            }
        }
        return null;
    }

    private static int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double doubleValue(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value);
        return "1".equals(text) || "yes".equalsIgnoreCase(text) || Boolean.parseBoolean(text);
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
