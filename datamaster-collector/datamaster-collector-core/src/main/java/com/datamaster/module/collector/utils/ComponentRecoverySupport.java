package com.datamaster.module.collector.utils;

import java.util.Locale;
import java.util.regex.Pattern;

/** Deterministic recovery boundary; model output can never override a hard rejection. */
public final class ComponentRecoverySupport {
    private static final Pattern SECRET = Pattern.compile(
            "(?i)((?:password|passwd|authorization|api[-_]?key|X-DataMaster-Task-Token)[\\\"']?\\s*[:=]\\s*[\\\"']?)([^\\s\\\"',;}]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~-]+");
    private ComponentRecoverySupport() { }

    public static long delayMillis(int attempt) {
        return (attempt <= 1 ? 60L : attempt == 2 ? 180L : 600L) * 1000L;
    }

    public static String sanitize(String text) {
        if (text == null) return "";
        return SECRET.matcher(BEARER.matcher(text).replaceAll("Bearer [REDACTED]")).replaceAll("$1[REDACTED]");
    }

    public static boolean transientFailure(String log) {
        String text = log == null ? "" : log.toLowerCase(Locale.ROOT);
        if (contains(text, "outofmemory", "java heap space", "cannot allocate memory", "no space left",
                "permission denied", "access denied", "authentication failed", "not authorized", "syntax error",
                "sqlsyntax", "parseexception", "analysisexception", "unknown column", "does not exist",
                "type mismatch", "invalid column", "字段不存在", "表不存在", "权限不足")) return false;
        return contains(text, "connection refused", "connect timed out", "connection timed out", "read timed out",
                "connection reset", "communications link failure", "sockettimeoutexception", "temporarily unavailable");
    }

    private static boolean contains(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }
}
