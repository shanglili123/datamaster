package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Opaque, length-delimited identities independent of any graph storage backend. */
@Component
public class LineageIdentity {
    public String objectId(LineageEndpointDTO endpoint) {
        return "obj:" + hash(canonical(endpoint));
    }

    public String columnId(LineageEndpointDTO endpoint, LineageColumnDTO column) {
        if (column == null || blank(column.getName())) throw new IllegalArgumentException("INVALID_COLUMN");
        return "col:" + hash(canonical(endpoint) + part(normalize(column.getName(), column.getQuoted(), endpoint.getCaseRule())));
    }

    public String taskId(Long taskId) {
        if (taskId == null || taskId <= 0) throw new IllegalArgumentException("INVALID_TASK");
        return "task:" + taskId;
    }

    public String routeId(Long taskId, String unit, String route) {
        return "task:" + taskId + ":" + hash(part(unit) + part(route));
    }

    public String relationId(Long taskId, LineageRelationDTO relation) {
        String source = relation.getSourceColumn() == null ? objectId(relation.getSource()) : columnId(relation.getSource(), relation.getSourceColumn());
        String target = relation.getTargetColumn() == null ? objectId(relation.getTarget()) : columnId(relation.getTarget(), relation.getTargetColumn());
        return "edge:" + hash(part(String.valueOf(taskId)) + part(relation.getUnitKey()) + part(relation.getRouteKey())
                + part(source) + part(target) + part(relation.getKind()));
    }

    public static String canonical(LineageEndpointDTO endpoint) {
        if (endpoint == null || endpoint.getDatasourceId() == null || endpoint.getDatasourceId() <= 0) {
            throw new IllegalArgumentException("MISSING_DATASOURCE");
        }
        String kind = blank(endpoint.getKind()) ? "TABLE" : endpoint.getKind().toUpperCase(Locale.ROOT);
        if (!"TABLE".equals(kind) && !"FILE".equals(kind) && !"STREAM".equals(kind) && !"EXTERNAL".equals(kind)) {
            throw new IllegalArgumentException("INVALID_OBJECT_KIND");
        }
        if ("TABLE".equals(kind) && blank(endpoint.getTable())) throw new IllegalArgumentException("MISSING_TABLE");
        if (!"TABLE".equals(kind) && blank(endpoint.getExternalKey())) throw new IllegalArgumentException("MISSING_EXTERNAL_KEY");
        return part(String.valueOf(endpoint.getDatasourceId())) + part(kind)
                + part(normalize(endpoint.getCatalog(), endpoint.getCatalogQuoted(), endpoint.getCaseRule()))
                + part(normalize(endpoint.getSchema(), endpoint.getSchemaQuoted(), endpoint.getCaseRule()))
                + part("TABLE".equals(kind) ? normalize(endpoint.getTable(), endpoint.getTableQuoted(), endpoint.getCaseRule()) : endpoint.getExternalKey());
    }

    public static String normalize(String value, Boolean quoted, String caseRule) {
        if (value == null || value.isEmpty()) return null;
        if (Boolean.TRUE.equals(quoted)) return value;
        if ("LOWER".equalsIgnoreCase(caseRule)) return value.toLowerCase(Locale.ROOT);
        if ("UPPER".equalsIgnoreCase(caseRule)) return value.toUpperCase(Locale.ROOT);
        return value;
    }

    public static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : bytes) result.append(String.format(Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256_UNAVAILABLE", e);
        }
    }

    private static String part(String value) { return value == null ? "-1:" : value.length() + ":" + value; }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
