package com.datamaster.common.utils.sql;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** JDBC 前后置 SQL 按分号分隔，保留字符串、标识符及注释中的分号。 */
public final class SqlStatementParser {

    private SqlStatementParser() {
    }

    public static List<String> parse(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) return result;
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) result.addAll(parse(item));
            return result;
        }
        if (raw.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(raw); i++) result.addAll(parse(Array.get(raw, i)));
            return result;
        }
        String sql = String.valueOf(raw);
        StringBuilder part = new StringBuilder();
        char quote = 0;
        String dollar = null;
        boolean lineComment = false;
        int blockDepth = 0;
        boolean hasSql = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;
            if (lineComment) {
                part.append(c);
                if (c == '\n' || c == '\r') lineComment = false;
            } else if (blockDepth > 0) {
                part.append(c);
                if (c == '/' && next == '*') {
                    part.append(next); i++; blockDepth++;
                } else if (c == '*' && next == '/') {
                    part.append(next); i++; blockDepth--;
                }
            } else if (dollar != null) {
                if (sql.startsWith(dollar, i)) {
                    part.append(dollar); i += dollar.length() - 1; dollar = null;
                } else part.append(c);
            } else if (quote != 0) {
                part.append(c);
                if (c == quote) {
                    if (next == quote) { part.append(next); i++; }
                    else quote = 0;
                } else if (c == '\\' && next != 0 && quote == '\'') {
                    part.append(next); i++;
                }
            } else if (c == '-' && next == '-') {
                part.append(c).append(next); i++; lineComment = true;
            } else if (c == '/' && next == '*') {
                part.append(c).append(next); i++; blockDepth = 1;
            } else if (c == '\'' || c == '"' || c == '`' || c == '[') {
                quote = c == '[' ? ']' : c; hasSql = true; part.append(c);
            } else if (c == '$' && dollarTag(sql, i) != null) {
                dollar = dollarTag(sql, i);
                part.append(dollar); i += dollar.length() - 1; hasSql = true;
            } else if (c == ';') {
                if (hasSql) result.add(part.toString().trim());
                part.setLength(0); hasSql = false;
            } else {
                part.append(c);
                if (!Character.isWhitespace(c)) hasSql = true;
            }
        }
        if (quote != 0 || dollar != null || blockDepth > 0) {
            throw new IllegalArgumentException("前后置 SQL 存在未闭合的引号或注释");
        }
        if (hasSql) result.add(part.toString().trim());
        return result;
    }

    private static String dollarTag(String sql, int start) {
        int end = sql.indexOf('$', start + 1);
        if (end < 0) return null;
        String tag = sql.substring(start + 1, end);
        if (!tag.isEmpty() && !tag.matches("[A-Za-z_][A-Za-z0-9_]*")) return null;
        return sql.substring(start, end + 1);
    }
}
