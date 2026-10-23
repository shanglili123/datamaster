package com.datamaster.flinkx.core.transform;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.enums.TaskComponentTypeEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TransformSqlBuilder {

    private static final Map<String, TransitionHandler> HANDLERS = new ConcurrentHashMap<>();

    static {
        register(new ValueMapHandler());
        register(new FieldDerivationHandler());
        register(new DataDeduplicationHandler());
        register(new SelectFieldsHandler());
        register(new AddConstantHandler());
        register(new SortRecordHandler());
        register(new SparkCleanHandler());
        register(new FieldSplitHandler());
        register(new FieldMergeHandler());
    }

    public static void register(TransitionHandler handler) {
        HANDLERS.put(handler.componentType(), handler);
    }

    public static String build(JSONArray transitions, List<String> sourceColumns, String sourceTableName) {
        return build(transitions, sourceColumns, sourceTableName, null);
    }

    /**
     * 在普通转换 SQL 之外补充采集来源元数据。该层运行在 Flink SQL 中，
     * 因此写入时间使用 CURRENT_TIMESTAMP，来源自然键使用稳定的源表+源主键 hash。
     */
    public static String build(JSONArray transitions, List<String> sourceColumns, String sourceTableName,
                               JSONObject sourceMetadata) {
        Map<String, String> colExprs = new LinkedHashMap<>();
        if (sourceColumns.isEmpty()) {
            colExprs.put("*", "*");
        } else {
            for (String col : sourceColumns) {
                colExprs.put(col, quoteId(col));
            }
        }

        List<String> orderByClauses = new ArrayList<>();
        List<String> dedupPartitions = new ArrayList<>();
        List<String> whereClauses = new ArrayList<>();
        boolean hasDedup = false;
        List<String> outputColumns = null;

        if (transitions != null) {
            for (int i = 0; i < transitions.size(); i++) {
                JSONObject trans = transitions.getJSONObject(i);
                String componentType = trans.getString("componentType");
                if (componentType != null) {
                    TaskComponentTypeEnum type = TaskComponentTypeEnum.findEnumByType(componentType);
                    if (type != null) {
                        componentType = type.name();
                    }
                }
                JSONObject param = trans.getJSONObject("parameter");
                if (param == null) continue;

                if ("OUTPUT_PROJECTION".equals(componentType)) {
                    outputColumns = stringList(param.getJSONArray("columns"));
                    continue;
                }

                if ("ROW_FILTER".equals(componentType)) {
                    String condition = param.getString("condition");
                    if (condition != null && !condition.trim().isEmpty()) whereClauses.add("(" + condition + ")");
                    continue;
                }

                if ("DATA_DEDUPLICATION".equals(componentType)) {
                    hasDedup = true;
                }

                TransitionHandler handler = HANDLERS.get(componentType);
                if (handler != null) {
                    handler.apply(colExprs, orderByClauses, dedupPartitions, whereClauses, param);
                }
            }
        }

        appendSourceMetadata(colExprs, sourceColumns, sourceTableName, sourceMetadata);

        // 按 writer 输入字段的顺序投影，包括已存在或本次生成的治理列。
        if (outputColumns != null && !isWildcardColumns(outputColumns)) {
            Map<String, String> selected = new LinkedHashMap<>();
            for (String column : outputColumns) {
                String expression = colExprs.get(column);
                if (expression == null && colExprs.containsKey("*")) expression = quoteId(column);
                if (expression == null) {
                    throw new IllegalArgumentException("输出映射引用了不存在的转换字段：" + column);
                }
                selected.put(column, expression);
            }
            colExprs.clear();
            colExprs.putAll(selected);
        }

        if (colExprs.isEmpty()) return null;

        String whereSql = whereClauses.isEmpty() ? ""
                : " WHERE " + whereClauses.stream().collect(Collectors.joining(" AND "));

        if (hasDedup && !dedupPartitions.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            List<String> outerItems = new ArrayList<>();
            for (String k : colExprs.keySet()) {
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
            if (!orderByClauses.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
            }
            sql.append(") AS `__rn` FROM ").append(sourceTableName);
            sql.append(whereSql);
            sql.append(") WHERE `__rn` = 1");
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
        sql.append(" FROM ").append(sourceTableName);
        sql.append(whereSql);
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
        }
        return sql.toString();
    }

    /** 自定义 SQL 与普通转换保持相同的 writer 输入字段顺序。 */
    public static String projectCustomSql(String customSql, List<String> outputColumns) {
        if (isWildcardColumns(outputColumns)) return customSql;
        String sql = customSql.trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1);
        List<String> items = new ArrayList<>();
        for (String column : outputColumns) items.add(quoteId(column));
        return "SELECT " + String.join(", ", items) + " FROM (" + sql + ") AS __dm_output__";
    }

    /** 为用户自定义转换 SQL 包一层来源元数据投影。 */
    public static String wrapCustomSql(String customSql, List<String> sourceColumns,
                                       String sourceTableName, JSONObject metadata) {
        if (metadata == null || !metadata.getBooleanValue("enabled")
                || customSql == null || customSql.trim().isEmpty()) {
            return customSql;
        }
        String tableColumn = stringOrDefault(metadata, "sourceTableColumn", "source_table");
        String sourceTableExpr = containsColumn(sourceColumns, tableColumn)
                ? "__dm_custom__." + quoteId(tableColumn)
                : literal(stringOrDefault(metadata, "sourceTableValue", sourceTableName));
        List<String> items = new ArrayList<>();
        String writeTimeColumn = stringOrDefault(metadata, "writeTimeColumn", "write_time");
        if (sourceColumns == null || sourceColumns.isEmpty()
                || (sourceColumns.size() == 1 && "*".equals(sourceColumns.get(0)))) {
            items.add("__dm_custom__.*");
        } else {
            for (String sourceColumn : sourceColumns) {
                if (writeTimeColumn.equalsIgnoreCase(sourceColumn)) {
                    items.add("CAST(CURRENT_TIMESTAMP AS TIMESTAMP(3)) AS " + quoteId(writeTimeColumn));
                } else {
                    items.add("__dm_custom__." + quoteId(sourceColumn));
                }
            }
        }
        addMetadataProjection(items, sourceColumns, metadata, "sourceDatasourceColumn", "source_datasource",
                literal(metadata.getString("sourceDatasourceValue")), "includeSourceDatasource");
        addMetadataProjection(items, sourceColumns, metadata, "sourceDatabaseColumn", "source_database",
                literal(metadata.getString("sourceDatabaseValue")), "includeSourceDatabase");
        addMetadataProjection(items, sourceColumns, metadata, "sourceTableColumn", "source_table", sourceTableExpr);
        addMetadataProjection(items, sourceColumns, metadata, "sourceTaskColumn", "source_task",
                literal(metadata.getString("sourceTaskValue")), "includeSourceTask");
        addMetadataProjection(items, sourceColumns, metadata, "sourceBatchColumn", "source_batch",
                literal(metadata.getString("sourceBatchValue")), "includeSourceBatch");
        // 下游连续输出时允许保留原始 source_* 字段，但 write_time 表示当前层写入时间，
        // 因此即使自定义 SQL 已返回同名字段，也重新投影当前时间。
        addMetadataProjection(items, sourceColumns, metadata, "writeTimeColumn", "write_time", "CAST(CURRENT_TIMESTAMP AS TIMESTAMP(3))");
        List<String> keyColumns = stringList(metadata.getJSONArray("naturalKeyColumns"));
        if (enabled(metadata, "includeNaturalKey") && !keyColumns.isEmpty()) {
            List<String> parts = new ArrayList<>();
            parts.add("CAST(" + sourceTableExpr + " AS STRING)");
            for (String keyColumn : keyColumns) {
                parts.add("COALESCE(CAST(__dm_custom__." + quoteId(keyColumn) + " AS STRING), '')");
            }
            addMetadataProjection(items, sourceColumns, metadata, "naturalKeyColumn", "source_natural_key",
                    "MD5(CONCAT_WS('|', " + String.join(", ", parts) + "))");
        }
        addConfiguredSourceProjection(items, sourceColumns, metadata, "includeSourceUpdateTime",
                "sourceUpdateTimeSourceColumn", "sourceUpdateTimeColumn", "source_update_time");
        addConfiguredSourceProjection(items, sourceColumns, metadata, "includeRecordOp",
                "recordOpSourceColumn", "recordOpColumn", "record_op");
        if (enabled(metadata, "includeRowHash")) {
            String rowHashColumn = stringOrDefault(metadata, "rowHashColumn", "source_row_hash");
            if (!containsColumn(sourceColumns, rowHashColumn) && !isWildcardColumns(sourceColumns)) {
                List<String> hashParts = new ArrayList<>();
                for (String sourceColumn : sourceColumns) {
                    if (!isGovernanceColumn(sourceColumn, metadata)) {
                        hashParts.add("COALESCE(CAST(__dm_custom__." + quoteId(sourceColumn) + " AS STRING), '')");
                    }
                }
                if (!hashParts.isEmpty()) {
                    items.add("MD5(CONCAT_WS('|', " + String.join(", ", hashParts) + ")) AS " + quoteId(rowHashColumn));
                }
            }
        }
        return "SELECT " + String.join(", ", items) + " FROM (" + customSql + ") AS __dm_custom__";
    }

    private static void addMetadataProjection(List<String> items, List<String> sourceColumns,
                                              JSONObject metadata, String key,
                                              String defaultColumn, String expression) {
        addMetadataProjection(items, sourceColumns, metadata, key, defaultColumn, expression, null);
    }

    private static void addMetadataProjection(List<String> items, List<String> sourceColumns,
                                              JSONObject metadata, String key,
                                              String defaultColumn, String expression, String includeFlag) {
        if (includeFlag != null && !enabled(metadata, includeFlag)) return;
        String column = stringOrDefault(metadata, key, defaultColumn);
        if (containsColumn(sourceColumns, column)) return;
        items.add(expression + " AS " + quoteId(column));
    }

    private static void addConfiguredSourceProjection(List<String> items, List<String> sourceColumns,
                                                      JSONObject metadata, String includeFlag,
                                                      String sourceKey, String outputKey, String defaultOutput) {
        if (!enabled(metadata, includeFlag)) return;
        String sourceColumn = metadata.getString(sourceKey);
        if (sourceColumn == null || sourceColumn.trim().isEmpty()) return;
        String outputColumn = stringOrDefault(metadata, outputKey, defaultOutput);
        if (containsColumn(sourceColumns, outputColumn)) return;
        if (!containsColumn(sourceColumns, sourceColumn)) return;
        items.add("__dm_custom__." + quoteId(sourceColumn) + " AS " + quoteId(outputColumn));
    }

    private static void appendSourceMetadata(Map<String, String> colExprs, List<String> sourceColumns,
                                             String sourceTableName, JSONObject metadata) {
        if (metadata == null || !metadata.getBooleanValue("enabled")) {
            return;
        }
        String datasourceColumn = stringOrDefault(metadata, "sourceDatasourceColumn", "source_datasource");
        String databaseColumn = stringOrDefault(metadata, "sourceDatabaseColumn", "source_database");
        String tableColumn = stringOrDefault(metadata, "sourceTableColumn", "source_table");
        String taskColumn = stringOrDefault(metadata, "sourceTaskColumn", "source_task");
        String batchColumn = stringOrDefault(metadata, "sourceBatchColumn", "source_batch");
        String writeTimeColumn = stringOrDefault(metadata, "writeTimeColumn", "write_time");
        String naturalKeyColumn = stringOrDefault(metadata, "naturalKeyColumn", "source_natural_key");

        String sourceTableExpr;
        String sourceTableColumn = stringOrDefault(metadata, "sourceTableColumn", "source_table");
        if (containsColumn(sourceColumns, sourceTableColumn)) {
            sourceTableExpr = quoteId(sourceTableColumn);
        } else {
            sourceTableExpr = literal(stringOrDefault(metadata, "sourceTableValue", sourceTableName));
        }

        if (enabled(metadata, "includeSourceDatasource")) {
            putIfAbsent(colExprs, datasourceColumn, literal(metadata.getString("sourceDatasourceValue")));
        }
        if (enabled(metadata, "includeSourceDatabase")) {
            putIfAbsent(colExprs, databaseColumn, literal(metadata.getString("sourceDatabaseValue")));
        }
        putIfAbsent(colExprs, tableColumn, sourceTableExpr);
        if (enabled(metadata, "includeSourceTask")) {
            putIfAbsent(colExprs, taskColumn, literal(metadata.getString("sourceTaskValue")));
        }
        if (enabled(metadata, "includeSourceBatch")) {
            putIfAbsent(colExprs, batchColumn, literal(metadata.getString("sourceBatchValue")));
        }
        colExprs.put(writeTimeColumn, "CAST(CURRENT_TIMESTAMP AS TIMESTAMP(3))");

        List<String> keyColumns = stringList(metadata.getJSONArray("naturalKeyColumns"));
        if (enabled(metadata, "includeNaturalKey") && !keyColumns.isEmpty()) {
            List<String> parts = new ArrayList<>();
            parts.add("CAST(" + sourceTableExpr + " AS STRING)");
            for (String keyColumn : keyColumns) {
                parts.add("COALESCE(CAST(" + quoteId(keyColumn) + " AS STRING), '')");
            }
            putIfAbsent(colExprs, naturalKeyColumn,
                    "MD5(CONCAT_WS('|', " + String.join(", ", parts) + "))");
        }
        addConfiguredSourceExpression(colExprs, sourceColumns, metadata, "includeSourceUpdateTime",
                "sourceUpdateTimeSourceColumn", "sourceUpdateTimeColumn", "source_update_time");
        addConfiguredSourceExpression(colExprs, sourceColumns, metadata, "includeRecordOp",
                "recordOpSourceColumn", "recordOpColumn", "record_op");
        if (enabled(metadata, "includeRowHash") && !isWildcardColumns(sourceColumns)) {
            String rowHashColumn = stringOrDefault(metadata, "rowHashColumn", "source_row_hash");
            if (!containsColumn(sourceColumns, rowHashColumn)) {
                List<String> hashParts = new ArrayList<>();
                for (String sourceColumn : sourceColumns) {
                    if (!isGovernanceColumn(sourceColumn, metadata)) {
                        hashParts.add("COALESCE(CAST(" + quoteId(sourceColumn) + " AS STRING), '')");
                    }
                }
                if (!hashParts.isEmpty()) {
                    putIfAbsent(colExprs, rowHashColumn,
                            "MD5(CONCAT_WS('|', " + String.join(", ", hashParts) + "))");
                }
            }
        }
    }

    private static void addConfiguredSourceExpression(Map<String, String> expressions, List<String> sourceColumns,
                                                      JSONObject metadata, String includeFlag,
                                                      String sourceKey, String outputKey, String defaultOutput) {
        if (!enabled(metadata, includeFlag)) return;
        String sourceColumn = metadata.getString(sourceKey);
        if (sourceColumn == null || sourceColumn.trim().isEmpty() || !containsColumn(sourceColumns, sourceColumn)) return;
        String outputColumn = stringOrDefault(metadata, outputKey, defaultOutput);
        if (!containsColumn(sourceColumns, outputColumn)) {
            putIfAbsent(expressions, outputColumn, quoteId(sourceColumn));
        }
    }

    /**
     * 行 Hash 只针对业务字段计算。治理列可能使用自定义列名，不能只按 source_ 前缀排除。
     */
    private static boolean isGovernanceColumn(String column, JSONObject metadata) {
        if (column == null) return true;
        String value = column.trim();
        JSONArray preservedColumns = metadata == null
                ? null : metadata.getJSONArray("preservedMetadataColumns");
        if (preservedColumns != null) {
            for (int i = 0; i < preservedColumns.size(); i++) {
                String preserved = preservedColumns.getString(i);
                if (preserved != null && preserved.equalsIgnoreCase(value)) {
                    return true;
                }
            }
        }
        String[] keys = {
                "sourceTableColumn", "writeTimeColumn", "sourceDatasourceColumn",
                "sourceDatabaseColumn", "sourceTaskColumn", "sourceBatchColumn",
                "naturalKeyColumn", "rowHashColumn", "sourceUpdateTimeColumn", "recordOpColumn"
        };
        String[] defaults = {
                "source_table", "write_time", "source_datasource", "source_database",
                "source_task", "source_batch", "source_natural_key", "source_row_hash",
                "source_update_time", "record_op"
        };
        for (int i = 0; i < keys.length; i++) {
            String configured = stringOrDefault(metadata, keys[i], defaults[i]);
            if (configured.equalsIgnoreCase(value) || defaults[i].equalsIgnoreCase(value)) {
                return true;
            }
        }
        return value.toLowerCase().startsWith("source_");
    }

    private static boolean isWildcardColumns(List<String> columns) {
        return columns == null || columns.isEmpty()
                || (columns.size() == 1 && "*".equals(columns.get(0)));
    }

    private static boolean enabled(JSONObject metadata, String key) {
        if (metadata == null) return false;
        Object value = metadata.get(key);
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value))
                || "1".equals(String.valueOf(value));
    }

    private static void putIfAbsent(Map<String, String> values, String key, String expression) {
        if (key != null && !key.trim().isEmpty() && !values.containsKey(key)) {
            values.put(key, expression == null ? "NULL" : expression);
        }
    }

    private static boolean containsColumn(List<String> columns, String column) {
        if (columns == null || column == null) return false;
        for (String item : columns) {
            if (column.equalsIgnoreCase(item)) return true;
        }
        return false;
    }

    private static String stringOrDefault(JSONObject object, String key, String defaultValue) {
        String value = object == null ? null : object.getString(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String literal(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return "NULL";
        }
        return "'" + escapeSql(value) + "'";
    }

    private static List<String> stringList(JSONArray values) {
        List<String> result = new ArrayList<>();
        if (values == null) return result;
        for (int i = 0; i < values.size(); i++) {
            String value = values.getString(i);
            if (value != null && !value.trim().isEmpty()) result.add(value.trim());
        }
        return result;
    }

    private static String quoteId(String name) {
        if (name == null || name.isEmpty() || "*".equals(name)) return name;
        if (name.startsWith("`") && name.endsWith("`")) return name;
        return "`" + name.replace("`", "``") + "`";
    }

    private static String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
    }

    /** Preserve empty strings and the text "null" in field processing. */
    private static String exactString(String value) {
        return value == null ? "NULL" : "'" + escapeSql(value) + "'";
    }

    private static String sqlType(String type, JSONObject config) {
        switch (type.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "string": case "internetaddress": return "STRING";
            case "binary": return "BYTES";
            case "int": case "integer": return "INTEGER";
            case "long": case "bigint": return "BIGINT";
            case "double": case "number": return "DOUBLE";
            case "boolean": return "BOOLEAN";
            case "date": return "DATE";
            case "timestamp": return "TIMESTAMP";
            case "decimal": case "bignumber":
                int precision = config.getInteger("precision") == null ? 38 : config.getIntValue("precision");
                int scale = config.getInteger("scale") == null ? 18 : config.getIntValue("scale");
                if (precision < 1 || precision > 38 || scale < 0 || scale > precision) {
                    throw new IllegalArgumentException("DECIMAL 要求 1 <= precision <= 38 且 0 <= scale <= precision");
                }
                return "DECIMAL(" + precision + ", " + scale + ")";
            default: throw new IllegalArgumentException("不支持的转换类型: " + type);
        }
    }

    private static String typedLiteral(Object value, String type, JSONObject config) {
        String target = sqlType(type, config);
        if (value == null) return "CAST(NULL AS " + target + ")";
        String text = String.valueOf(value);
        try {
            switch (type.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "string": return exactString(text);
                case "double": case "number":
                    double number = Double.parseDouble(text);
                    if (Double.isNaN(number) || Double.isInfinite(number))
                        throw new IllegalArgumentException("不是有限浮点值: " + text);
                    break;
                case "date": java.time.LocalDate.parse(text); break;
                case "timestamp": java.sql.Timestamp.valueOf(text); break;
                case "int": case "integer": Integer.parseInt(text); break;
                case "long": case "bigint": Long.parseLong(text); break;
                case "decimal": case "bignumber":
                    java.math.BigDecimal decimal = new java.math.BigDecimal(text);
                    int scale = config.getInteger("scale") == null ? 18 : config.getIntValue("scale");
                    int precision = config.getInteger("precision") == null ? 38 : config.getIntValue("precision");
                    if (decimal.setScale(scale, java.math.RoundingMode.UNNECESSARY).precision() > precision)
                        throw new IllegalArgumentException("DECIMAL 常量超出精度: " + text);
                    break;
                case "boolean":
                    if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text))
                        throw new IllegalArgumentException("不是合法布尔值: " + text);
                    return text.toUpperCase(java.util.Locale.ROOT);
                default: throw new IllegalArgumentException("值映射不支持的输出类型: " + type);
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("常量无法转换为 " + target + ": " + text, ex);
        }
        return "CAST(" + exactString(text) + " AS " + target + ")";
    }

    private static String castExpression(String source, String type, JSONObject config) {
        String failure = stringOrDefault(config, "castFailure", "FAIL").toUpperCase(java.util.Locale.ROOT);
        String target = sqlType(type, config);
        switch (failure) {
            case "FAIL": return "CAST(" + source + " AS " + target + ")";
            case "NULL": return "TRY_CAST(" + source + " AS " + target + ")";
            case "DEFAULT":
                if (!config.containsKey("defaultValue"))
                    throw new IllegalArgumentException("castFailure=DEFAULT 必须配置 defaultValue");
                String attempt = "TRY_CAST(" + source + " AS " + target + ")";
                // A genuine input NULL remains NULL; the default applies only to failed non-NULL input.
                return "CASE WHEN " + source + " IS NULL THEN CAST(NULL AS " + target + ") ELSE COALESCE("
                        + attempt + ", " + typedLiteral(config.get("defaultValue"), type, config) + ") END";
            default: throw new IllegalArgumentException("未知的转换失败策略: " + failure);
        }
    }

    // ========================================================================
    // VALUE_MAP
    // ========================================================================

    private static class ValueMapHandler implements TransitionHandler {
        @Override
        public String componentType() { return "VALUE_MAP"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            String inputField = param.getString("inputField");
            String outputField = param.getString("outputField");
            JSONArray mappings = param.getJSONArray("tableFields");
            if (inputField == null || outputField == null || mappings == null || mappings.isEmpty()) return;
            boolean legacy = !param.containsKey("outputType") && !param.containsKey("unmatched");
            for (int i = 0; i < mappings.size(); i++) {
                if (mappings.getJSONObject(i).containsKey("matchType")) legacy = false;
            }
            if (legacy) {
                StringBuilder old = new StringBuilder("CASE ").append(quoteId(inputField));
                for (int i = 0; i < mappings.size(); i++) {
                    JSONObject mapping = mappings.getJSONObject(i);
                    String source = mapping.getString("source");
                    String target = mapping.getString("target");
                    if (source != null && target != null) {
                        old.append(" WHEN ").append(exactString(source)).append(" THEN ").append(exactString(target));
                    }
                }
                String defaultValue = param.getString("defaultValue");
                old.append(" ELSE ").append(defaultValue == null ? quoteId(inputField) : exactString(defaultValue));
                colExprs.put(outputField, old.append(" END").toString());
                return;
            }
            String outputType = stringOrDefault(param, "outputType", "string");
            String unmatched = stringOrDefault(param, "unmatched",
                    param.get("defaultValue") == null ? "KEEP" : "DEFAULT").toUpperCase(java.util.Locale.ROOT);
            if ("FAIL".equals(unmatched)) {
                throw new IllegalArgumentException("值映射 unmatched=FAIL 暂不支持可靠的逐行失败，请使用 KEEP/DEFAULT/NULL");
            }
            String input = colExprs.getOrDefault(inputField, quoteId(inputField));
            StringBuilder expr = new StringBuilder("CASE");
            int count = 0;
            for (int i = 0; i < mappings.size(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                String match = stringOrDefault(mapping, "matchType", "VALUE").toUpperCase(java.util.Locale.ROOT);
                String source = mapping.getString("source");
                if ("VALUE".equals(match) && source == null) continue;
                if (!mapping.containsKey("target")) continue;
                String condition;
                if ("NULL".equals(match)) condition = input + " IS NULL";
                else if ("EMPTY".equals(match)) condition = "CAST(" + input + " AS STRING) = ''";
                else if ("VALUE".equals(match)) condition = "CAST(" + input + " AS STRING) = " + exactString(source);
                else throw new IllegalArgumentException("未知的值映射匹配类型: " + match);
                expr.append(" WHEN ").append(condition).append(" THEN ")
                        .append(typedLiteral(mapping.get("target"), outputType, param));
                count++;
            }
            if (count == 0) return;
            String fallback;
            switch (unmatched) {
                case "KEEP": fallback = castExpression(input, outputType, param); break;
                case "DEFAULT": fallback = typedLiteral(param.get("defaultValue"), outputType, param); break;
                case "NULL": fallback = "CAST(NULL AS " + sqlType(outputType, param) + ")"; break;
                default: throw new IllegalArgumentException("未知的值映射未匹配策略: " + unmatched);
            }
            colExprs.put(outputField, expr.append(" ELSE ").append(fallback).append(" END").toString());
        }
    }

    // ========================================================================
    // FIELD_DERIVATION
    // ========================================================================

    private static class FieldDerivationHandler implements TransitionHandler {
        @Override
        public String componentType() { return "FIELD_DERIVATION"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            String type = param.getString("fieldDerivationType");
            if (type == null) return;
            if ("FIELD_DERIVE_CONCAT".equals(type)) {
                applyFieldConcat(colExprs, param);
            } else if ("FIELD_DERIVE_SUBSTRING".equals(type)) {
                applyFieldSubstring(colExprs, param);
            } else if ("FIELD_DERIVE_REPLACE".equals(type)) {
                applyFieldReplace(colExprs, param);
            } else if ("FIELD_DERIVE_EXPRESSION".equals(type)) {
                applyFieldExpression(colExprs, param);
            } else if ("FIELD_DERIVE_HASH".equals(type)) {
                applyFieldHash(colExprs, param);
            } else if ("FIELD_DERIVE_REGEX".equals(type)) {
                applyFieldRegex(colExprs, param);
            } else if ("FIELD_DERIVE_NULL".equals(type) || "8".equals(type)) {
                applyFieldNull(colExprs, param);
            } else if ("FIELD_DERIVE_CONSTANT".equals(type)) {
                applyFieldConstant(colExprs, param);
            }
            String resultType = param.getString("resultType");
            String output = param.getString("fieldDerivationName");
            if (resultType != null && !resultType.trim().isEmpty() && colExprs.containsKey(output)) {
                colExprs.put(output, castExpression(colExprs.get(output), resultType, param));
            }
        }

        private void applyFieldConcat(Map<String, String> colExprs, JSONObject param) {
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
                    String source = colExprs.getOrDefault(colName, quoteId(colName));
                    String part = "CAST(" + source + " AS VARCHAR)";
                    String mode = stringOrDefault(param, "nullMode", "NULL_LITERAL").toUpperCase(java.util.Locale.ROOT);
                    if ("NULL_LITERAL".equals(mode)) part = "COALESCE(" + part + ", 'null')";
                    else if ("EMPTY".equals(mode)) part = "COALESCE(" + part + ", '')";
                    else if (!"KEEP".equals(mode) && !"SKIP".equals(mode))
                        throw new IllegalArgumentException("未知的拼接空值策略: " + mode);
                    colParts.add(part);
                }
            }
            if (colParts.isEmpty()) return;

            StringBuilder expr = new StringBuilder("CONCAT(");
            List<String> concatParts = new ArrayList<>();
            if (!prefix.isEmpty()) {
                concatParts.add("'" + escapeSql(prefix) + "'");
            }
            String nullMode = stringOrDefault(param, "nullMode", "NULL_LITERAL").toUpperCase(java.util.Locale.ROOT);
            if ("KEEP".equals(nullMode)) {
                List<String> withSeparators = new ArrayList<>();
                for (String part : colParts) {
                    if (!withSeparators.isEmpty() && !delimiter.isEmpty()) withSeparators.add(exactString(delimiter));
                    withSeparators.add(part);
                }
                concatParts.addAll(withSeparators);
            } else if (delimiter.isEmpty() && !"SKIP".equals(nullMode)) {
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

        private void applyFieldSubstring(Map<String, String> colExprs, JSONObject param) {
            String fieldDerivationName = param.getString("fieldDerivationName");
            String direction = param.getString("direction");
            Integer startIndex = param.getInteger("startIndex");
            if (startIndex == null) {
                // 前端预览默认 0（从第 1 位开始），后端保持一致，避免字段派生静默丢失
                startIndex = 0;
            }
            Integer endIndex = param.getInteger("endIndex");
            JSONArray tableFields = param.getJSONArray("tableFields");
            if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;
            String columnName = tableFields.getJSONObject(0).getString("columnName");
            if (columnName == null) return;

            StringBuilder expr = new StringBuilder();
            if ("2".equalsIgnoreCase(direction)) {
                if (endIndex != null) {
                    int subLen = Math.max(1, endIndex - startIndex);
                    expr.append("SUBSTRING(").append(quoteId(columnName))
                            .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1, ")
                            .append(subLen).append(")");
                } else {
                    expr.append("SUBSTRING(").append(quoteId(columnName))
                            .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1)");
                }
            } else {
                if (endIndex != null) {
                    int subLen = Math.max(1, endIndex - startIndex);
                    expr.append("SUBSTRING(").append(quoteId(columnName))
                            .append(", ").append(startIndex + 1).append(", ").append(subLen).append(")");
                } else {
                    expr.append("SUBSTRING(").append(quoteId(columnName))
                            .append(", ").append(startIndex + 1).append(")");
                }
            }
            colExprs.put(fieldDerivationName, expr.toString());
        }

        private void applyFieldReplace(Map<String, String> colExprs, JSONObject param) {
            String output = param.getString("fieldDerivationName");
            String column = firstColumn(param);
            if (output == null || column == null) return;
            String search = firstString(param, "searchValue", "oldValue", "search", "pattern");
            String replacement = firstString(param, "replaceValue", "newValue", "replacement");
            if (search == null || replacement == null) return;
            String source = "CAST(" + quoteId(column) + " AS VARCHAR)";
            colExprs.put(output, "REPLACE(" + source + ", '" + escapeSql(search)
                    + "', '" + escapeSql(replacement) + "')");
        }

        private void applyFieldExpression(Map<String, String> colExprs, JSONObject param) {
            String output = param.getString("fieldDerivationName");
            String expression = firstString(param, "expression", "formula", "sqlExpression");
            if (output == null || expression == null || expression.trim().isEmpty()) return;
            String safeExpression = expression.trim();
            // 表达式允许使用 ${COLUMN} 占位符，但不能拼接多条 SQL 或注释逃逸到查询外。
            if (safeExpression.indexOf(';') >= 0 || safeExpression.contains("--")
                    || safeExpression.contains("/*") || safeExpression.contains("*/")) {
                throw new IllegalArgumentException("字段派生表达式包含禁止的 SQL 分隔符或注释");
            }
            JSONArray fields = param.getJSONArray("tableFields");
            if (fields != null) {
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject field = fields.getJSONObject(i);
                    if (field == null) continue;
                    String column = field.getString("columnName");
                    if (column != null) {
                        safeExpression = safeExpression.replace("${" + column + "}", quoteId(column));
                    }
                }
            }
            colExprs.put(output, safeExpression);
        }

        private void applyFieldHash(Map<String, String> colExprs, JSONObject param) {
            String output = param.getString("fieldDerivationName");
            String column = firstColumn(param);
            if (output == null || column == null) return;
            String algorithm = firstString(param, "algorithm", "hashAlgorithm");
            if (algorithm == null || algorithm.trim().isEmpty()) algorithm = "MD5";
            String normalized = algorithm.replace("-", "").replace("_", "").toUpperCase();
            String source = "CAST(" + quoteId(column) + " AS VARCHAR)";
            if ("MD5".equals(normalized)) {
                colExprs.put(output, "MD5(" + source + ")");
            } else if ("SHA1".equals(normalized)) {
                colExprs.put(output, "SHA1(" + source + ")");
            } else if ("SHA256".equals(normalized) || "SHA2".equals(normalized)) {
                colExprs.put(output, "SHA2(" + source + ", 256)");
            } else {
                throw new IllegalArgumentException("不支持的字段哈希算法: " + algorithm);
            }
        }

        private void applyFieldRegex(Map<String, String> colExprs, JSONObject param) {
            String output = param.getString("fieldDerivationName");
            String column = firstColumn(param);
            String regex = firstString(param, "regex", "pattern");
            if (output == null || column == null || regex == null || regex.isEmpty()) return;
            int group = 0;
            Integer configuredGroup = param.getInteger("group");
            if (configuredGroup == null) configuredGroup = param.getInteger("groupIndex");
            if (configuredGroup != null) group = Math.max(0, configuredGroup);
            colExprs.put(output, "REGEXP_EXTRACT(CAST(" + quoteId(column) + " AS VARCHAR), '"
                    + escapeSql(regex) + "', " + group + ")");
        }

        private void applyFieldNull(Map<String, String> colExprs, JSONObject param) {
            String output = param.getString("fieldDerivationName");
            JSONArray fields = param.getJSONArray("tableFields");
            if (output == null || fields == null || fields.isEmpty()) return;
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < fields.size(); i++) {
                String column = fields.getJSONObject(i).getString("columnName");
                if (column == null) continue;
                String source = "CAST(" + colExprs.getOrDefault(column, quoteId(column)) + " AS STRING)";
                if (param.getBooleanValue("trimWhitespace")) source = "TRIM(" + source + ")";
                if (param.getBooleanValue("normalizeEmpty")) source = "NULLIF(" + source + ", '')";
                parts.add(source);
            }
            if (parts.isEmpty()) return;
            String action = stringOrDefault(param, "nullAction", "COALESCE").toUpperCase(java.util.Locale.ROOT);
            String expression;
            switch (action) {
                case "EMPTY_TO_NULL": expression = "NULLIF(" + parts.get(0) + ", '')"; break;
                case "DEFAULT": expression = "COALESCE(" + parts.get(0) + ", " + exactString(param.getString("defaultValue")) + ")"; break;
                case "COALESCE":
                    if (param.containsKey("defaultValue")) parts.add(exactString(param.getString("defaultValue")));
                    expression = parts.size() == 1 ? parts.get(0) : "COALESCE(" + String.join(", ", parts) + ")";
                    break;
                default: throw new IllegalArgumentException("未知的空值处理: " + action);
            }
            colExprs.put(output, expression);
        }

        private void applyFieldConstant(Map<String, String> colExprs, JSONObject param) {
            String output = param.getString("fieldDerivationName");
            if (output == null) return;
            Object value = param.get("constantValue");
            if (value == null) value = param.get("value");
            if (value == null) value = param.get("defaultValue");
            if (value == null) {
                colExprs.put(output, "NULL");
                return;
            }
            String type = firstString(param, "constantType", "type", "dataType");
            String text = String.valueOf(value);
            if (type != null) {
                String normalized = type.trim().toLowerCase();
                if ("int".equals(normalized) || "integer".equals(normalized)
                        || "long".equals(normalized) || "double".equals(normalized)
                        || "decimal".equals(normalized) || "float".equals(normalized)) {
                    if (!text.matches("[-+]?[0-9]+(\\.[0-9]+)?")) {
                        throw new IllegalArgumentException("常量值不是合法数字: " + text);
                    }
                    colExprs.put(output, text);
                    return;
                }
                if ("boolean".equals(normalized)) {
                    if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                        throw new IllegalArgumentException("常量值不是合法布尔值: " + text);
                    }
                    colExprs.put(output, text.toUpperCase());
                    return;
                }
            }
            colExprs.put(output, "'" + escapeSql(text) + "'");
        }

        private String firstColumn(JSONObject param) {
            JSONArray fields = param.getJSONArray("tableFields");
            if (fields == null || fields.isEmpty()) return null;
            JSONObject first = fields.getJSONObject(0);
            return first == null ? null : first.getString("columnName");
        }

        private String firstString(JSONObject param, String... names) {
            for (String name : names) {
                String value = param.getString(name);
                if (value != null) return value;
            }
            return null;
        }
    }

    // ========================================================================
    // DATA_DEDUPLICATION
    // ========================================================================

    private static class DataDeduplicationHandler implements TransitionHandler {
        @Override
        public String componentType() { return "DATA_DEDUPLICATION"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            JSONArray tableFields = param.getJSONArray("tableFields");
            if (tableFields == null) return;
            for (int i = 0; i < tableFields.size(); i++) {
                JSONObject field = tableFields.getJSONObject(i);
                String columnName = field.getString("columnName");
                if (columnName == null) continue;
                boolean selected = field.getBooleanValue("selected");
                if (!selected) continue;
                String ignoreCase = field.getString("ignoreCase");
                if ("2".equals(ignoreCase)) {
                    dedupPartitions.add("LOWER(" + quoteId(columnName) + ")");
                } else {
                    dedupPartitions.add(quoteId(columnName));
                }
            }
        }
    }

    // ========================================================================
    // SELECT_FIELDS
    // ========================================================================

    private static class SelectFieldsHandler implements TransitionHandler {
        @Override
        public String componentType() { return "SELECT_FIELDS"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            JSONArray tableFields = param.getJSONArray("tableFields");
            JSONArray removeFields = param.getJSONArray("removeFields");
            if (tableFields != null) {
                for (int i = 0; i < tableFields.size(); i++) {
                    JSONObject field = tableFields.getJSONObject(i);
                    String inputField = field.getString("inputField");
                    String outputField = field.getString("outputField");
                    if (outputField == null || inputField == null) continue;

                    String expr = colExprs.getOrDefault(inputField, quoteId(inputField));
                    String type = field.getString("type");
                    Integer length = field.getInteger("length");
                    Integer precision = field.getInteger("precision");

                    if (type != null) {
                        expr = castExpression(expr, type, field);
                        switch (type.toLowerCase(java.util.Locale.ROOT)) {
                            case "int":
                            case "integer":
                                if (length != null && length > 0) {
                                    int maxVal = length > 9 ? Integer.MAX_VALUE : (int) Math.pow(10, length) - 1;
                                    expr = "CASE WHEN " + expr + " > " + maxVal + " THEN " + maxVal
                                            + " WHEN " + expr + " < " + (-maxVal) + " THEN " + (-maxVal)
                                            + " ELSE " + expr + " END";
                                }
                                break;
                            case "long":
                                if (length != null && length > 0) {
                                    long maxVal = length > 18 ? Long.MAX_VALUE : (long) Math.pow(10, length) - 1;
                                    expr = "CASE WHEN " + expr + " > " + maxVal + " THEN " + maxVal
                                            + " WHEN " + expr + " < " + (-maxVal) + " THEN " + (-maxVal)
                                            + " ELSE " + expr + " END";
                                }
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
    }

    // ========================================================================
    // ADD_CONSTANT
    // ========================================================================

    private static class AddConstantHandler implements TransitionHandler {
        @Override
        public String componentType() { return "ADD_CONSTANT"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
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
    }

    // ========================================================================
    // SORT_RECORD
    // ========================================================================

    private static class SortRecordHandler implements TransitionHandler {
        @Override
        public String componentType() { return "SORT_RECORD"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
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
    }

    // ========================================================================
    // SPARK_CLEAN
    // ========================================================================

    private static class SparkCleanHandler implements TransitionHandler {
        @Override
        public String componentType() { return "SPARK_CLEAN"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            String where = param.getString("where");
            if (where != null && !where.trim().isEmpty()) {
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
                if (whereClause != null && !whereClause.trim().isEmpty()) {
                    whereClauses.add(whereClause);
                }
                applyCleanRule(colExprs, whereClauses, ruleType, cfg);
            }
        }

        private void applyCleanRule(Map<String, String> colExprs, List<String> whereClauses,
                                    String ruleType, JSONObject cfg) {
            switch (ruleType) {
                case "WITHIN_BOUNDARY": applyCleanBoundary(colExprs, cfg); break;
                case "REMOVE_WHITESPACE": applyCleanTrim(colExprs, cfg); break;
                case "SIMPLE_REPLACE": applyCleanRegexReplace(colExprs, cfg); break;
                case "REMOVE_EMPTY_COMBINATION": applyCleanDeleteNullCombination(whereClauses, cfg); break;
                case "TO_UPPERCASE": applyCleanCase(colExprs, cfg, true); break;
                case "TO_LOWERCASE": applyCleanCase(colExprs, cfg, false); break;
                case "ADD_PREFIX_SUFFIX": applyCleanPrefixSuffix(colExprs, cfg); break;
                case "MENU_CUSTOM": applyCleanEnumMap(colExprs, cfg); break;
                case "KEEP_LATEST_OR_FIRST": break;
                case "CHECK_EXPIRATION": applyCleanExpiration(colExprs, whereClauses, cfg); break;
                case "FIX_TO_PRECISION": applyCleanRound(colExprs, cfg); break;
                case "DATE_FORMAT_STD": applyCleanDateFormat(colExprs, cfg); break;
                case "STRING_SUBSTR": applyCleanSubstr(colExprs, cfg); break;
            }
        }

        private List<String> cleanColumns(JSONObject cfg) {
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

        private void applyCleanBoundary(Map<String, String> colExprs, JSONObject cfg) {
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
                default: return;
            }
            colExprs.put(col, expr);
        }

        private void applyCleanTrim(Map<String, String> colExprs, JSONObject cfg) {
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

        private void applyCleanRegexReplace(Map<String, String> colExprs, JSONObject cfg) {
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

        private void applyCleanDeleteNullCombination(List<String> whereClauses, JSONObject cfg) {
            List<String> cols = cleanColumns(cfg);
            if (cols.isEmpty()) return;
            List<String> nullChecks = new ArrayList<>();
            for (String c : cols) {
                nullChecks.add("(" + quoteId(c) + " IS NULL OR TRIM(CAST(" + quoteId(c) + " AS VARCHAR)) = '')");
            }
            whereClauses.add("NOT (" + String.join(" AND ", nullChecks) + ")");
        }

        private void applyCleanCase(Map<String, String> colExprs, JSONObject cfg, boolean upper) {
            List<String> cols = cleanColumns(cfg);
            if (cols.isEmpty()) return;
            String col = cols.get(0);
            String colExpr = colExprs.getOrDefault(col, quoteId(col));
            colExprs.put(col, (upper ? "UPPER" : "LOWER") + "(" + colExpr + ")");
        }

        private void applyCleanPrefixSuffix(Map<String, String> colExprs, JSONObject cfg) {
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

        private void applyCleanEnumMap(Map<String, String> colExprs, JSONObject cfg) {
            List<String> cols = cleanColumns(cfg);
            if (cols.isEmpty()) return;
            String col = cols.get(0);
            Object rawMappings = cfg.get("stringValue");
            if (!(rawMappings instanceof JSONArray)) return;
            JSONArray mappings = (JSONArray) rawMappings;
            if (mappings.isEmpty()) return;
            String colExpr = colExprs.getOrDefault(col, quoteId(col));
            StringBuilder expr = new StringBuilder("CASE");
            for (int i = 0; i < mappings.size(); i++) {
                JSONObject m = mappings.getJSONObject(i);
                String val = m.getString("value");
                String name = m.getString("name");
                if (val != null && name != null) {
                    expr.append(" WHEN ").append(colExpr).append(" = '").append(escapeSql(val)).append("' THEN '").append(escapeSql(name)).append("'");
                }
            }
            expr.append(" ELSE ").append(colExpr).append(" END");
            colExprs.put(col, expr.toString());
        }

        private void applyCleanExpiration(Map<String, String> colExprs,
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
                            condition = dateExpr + " < CURRENT_DATE - INTERVAL '" + (dataRangeValue != null ? dataRangeValue : 30) + "' DAY";
                            break;
                        case 2:
                            condition = dateExpr + " < CURRENT_DATE - INTERVAL '" + (dataRangeValue != null ? dataRangeValue : 1) + "' MONTH";
                            break;
                        case 3:
                            condition = dateExpr + " < CURRENT_DATE - INTERVAL '" + (dataRangeValue != null ? dataRangeValue : 1) + "' YEAR";
                            break;
                        default: return;
                    }
                } else { return; }
            }

            if (handleType != null && handleType == 0 && handleColumns != null) {
                String hcExpr = colExprs.getOrDefault(handleColumns, quoteId(handleColumns));
                String setVal = handleValue != null ? "'" + escapeSql(handleValue) + "'" : "NULL";
                colExprs.put(handleColumns, "CASE WHEN " + condition + " THEN " + setVal + " ELSE " + hcExpr + " END");
            } else {
                whereClauses.add("NOT (" + condition + ")");
            }
        }

        private void applyCleanRound(Map<String, String> colExprs, JSONObject cfg) {
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

        private void applyCleanDateFormat(Map<String, String> colExprs, JSONObject cfg) {
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

        private void applyCleanSubstr(Map<String, String> colExprs, JSONObject cfg) {
            List<String> cols = cleanColumns(cfg);
            if (cols.isEmpty()) return;
            String col = cols.get(0);
            Integer maxLength = cfg.getInteger("maxLength");
            if (maxLength == null) return;
            String direction = cfg.getString("direction");
            if (direction == null) direction = "1";
            String colExpr = colExprs.getOrDefault(col, quoteId(col));
            if ("2".equals(direction)) {
                colExprs.put(col, "CASE WHEN LENGTH(" + colExpr + ") > " + maxLength
                        + " THEN SUBSTRING(" + colExpr + ", LENGTH(" + colExpr + ") - " + (maxLength - 1) + ", " + maxLength + ")"
                        + " ELSE " + colExpr + " END");
            } else {
                colExprs.put(col, "CASE WHEN LENGTH(" + colExpr + ") > " + maxLength
                        + " THEN SUBSTRING(" + colExpr + ", 1, " + maxLength + ")"
                        + " ELSE " + colExpr + " END");
            }
        }
    }

    // ========================================================================
    // FIELD_SPLIT
    // ========================================================================

    /**
     * 字段拆分:将源字段按分隔符/位置/正则拆分为多个输出字段。
     *
     * <p>parameter 结构:
     * <pre>
     * {
     *   "splitField": "NAME",        // 被拆分的源字段
     *   "splitType": "delimiter",    // delimiter(按分隔符) / position(按位置) / regex(按正则)
     *   "delimiter": ",",            // splitType=delimiter 时的分隔符
     *   "regex": "",                 // splitType=regex 时的正则
     *   "tableFields": [             // 输出字段列表
     *     { "columnName": "FIRST", "index": 1 },          // delimiter/regex: 第几段(从1开始)
     *     { "columnName": "PART2", "index": 2,
     *       "startIndex": 0, "endIndex": 5 }              // position: 0-based 起止位置
     *   ]
     * }
     * </pre>
     *
     * <p>SQL 函数说明:Flink SPLIT_INDEX 为 zero-based(越界/NULL 返回 NULL),前端 index 从 1 开始,故 -1 对齐。
     */
    private static class FieldSplitHandler implements TransitionHandler {
        @Override
        public String componentType() { return "FIELD_SPLIT"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            String splitField = param.getString("splitField");
            if (splitField == null) return;
            String splitType = param.getString("splitType");
            if (splitType == null) splitType = "delimiter";
            String delimiter = param.getString("delimiter");
            String regex = param.getString("regex");
            JSONArray tableFields = param.getJSONArray("tableFields");
            if (tableFields == null) return;

            String srcExpr = "CAST(" + quoteId(splitField) + " AS VARCHAR)";
            for (int i = 0; i < tableFields.size(); i++) {
                JSONObject field = tableFields.getJSONObject(i);
                String columnName = field.getString("columnName");
                if (columnName == null) continue;

                String expr;
                if ("position".equalsIgnoreCase(splitType)) {
                    Integer startIndex = field.getInteger("startIndex");
                    Integer endIndex = field.getInteger("endIndex");
                    if (startIndex == null) startIndex = 0;
                    if (endIndex != null && endIndex > startIndex) {
                        expr = "SUBSTRING(" + srcExpr + ", " + (startIndex + 1) + ", " + (endIndex - startIndex) + ")";
                    } else {
                        expr = "SUBSTRING(" + srcExpr + ", " + (startIndex + 1) + ")";
                    }
                } else if ("regex".equalsIgnoreCase(splitType)) {
                    Integer index = field.getInteger("index");
                    if (regex == null || index == null) continue;
                    // SPLIT_INDEX 底层按正则切分(zero-based)
                    expr = "SPLIT_INDEX(" + srcExpr + ", '" + escapeSql(regex) + "', " + Math.max(0, index - 1) + ")";
                } else {
                    Integer index = field.getInteger("index");
                    if (delimiter == null || index == null) continue;
                    expr = "SPLIT_INDEX(" + srcExpr + ", '" + escapeSql(delimiter) + "', " + Math.max(0, index - 1) + ")";
                }
                colExprs.put(columnName, expr);
            }
        }
    }

    // ========================================================================
    // FIELD_MERGE
    // ========================================================================

    /**
     * 字段合并:将多个源字段按分隔符合并为一个输出字段。
     *
     * <p>parameter 结构:
     * <pre>
     * {
     *   "mergeFieldName": "FULL_NAME",  // 合并后的输出字段
     *   "delimiter": " ",               // 合并分隔符(可空)
     *   "handleNull": "1",              // 1忽略(NULL字段跳过) 2保留(NULL输出空串) 默认1
     *   "trimSpace": "1",               // 1去除首尾空格 2保留 默认1
     *   "tableFields": [                // 参与合并的源字段
     *     { "columnName": "FIRST_NAME" },
     *     { "columnName": "LAST_NAME" }
     *   ]
     * }
     * </pre>
     */
    private static class FieldMergeHandler implements TransitionHandler {
        @Override
        public String componentType() { return "FIELD_MERGE"; }

        @Override
        public void apply(Map<String, String> colExprs, List<String> orderByClauses,
                          List<String> dedupPartitions, List<String> whereClauses,
                          JSONObject param) {
            String mergeFieldName = param.getString("mergeFieldName");
            if (mergeFieldName == null) return;
            String delimiter = param.getString("delimiter");
            if (delimiter == null) delimiter = "";
            String handleNull = param.getString("handleNull");
            if (handleNull == null) handleNull = "1";
            String trimSpace = param.getString("trimSpace");
            if (trimSpace == null) trimSpace = "1";
            JSONArray tableFields = param.getJSONArray("tableFields");
            if (tableFields == null) return;

            List<String> parts = new ArrayList<>();
            for (int i = 0; i < tableFields.size(); i++) {
                String columnName = tableFields.getJSONObject(i).getString("columnName");
                if (columnName == null) continue;
                String part = "CAST(" + quoteId(columnName) + " AS VARCHAR)";
                if ("1".equals(trimSpace)) {
                    part = "TRIM(" + part + ")";
                }
                if ("2".equals(handleNull)) {
                    part = "COALESCE(" + part + ", '')";
                }
                parts.add(part);
            }
            if (parts.isEmpty()) return;

            String expr;
            if (delimiter.isEmpty()) {
                expr = "CONCAT(" + String.join(", ", parts) + ")";
            } else {
                // CONCAT_WS 自动跳过 NULL 参数,配合 handleNull=1(忽略)使用
                expr = "CONCAT_WS('" + escapeSql(delimiter) + "', " + String.join(", ", parts) + ")";
            }
            colExprs.put(mergeFieldName, expr);
        }
    }
}
