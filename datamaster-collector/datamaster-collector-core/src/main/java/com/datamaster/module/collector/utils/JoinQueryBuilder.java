package com.datamaster.module.collector.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Builds a join SELECT from structured identifiers only; never accepts SQL expressions. */
public final class JoinQueryBuilder {

    private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_$]*");
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList("=", "!=", ">", ">=", "<", "<="));
    private static final Set<String> FULL_DATABASES = new HashSet<>(Arrays.asList(
            "POSTGRESQL", "POSTGRES", "ORACLE", "ORACLE11", "SQLSERVER", "SQLSERVER2005", "SQLSERVER2008",
            "KINGBASE", "KINGBASE8", "KINGBASEES", "DM", "DM8", "DAMENG"));

    private JoinQueryBuilder() {
    }

    /**
     * Inputs must already follow joinConfig.sources order. Metadata column names are supplied
     * as *Column properties (including sourceTableColumn). write_time is generated downstream.
     * The source-table value identifies this join's physical tables, not an upstream row key.
     *
     * @throws IllegalArgumentException when the structured configuration is invalid
     */
    public static String build(List<Map<String, Object>> inputs, Map<String, Object> joinConfig,
                               String dbType, Map<String, Object> sourceMetadata) {
        if (inputs == null || inputs.size() < 2 || joinConfig == null || sourceMetadata == null) {
            throw invalid("A join requires at least two inputs, joinConfig and sourceMetadata");
        }
        String database = text(dbType, "dbType").toUpperCase(Locale.ROOT).replace("_", "").replace("-", "");
        if ("2".equals(database)) database = "MARIADB";
        if (!FULL_DATABASES.contains(database) && !Arrays.asList("MYSQL", "MARIADB", "DORIS", "CLICKHOUSE", "HIVE").contains(database)) {
            throw invalid("JOIN 暂不支持该数据源类型：" + dbType);
        }
        Dialect dialect = new Dialect(database);
        String sourceColumn = identifier(sourceMetadata.get("sourceTableColumn"), "sourceMetadata.sourceTableColumn");
        Set<String> metadataColumns = new HashSet<>(Arrays.asList("source_table", "write_time", "source_natural_key",
                "source_row_hash", "source_datasource", "source_database", "source_task", "source_batch", "source_update_time", "record_op"));
        for (Map.Entry<String, Object> entry : sourceMetadata.entrySet()) {
            if (entry.getKey().endsWith("Column") && entry.getValue() != null) {
                metadataColumns.add(identifier(entry.getValue(), entry.getKey()).toLowerCase(Locale.ROOT));
            }
        }
        List<?> sources = list(joinConfig.get("sources"), "sources");
        if (sources.size() != inputs.size()) {
            throw invalid("sources must match inputs exactly");
        }
        Map<String, Source> byId = new LinkedHashMap<>();
        Set<String> aliases = new HashSet<>();
        List<String> tables = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            Map<?, ?> input = map(inputs.get(i), "input");
            Map<?, ?> configured = map(sources.get(i), "source");
            String id = text(input.get("refNodeId"), "refNodeId");
            if (!id.equals(text(configured.get("nodeId"), "source.nodeId"))) {
                throw invalid("inputs must follow sources order");
            }
            String alias = identifier(configured.get("alias"), "source.alias");
            if (!alias.equals(identifier(input.get("alias"), "input.alias"))) {
                throw invalid("input alias must match source alias");
            }
            if (!aliases.add(alias.toLowerCase(Locale.ROOT)) || byId.containsKey(id)) {
                throw invalid("Duplicate source alias or nodeId");
            }
            String table = text(input.get("table_name"), "table_name");
            String quotedTable = dialect.table(table);
            Set<String> columns = new HashSet<>();
            for (Object column : list(input.get("columns"), "input.columns")) {
                columns.add(identifier(column, "input column"));
            }
            byId.put(id, new Source(alias, quotedTable, columns));
            tables.add(table);
        }

        List<?> joins = list(joinConfig.get("joins"), "joins");
        if (joins.size() != inputs.size() - 1) {
            throw invalid("Exactly inputs.size - 1 joins are required");
        }
        String firstId = byId.keySet().iterator().next();
        Set<String> joined = new LinkedHashSet<>();
        joined.add(firstId);
        StringBuilder from = new StringBuilder(" FROM ").append(byId.get(firstId).sql(dialect));
        for (Object item : joins) {
            Map<?, ?> join = map(item, "join");
            String rightId = text(join.get("rightNodeId"), "rightNodeId");
            Source right = source(byId, rightId);
            if (joined.contains(rightId)) {
                throw invalid("rightNodeId must introduce a new source");
            }
            String type = text(join.get("type"), "join.type");
            if (!Arrays.asList("INNER", "LEFT", "RIGHT", "FULL").contains(type)) {
                throw invalid("Unsupported join type: " + type);
            }
            if ("FULL".equals(type) && !FULL_DATABASES.contains(database)) {
                throw invalid("FULL JOIN is not supported for database: " + dbType);
            }
            if ("RIGHT".equals(type) && "HIVE".equals(database)) {
                throw invalid("RIGHT JOIN is not supported for Hive");
            }
            if ("CLICKHOUSE".equals(database) && conditionsUseNonEquality(join)) {
                throw invalid("ClickHouse JOIN 当前只支持等值条件");
            }
            String relation = text(join.get("relation"), "join.relation");
            if (!"AND".equals(relation) && !"OR".equals(relation)) {
                throw invalid("join.relation must be AND or OR");
            }
            List<?> conditions = list(join.get("conditions"), "conditions");
            if (conditions.isEmpty()) {
                throw invalid("Join conditions cannot be empty");
            }
            List<String> predicates = new ArrayList<>();
            for (Object condition : conditions) {
                Map<?, ?> predicate = map(condition, "condition");
                String leftId = text(predicate.get("leftNodeId"), "leftNodeId");
                if (!joined.contains(leftId)) {
                    throw invalid("leftNodeId must already be joined");
                }
                String operator = text(predicate.get("operator"), "operator");
                if (!OPERATORS.contains(operator)) {
                    throw invalid("Unsupported condition operator");
                }
                predicates.add(source(byId, leftId).column(predicate.get("leftColumn"), dialect)
                        + " " + operator + " " + right.column(predicate.get("rightColumn"), dialect));
            }
            from.append(" ").append(type).append(" JOIN ").append(right.sql(dialect))
                    .append(" ON (").append(String.join(" " + relation + " ", predicates)).append(")");
            joined.add(rightId);
        }
        if (joined.size() != byId.size()) {
            throw invalid("All upstream sources must participate in the join");
        }

        // 增量窗口必须作用于连接结果：任一侧在本轮变化的配对行重算，另一侧保持全量参与。
        Object rawFilters = joinConfig.get("sourceFilters");
        if (rawFilters instanceof List && !((List<?>) rawFilters).isEmpty()) {
            List<String> changes = new ArrayList<>();
            for (Object item : (List<?>) rawFilters) {
                Map<?, ?> filter = map(item, "sourceFilter");
                String nodeId = text(filter.get("nodeId"), "sourceFilter.nodeId");
                Source source = source(byId, nodeId);
                String column = identifier(filter.get("column"), "sourceFilter.column");
                if (!source.hasColumn(column)) {
                    throw invalid("Unknown cursor column " + column + " for alias " + source.alias);
                }
                Object end = filter.get("end");
                if (end == null || String.valueOf(end).trim().isEmpty()) {
                    changes.add("1 = 0");
                    continue;
                }
                Object start = filter.get("start");
                changes.add(StageIncrementalSql.predicate(column,
                        text(filter.get("cursorType"), "sourceFilter.cursorType"),
                        start == null ? null : String.valueOf(start), String.valueOf(end), dbType, source.alias));
            }
            from.append(" WHERE (").append(String.join(" OR ", changes)).append(")");
        }

        List<?> fields = list(joinConfig.get("fields"), "fields");
        if (fields.isEmpty()) {
            throw invalid("At least one output field is required");
        }
        List<String> projections = new ArrayList<>();
        Set<String> outputNames = new HashSet<>();
        for (Object item : fields) {
            Map<?, ?> field = map(item, "field");
            String output = identifier(field.get("outputName"), "outputName");
            String normalized = output.toLowerCase(Locale.ROOT);
            if (!outputNames.add(normalized) || metadataColumns.contains(normalized)) {
                throw invalid("JOIN 输出字段重名或与追溯列冲突：" + output);
            }
            Source source = source(byId, text(field.get("nodeId"), "field.nodeId"));
            projections.add(source.column(field.get("column"), dialect) + " AS " + dialect.quote(output));
        }
        String label = "JOIN[" + String.join(",", tables) + "]";
        if (label.length() > 256) throw invalid("JOIN 复合来源标记超过 source_table 默认长度256，请缩短表名或减少关联表");
        projections.add("'" + label.replace("'", "''") + "' AS " + dialect.quote(sourceColumn));
        return "SELECT " + String.join(", ", projections) + from;
    }

    private static boolean conditionsUseNonEquality(Map<?, ?> join) {
        for (Object raw : list(join.get("conditions"), "conditions")) {
            if (!"=".equals(map(raw, "condition").get("operator"))) return true;
        }
        return false;
    }

    private static Source source(Map<String, Source> sources, String nodeId) {
        Source source = sources.get(nodeId);
        if (source == null) {
            throw invalid("Unknown source node: " + nodeId);
        }
        return source;
    }

    private static String text(Object value, String name) {
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw invalid(name + " must be a nonempty string");
        }
        return (String) value;
    }

    private static String identifier(Object value, String name) {
        String identifier = text(value, name);
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw invalid("Invalid identifier for " + name + ": " + identifier);
        }
        return identifier;
    }

    private static List<?> list(Object value, String name) {
        if (!(value instanceof List)) {
            throw invalid(name + " must be a list");
        }
        return (List<?>) value;
    }

    private static Map<?, ?> map(Object value, String name) {
        if (!(value instanceof Map)) {
            throw invalid(name + " must be an object");
        }
        return (Map<?, ?>) value;
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static final class Source {
        private final String alias;
        private final String table;
        private final Set<String> columns;

        private Source(String alias, String table, Set<String> columns) {
            this.alias = alias;
            this.table = table;
            this.columns = columns;
        }

        private String sql(Dialect dialect) {
            // Oracle does not permit AS before table aliases.
            return table + " " + dialect.quote(alias);
        }

        private String column(Object value, Dialect dialect) {
            String column = identifier(value, "column");
            if (!columns.contains(column)) {
                throw invalid("Unknown column " + column + " for alias " + alias);
            }
            return dialect.quote(alias) + "." + dialect.quote(column);
        }

        private boolean hasColumn(String column) {
            return column != null && columns.contains(column);
        }
    }

    private static final class Dialect {
        private final String open;
        private final String close;

        private Dialect(String database) {
            if (Arrays.asList("MYSQL", "MARIADB", "DORIS", "CLICKHOUSE", "HIVE").contains(database)) {
                open = "`";
                close = "`";
            } else if (database.startsWith("SQLSERVER")) {
                open = "[";
                close = "]";
            } else {
                open = "\"";
                close = "\"";
            }
        }

        private String quote(String value) {
            return open + value + close;
        }

        private String table(String value) {
            List<String> parts = new ArrayList<>();
            for (String part : value.split("\\.", -1)) {
                parts.add(quote(identifier(part, "table_name")));
            }
            return String.join(".", parts);
        }
    }
}
