package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders a template only when its data bindings can be resolved without inventing values. */
final class UnifiedReportEvidence {
    private UnifiedReportEvidence() { }

    static JSONObject bind(String template, String goal, List<Object> evidence) {
        if (template == null || template.length() > 48_000 || evidence.isEmpty()) return null;
        try {
            JSONObject schema = JSON.parseObject(template).getJSONObject("dataSchema");
            if (schema == null) return null;
            JSONObject result = new JSONObject();
            result.put("title", goal);
            Set<String> required = new LinkedHashSet<>();
            JSONArray declared = schema.getJSONArray("required");
            if (declared != null) for (Object key : declared) required.add(String.valueOf(key));
            JSONArray fields = schema.getJSONArray("fields");
            boolean evidenceBound = false;
            if (fields != null) for (int i = 0; i < fields.size(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String key = field.getString("key");
                if (!safePath(key)) return null;
                if (Boolean.TRUE.equals(field.getBoolean("required"))) required.add(key);
                String source = field.getString("evidencePath");
                if (!safePath(source)) continue;
                Object value = lookup(Map.of("queries", evidence), source);
                if (value != null && accepts(field.getString("type"), value)) {
                    put(result, key, value);
                    evidenceBound = true;
                }
            }
            if (!evidenceBound) return null;
            for (String key : required) if (!safePath(key) || lookup(result, key) == null) return null;
            return result;
        } catch (RuntimeException invalidTemplate) {
            return null;
        }
    }

    static boolean satisfiesTemplate(String template, JSONObject data) {
        if (template == null || data == null || data.isEmpty()) return false;
        try {
            JSONObject schema = JSON.parseObject(template).getJSONObject("dataSchema");
            if (schema == null) return false;
            JSONArray required = schema.getJSONArray("required");
            if (required != null) for (Object item : required) {
                if (!(item instanceof String key) || !safePath(key) || lookup(data, key) == null) return false;
            }
            JSONArray fields = schema.getJSONArray("fields");
            if (fields != null) for (int i = 0; i < fields.size(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String key = field.getString("key");
                if (!safePath(key)) return false;
                Object value = lookup(data, key);
                if (value == null ? Boolean.TRUE.equals(field.getBoolean("required")) : !accepts(field.getString("type"), value)) return false;
            }
            return true;
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private static boolean safePath(String path) {
        if (path == null || path.length() > 160 || !path.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+){0,10}")) return false;
        for (String part : path.split("\\.")) {
            if (Set.of("__proto__", "prototype", "constructor").contains(part)) return false;
        }
        return true;
    }

    private static boolean accepts(String type, Object value) {
        if (type == null) return false;
        return switch (type) {
            case "array" -> value instanceof List<?>;
            case "object" -> value instanceof Map<?, ?>;
            case "string" -> value instanceof String;
            // Keep exact decimal strings intact for presentation.
            case "number", "integer" -> value instanceof Number || value instanceof String text
                    && text.matches("-?[0-9]+(\\.[0-9]+)?");
            case "boolean" -> value instanceof Boolean;
            default -> false;
        };
    }

    private static Object lookup(Object root, String path) {
        Object node = root;
        for (String part : path.split("\\.")) {
            if (node instanceof Map<?, ?> map) node = map.get(part);
            else if (node instanceof List<?> list && part.matches("[0-9]{1,4}")) {
                int index = Integer.parseInt(part);
                node = index < list.size() ? list.get(index) : null;
            } else return null;
        }
        return node;
    }

    private static void put(JSONObject root, String key, Object value) {
        String[] segments = key.split("\\.");
        JSONObject node = root;
        for (int i = 0; i < segments.length - 1; i++) {
            Object existing = node.computeIfAbsent(segments[i], ignored -> new JSONObject());
            if (!(existing instanceof JSONObject child)) throw new IllegalArgumentException("Conflicting report bindings");
            node = child;
        }
        node.put(segments[segments.length - 1], value);
    }
}
