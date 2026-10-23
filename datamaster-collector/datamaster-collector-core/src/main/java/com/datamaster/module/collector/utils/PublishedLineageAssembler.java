package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import java.util.*;

/** Captures only actual executable routes, not graph scheduling links or connection credentials. */
public final class PublishedLineageAssembler {
    private PublishedLineageAssembler() { }

    public static LineageSnapshotDTO snapshot(CollectorEtlTaskDO task, Map<String, Object> args) {
        LineageSnapshotDTO snapshot = new LineageSnapshotDTO();
        snapshot.setTaskId(task.getId());
        snapshot.setTaskName(task.getName());
        snapshot.setTaskType(task.getType());
        snapshot.setTaskCode(task.getCode());
        snapshot.setSpaceId(task.getSpaceId());
        snapshot.setSpaceCode(task.getSpaceCode());
        snapshot.setVersion(task.getVersion() == null ? null : String.valueOf(task.getVersion()));
        snapshot.setEventType("UPSERT");
        snapshot.setCaptureStatus("COMPLETE");
        List<LineageUnitDTO> units = new ArrayList<>();
        snapshot.setUnits(units);
        List<Map<String, Object>> published = list(args.get("lineageUnits"));
        boolean complete = true;
        try {
            if (published.isEmpty()) {
                complete = capture(units, args, "main");
            } else {
                for (Map<String, Object> unit : published) {
                    complete &= capture(units, map(unit.get("args")), text(unit.get("unitKey")));
                }
            }
        } catch (RuntimeException captureFailure) {
            complete = false;
        }
        if (!complete) {
            snapshot.setCaptureStatus("PARTIAL");
            snapshot.setErrorCode("UNSUPPORTED_PUBLISHED_ROUTE");
        }
        if (units.isEmpty()) {
            snapshot.setCaptureStatus("FAILED");
            snapshot.setErrorCode("NO_PROVABLE_PUBLISHED_ROUTE");
        }
        if (list(args.get("readerList")).size() > 1) {
            snapshot.setCaptureStatus(units.isEmpty() ? "FAILED" : "PARTIAL");
            snapshot.setErrorCode("UNEXECUTED_READERS_NOT_INFERRED");
        }
        return snapshot;
    }

    private static boolean capture(List<LineageUnitDTO> units, Map<String, Object> args, String unitKey) {
        Map<String, Object> reader = map(args.get("reader"));
        Map<String, Object> readerParam = map(reader.get("parameter"));
        List<Map<String, Object>> writers = list(args.get("writerList"));
        if (writers.isEmpty() && args.get("writer") instanceof Map) writers = Collections.singletonList(map(args.get("writer")));
        String actualJson = text(args.get("publishedJobJson"));
        Map<String, Object> job = actualJson == null ? Collections.emptyMap() : map(JSON.parseObject(actualJson).get("job"));
        List<Map<String, Object>> contents = list(job.get("content"));
        // An executable job is required; never reinterpret an editable SQL draft as a published statement.
        if (contents.size() != writers.size() || readerParam.isEmpty()) return false;
        boolean complete = list(args.get("readerList")).size() <= 1;
        LineageEndpointDTO source = endpoint(readerParam, true);
        String readerSql = text(map(readerParam.get("connection")).get("querySql"));
        LineageEndpointDTO context = endpoint(readerParam, false);
        for (int i = 0; i < writers.size(); i++) {
            Map<String, Object> writerParam = map(writers.get(i).get("parameter"));
            LineageEndpointDTO target = endpoint(writerParam, true);
            if (target == null || (source == null && (context == null || readerSql == null))) {
                complete = false;
                continue;
            }
            LineageUnitDTO route = new LineageUnitDTO();
            route.setUnitKey(unitKey == null ? "main" : unitKey);
            route.setRouteKey("output:" + i);
            route.setContext(context);
            route.setSources(source == null ? Collections.emptyList() : Collections.singletonList(source));
            route.setTarget(target);
            route.setReaderSql(readerSql);
            route.setTransformSql(text(map(contents.get(i).get("transformer")).get("transformSql")));
            List<LineageColumnDTO> inputs = columns(readerParam.get("column"));
            // The generated job's writer columns are the actual output order, not UI mapping labels.
            List<LineageColumnDTO> targets = columns(map(map(contents.get(i).get("writer")).get("parameter")).get("column"));
            route.setSourceColumns(inputs);
            route.setTargetColumns(targets);
            // Reliable only for explicit physical columns with no transformation or query result reordering.
            boolean direct = source != null && readerSql == null && route.getTransformSql() == null
                    && !inputs.isEmpty() && inputs.size() == targets.size();
            // Explicit generated writer columns preserve actual write order; SQL output order is proven by AST.
            route.setColumnOrderReliable(!targets.isEmpty() && (readerSql != null || !inputs.isEmpty()));
            if (direct) {
                List<LineageMappingDTO> mappings = new ArrayList<>();
                for (int c = 0; c < inputs.size(); c++) {
                    LineageMappingDTO mapping = new LineageMappingDTO();
                    mapping.setSource(source);
                    mapping.setSourceColumn(inputs.get(c));
                    mapping.setTargetColumn(targets.get(c));
                    mappings.add(mapping);
                }
                route.setMappings(mappings);
            }
            units.add(route);
        }
        return complete;
    }

    private static LineageEndpointDTO endpoint(Map<String, Object> param, boolean requireTable) {
        Object rawId = param.get("datasourceId");
        if (rawId == null) return null;
        Long id;
        try { id = Long.valueOf(String.valueOf(rawId)); } catch (NumberFormatException failure) { return null; }
        Map<String, Object> connection = map(param.get("connection"));
        String table = text(connection.get("table"));
        if (requireTable && table == null) return null;
        LineageEndpointDTO endpoint = new LineageEndpointDTO();
        endpoint.setDatasourceId(id);
        endpoint.setKind("TABLE");
        endpoint.setCatalog(text(param.get("dbName")));
        endpoint.setSchema(text(param.get("sid")));
        endpoint.setTable(table);
        String type = String.valueOf(param.get("dbType"));
        if (table != null && !qualifiedName(endpoint, table, type)) return null;
        endpoint.setCaseRule(Arrays.asList("PostgreSQL", "Kingbase8").contains(type) ? "LOWER"
                : Arrays.asList("Oracle", "Oracle11", "DM8", "DB2", "OSCAR").contains(type) ? "UPPER" : "EXACT");
        return endpoint;
    }

    /** Parse identifier segments with quote awareness; a dot inside quotes is not a qualifier. */
    private static boolean qualifiedName(LineageEndpointDTO endpoint, String raw, String datasourceType) {
        List<String> parts = new ArrayList<>();
        List<Boolean> quoted = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        char end = 0;
        boolean isQuoted = false;
        boolean closed = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (end != 0) {
                if (c == end) {
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == end) { value.append(c); i++; }
                    else { end = 0; closed = true; }
                } else value.append(c);
            } else if (c == '.') {
                if (value.length() == 0) return false;
                parts.add(value.toString()); quoted.add(isQuoted);
                value.setLength(0); isQuoted = false; closed = false;
            } else if (c == '`' || c == '"' || c == '[') {
                if (value.length() != 0 || closed) return false;
                isQuoted = true; end = c == '[' ? ']' : c;
            } else {
                if (closed || Character.isWhitespace(c) || !(Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#')) return false;
                value.append(c);
            }
        }
        if (end != 0 || value.length() == 0) return false;
        parts.add(value.toString()); quoted.add(isQuoted);
        if (parts.size() > 3) return false;
        int last = parts.size() - 1;
        endpoint.setTable(parts.get(last)); endpoint.setTableQuoted(quoted.get(last));
        if (parts.size() == 3) {
            endpoint.setCatalog(parts.get(0)); endpoint.setCatalogQuoted(quoted.get(0));
            endpoint.setSchema(parts.get(1)); endpoint.setSchemaQuoted(quoted.get(1));
        } else if (parts.size() == 2) {
            if (Arrays.asList("MySql", "Doris", "Hive", "ClickHouse").contains(datasourceType)) {
                endpoint.setCatalog(parts.get(0)); endpoint.setCatalogQuoted(quoted.get(0));
            } else {
                endpoint.setSchema(parts.get(0)); endpoint.setSchemaQuoted(quoted.get(0));
            }
        }
        return true;
    }

    private static List<LineageColumnDTO> columns(Object raw) {
        List<LineageColumnDTO> columns = new ArrayList<>();
        if (!(raw instanceof List)) return columns;
        for (Object item : (List<?>) raw) {
            String name;
            Boolean quoted = null;
            if (item instanceof Map) {
                Map<?, ?> field = (Map<?, ?>) item;
                name = text(field.get("name"));
                if (name == null) name = text(field.get("columnName"));
                if (field.get("quoted") instanceof Boolean) quoted = (Boolean) field.get("quoted");
            } else name = text(item);
            if (name == null || "*".equals(name)) return Collections.emptyList();
            LineageEndpointDTO parsed = new LineageEndpointDTO();
            if (!qualifiedName(parsed, name, "")) return Collections.emptyList();
            // These are actual reader/writer column names, not raw SQL expressions.
            if (parsed.getCatalog() != null || parsed.getSchema() != null) return Collections.emptyList();
            LineageColumnDTO column = new LineageColumnDTO();
            column.setName(parsed.getTable());
            column.setQuoted(quoted != null ? quoted : true);
            columns.add(column);
        }
        return columns;
    }
    private static String text(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
    @SuppressWarnings("unchecked") private static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }
    @SuppressWarnings("unchecked") private static List<Map<String, Object>> list(Object value) {
        if (!(value instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) if (item instanceof Map) result.add((Map<String, Object>) item);
        return result;
    }
}
