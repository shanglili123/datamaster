package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Resolves saved writer snapshots to credential-free physical table identities.
 * No datasource lookup is performed: incomplete or unsupported writers fail closed.
 * Keys are canonical, length-prefixed strings; callers may SHA-256 them for lock names.
 */
public final class EtlWriteResourceResolver {
    private EtlWriteResourceResolver() {
    }

    public static SortedSet<String> resolve(String taskDefinitionList) {
        SortedSet<String> resources = new TreeSet<>();
        try {
            if (blank(taskDefinitionList)) {
                throw invalid();
            }
            JSONArray nodes = JSON.parseArray(taskDefinitionList);
            if (nodes == null) {
                throw invalid();
            }
            for (Object raw : nodes) {
                JSONObject node = object(raw);
                String type = required(text(node.get("componentType")));
                JSONObject params = optionalObject(node.get("taskParams"));
                if ("91".equals(type)) {
                    if (isDestructive(params)) resources.add(tableIdentity(params));
                } else if ("37".equals(type)) {
                    Object targets = params.get("targetTables");
                    if (!(targets instanceof List) || ((List<?>) targets).isEmpty()) {
                        throw invalid();
                    }
                    for (Object target : (List<?>) targets) {
                        JSONObject targetParams = object(target);
                        if (isDestructive(targetParams)) resources.add(tableIdentity(targetParams));
                    }
                } else if (params.containsKey("logicNode")) {
                    // 纯逻辑节点不直接写入，目标由独立表输出负责。
                    continue;
                } else if ("100".equals(type) || params.containsKey("pipelineNode")) {
                    JSONObject pipeline = object(params.get("pipelineNode"));
                    Object output = pipeline.get("output");
                    if (output != null) {
                        JSONObject outputParams = object(output);
                        if (isDestructive(outputParams)) resources.add(tableIdentity(outputParams));
                    } else if (!pipeline.containsKey("logicNode") && !params.containsKey("logicNode")
                            && !java.util.Arrays.asList("CONDITION", "REFERENCE", "SPLIT", "BROADCAST", "MERGE")
                            .contains(text(pipeline.get("mode")).toUpperCase(Locale.ROOT))) {
                        throw invalid();
                    }
                } else if ("2".equals(text(params.get("type"))) || "93".equals(type)
                        || params.containsKey("writerDatasource") || params.containsKey("target_table_name")
                        || params.containsKey("targetTables") || params.containsKey("target_asset_id")
                        || params.containsKey("writer") || params.containsKey("sink")
                        || type.toUpperCase(Locale.ROOT).contains("WRITER")
                        || type.toUpperCase(Locale.ROOT).contains("OUTPUT")
                        || text(node.get("taskType")).toUpperCase(Locale.ROOT).contains("WRITER")
                        || text(node.get("taskType")).toUpperCase(Locale.ROOT).contains("OUTPUT")) {
                    throw unsupported();
                }
            }
            return resources;
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Parser exceptions can include the source (and thus credentials). Never propagate them.
            throw invalid();
        }
    }

    /** 仅破坏性写入（全量清表或显式清表前置 SQL）需要目标独占；追加/Upsert 可并行。 */
    private static boolean isDestructive(JSONObject writerParams) {
        if ("1".equals(text(writerParams.get("writeModeType")))) {
            return true;
        }
        String preSql = String.valueOf(writerParams.get("preSql")).toUpperCase(Locale.ROOT);
        return preSql.contains("TRUNCATE") || preSql.contains("DELETE");
    }

    private static String tableIdentity(JSONObject output) {
        JSONObject ds = object(output.get("writerDatasource"));
        String type = databaseType(text(ds.get("datasourceType")));
        JSONObject config = optionalObject(ds.get("datasourceConfig"));
        String host = first(ds, "ip", "host");
        if (blank(host)) {
            host = first(config, "host", "ip");
        }
        host = required(host).toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        // A host must not be a URL, user-info, or connection string.
        if (!host.matches("[a-z0-9_.:%-]+")) {
            throw invalid();
        }
        String port = first(ds, "port");
        if (blank(port)) {
            port = first(config, "port");
        }
        if (blank(port)) {
            port = defaultPort(type);
        }
        if (!port.matches("[0-9]+")) {
            throw invalid();
        }
        int numericPort = Integer.parseInt(port);
        if (numericPort < 1 || numericPort > 65535) {
            throw invalid();
        }
        String database = "oracle".equals(type)
                ? first(config, "sid", "dbname", "databaseName", "database")
                : first(config, "dbname", "databaseName", "database", "sid");
        if (blank(database)) {
            database = "oracle".equals(type)
                    ? first(ds, "sid", "dbname", "databaseName", "database")
                    : first(ds, "dbname", "databaseName", "database", "sid");
        }
        database = required(database);
        // Database names denote connection targets, not SQL fragments. Do not expose URLs.
        if (!database.matches("[\\p{L}\\p{N}_.$-]+")) {
            throw invalid();
        }
        if (!"postgresql".equals(type)) {
            database = database.toLowerCase(Locale.ROOT);
        }
        String table = first(output, "target_table_name", "target_asset_id", "table");
        List<String> parts = identifiers(required(table), type);
        if (parts.size() > 2) {
            // Cross-database names need a separately modelled connection identity.
            throw invalid();
        }
        String schema = "";
        String name = parts.get(parts.size() - 1);
        if ("mysql".equals(type)) {
            if (parts.size() == 2) {
                database = parts.get(0);
            }
        } else if (parts.size() == 2) {
            schema = parts.get(0);
        } else {
            String configuredSchema = first(config, "schema", "schemaName", "currentSchema");
            if (blank(configuredSchema)) {
                configuredSchema = first(ds, "schema", "schemaName", "currentSchema");
            }
            if (!blank(configuredSchema)) {
                List<String> schemas = identifiers(configuredSchema, type);
                if (schemas.size() != 1) {
                    throw invalid();
                }
                schema = schemas.get(0);
            } else if ("postgresql".equals(type) || "kingbase".equals(type)) {
                schema = "public";
            } else {
                // Oracle/DM/SQL Server defaults can depend on the user. Username is not an identity.
                throw invalid();
            }
        }
        return "etl-table-v1|" + field(type) + field(host) + field(String.valueOf(numericPort))
                + field(database) + field(schema) + field(name);
    }

    private static List<String> identifiers(String input, String type) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                i++;
            }
            if (i == input.length()) {
                throw invalid();
            }
            char first = input.charAt(i);
            boolean quoted = first == '"' || first == '`' || first == '[';
            StringBuilder part = new StringBuilder();
            if (quoted) {
                if ((first == '`' && !"mysql".equals(type))
                        || (first == '[' && !"sqlserver".equals(type))) {
                    throw invalid();
                }
                char end = first == '[' ? ']' : first;
                i++;
                boolean closed = false;
                while (i < input.length()) {
                    char c = input.charAt(i++);
                    if (c == end) {
                        if (i < input.length() && input.charAt(i) == end) {
                            part.append(end);
                            i++;
                        } else {
                            closed = true;
                            break;
                        }
                    } else {
                        part.append(c);
                    }
                }
                if (!closed) {
                    throw invalid();
                }
            } else {
                while (i < input.length() && input.charAt(i) != '.'
                        && !Character.isWhitespace(input.charAt(i))) {
                    part.append(input.charAt(i++));
                }
                if (!part.toString().matches("[\\p{L}_][\\p{L}\\p{N}_$#]*")) {
                    throw invalid();
                }
            }
            if (part.length() == 0 || part.toString().matches("(?s).*\\p{Cntrl}.*")) {
                throw invalid();
            }
            String value = part.toString();
            if ("mysql".equals(type) || "sqlserver".equals(type)) {
                value = value.toLowerCase(Locale.ROOT);
            } else if (!quoted) {
                value = ("oracle".equals(type) || "dm".equals(type))
                        ? value.toUpperCase(Locale.ROOT) : value.toLowerCase(Locale.ROOT);
            }
            result.add(value);
            while (i < input.length() && Character.isWhitespace(input.charAt(i))) {
                i++;
            }
            if (i < input.length()) {
                if (input.charAt(i++) != '.' || i == input.length()) {
                    throw invalid();
                }
            }
        }
        if (result.isEmpty()) {
            throw invalid();
        }
        return result;
    }

    private static String databaseType(String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "mysql": return "mysql";
            case "postgresql": return "postgresql";
            case "oracle":
            case "oracle11": return "oracle";
            case "dm8": return "dm";
            case "kingbase8": return "kingbase";
            case "sql_server":
            case "sql_server2008": return "sqlserver";
            default: throw unsupported();
        }
    }

    private static String defaultPort(String type) {
        switch (type) {
            case "mysql": return "3306";
            case "postgresql": return "5432";
            case "oracle": return "1521";
            case "dm": return "5236";
            case "kingbase": return "54321";
            case "sqlserver": return "1433";
            default: throw unsupported();
        }
    }

    private static JSONObject object(Object value) {
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof java.util.Map) {
            return new JSONObject((java.util.Map<String, Object>) value);
        }
        if (value instanceof String && !blank((String) value)) {
            JSONObject parsed = JSON.parseObject((String) value);
            if (parsed != null) {
                return parsed;
            }
        }
        throw invalid();
    }

    private static JSONObject optionalObject(Object value) {
        return value == null ? new JSONObject() : object(value);
    }

    private static String first(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = text(object.get(key));
            if (!blank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String field(String value) {
        return value.length() + ":" + value + "|";
    }

    private static String required(String value) {
        if (blank(value)) {
            throw invalid();
        }
        return value;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static ServiceException invalid() {
        return new ServiceException("写资源缺少有效的物理数据源、数据库、schema或表身份，无法启用并发保护");
    }

    private static ServiceException unsupported() {
        return new ServiceException("并发保护目前仅支持关系表输出，不支持该输出类型");
    }
}
