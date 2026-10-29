package com.datamaster.module.collector.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Pattern;

/** Builds a single [start, end] predicate, never a WHERE clause or a SQL expression. */
public final class StageIncrementalSql {

    private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_$]*");
    private static final String NAME = "[\\p{L}_][\\p{L}\\p{N}_$]*";
    private static final Pattern QUOTED_COLUMN = Pattern.compile(
            "(?:\"" + NAME + "\"(?:\\.\"" + NAME + "\")?"
                    + "|`" + NAME + "`(?:\\.`" + NAME + "`)?"
                    + "|\\[" + NAME + "\\](?:\\.\\[" + NAME + "\\])?)");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = new DateTimeFormatterBuilder()
            .append(DATE).appendLiteral(' ').appendPattern("HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true).optionalEnd()
            .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter CANONICAL_TIME = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter(Locale.ROOT);

    private StageIncrementalSql() {
    }

    /**
     * Compatibility entry point: only an already quoted column or alias.column is accepted.
     * Uses canonical quoted time values; prefer the dialect-aware overload for Oracle/DM.
     */
    public static String predicate(String qualifiedColumn, String cursorType, String start, String end) {
        if (qualifiedColumn == null || !QUOTED_COLUMN.matcher(qualifiedColumn).matches()) {
            throw new IllegalArgumentException("Expected a quoted column or quoted alias.column");
        }
        return build(qualifiedColumn, cursorType, start, end, null);
    }

    /**
     * Quotes raw, single-part identifiers without changing their case. A null alias means
     * an unqualified column. Unknown database types, expressions and prequoted names fail closed.
     */
    public static String predicate(String column, String cursorType, String start, String end,
                                   String dbType, String alias) {
        Dialect dialect = dialect(dbType);
        String qualifiedColumn = quote(column, dialect);
        if (alias != null) {
            qualifiedColumn = quote(alias, dialect) + "." + qualifiedColumn;
        }
        return build(qualifiedColumn, cursorType, start, end, dialect);
    }

    public static String quotedColumn(String column, String dbType) {
        return quote(column, dialect(dbType));
    }

    public static String quotedTable(String table, String dbType) {
        Dialect dialect = dialect(dbType);
        if (table == null) throw new IllegalArgumentException("Table is required");
        StringBuilder qualified = new StringBuilder();
        for (String part : table.split("\\.", -1)) {
            if (qualified.length() > 0) qualified.append('.');
            qualified.append(quote(part, dialect));
        }
        return qualified.toString();
    }

    /** Uses the same physical identifier quoting for MAX and runtime window predicates. */
    public static String maxQuery(String table, String column, String dbType) {
        return "SELECT MAX(" + quotedColumn(column, dbType) + ") AS MAX_VALUE FROM " + quotedTable(table, dbType);
    }

    private static String build(String column, String cursorType, String start, String end,
                                Dialect dialect) {
        CursorType type;
        try {
            type = CursorType.valueOf(cursorType == null ? "" : cursorType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cursor type must be TIME or ID", ex);
        }
        // Validate every supplied boundary, even if an absent end closes the window.
        String startLiteral = start == null ? null : literal(start, type, dialect);
        String endLiteral = end == null ? null : literal(end, type, dialect);
        if (endLiteral == null) {
            return "1=0";
        }
        if (startLiteral == null) {
            return "(" + column + " <= " + endLiteral + ")";
        }
        return "(" + column + " >= " + startLiteral + " AND " + column + " <= " + endLiteral + ")";
    }

    /** 成功水位只能前进；空源或源MAX回退都不能覆盖已有成功边界。 */
    public static int compare(String left, String right, String cursorType) {
        if (left == null || right == null) throw new IllegalArgumentException("Cursor boundaries are required");
        if ("ID".equalsIgnoreCase(cursorType)) return new BigDecimal(left).compareTo(new BigDecimal(right));
        if (!"TIME".equalsIgnoreCase(cursorType)) throw new IllegalArgumentException("Cursor type must be TIME or ID");
        LocalDateTime a = left.length() == 10 ? LocalDate.parse(left, DATE).atStartOfDay()
                : LocalDateTime.parse(left.replace('T', ' '), TIME);
        LocalDateTime b = right.length() == 10 ? LocalDate.parse(right, DATE).atStartOfDay()
                : LocalDateTime.parse(right.replace('T', ' '), TIME);
        return a.compareTo(b);
    }

    private static String literal(String value, CursorType type, Dialect dialect) {
        if (type == CursorType.ID) {
            try {
                // BigDecimal rejects SQL tokens, NaN and Infinity; plain output has no exponent.
                return new BigDecimal(value).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid ID cursor", ex);
            }
        }
        LocalDateTime timestamp;
        try {
            if (value.length() == 10) {
                timestamp = LocalDate.parse(value, DATE).atStartOfDay();
            } else {
                timestamp = LocalDateTime.parse(value.replace('T', ' '), TIME);
            }
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid TIME cursor", ex);
        }
        // Keep a four-digit SQL year, including after parsing ISO dates.
        if (timestamp.getYear() < 1 || timestamp.getYear() > 9999) {
            throw new IllegalArgumentException("TIME cursor year must be between 0001 and 9999");
        }
        String canonical = CANONICAL_TIME.format(timestamp);
        if (dialect == Dialect.ORACLE) {
            // Always include the fractional part to match the explicit conversion mask.
            String oracleTime = canonical.indexOf('.') < 0 ? canonical + ".0" : canonical;
            return "TO_TIMESTAMP('" + oracleTime + "', 'YYYY-MM-DD HH24:MI:SS.FF9')";
        }
        if (dialect == Dialect.SQLSERVER) {
            return "CAST('" + canonical.replace(' ', 'T') + "' AS datetime2)";
        }
        if (dialect == Dialect.POSTGRES) {
            return "TIMESTAMP '" + canonical + "'";
        }
        return "'" + canonical + "'";
    }

    private static String quote(String name, Dialect dialect) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Expected a single SQL identifier");
        }
        if (dialect == Dialect.MYSQL) {
            return "`" + name + "`";
        }
        if (dialect == Dialect.SQLSERVER) {
            return "[" + name + "]";
        }
        return "\"" + name + "\"";
    }

    private static Dialect dialect(String dbType) {
        if (dbType == null) {
            throw new IllegalArgumentException("Database type is required");
        }
        switch (dbType.toUpperCase(Locale.ROOT)) {
            case "MYSQL":
            case "MARIADB":
            case "2":
            case "DORIS":
            case "CLICKHOUSE":
            case "HIVE":
                return Dialect.MYSQL;
            case "SQLSERVER":
            case "SQL_SERVER":
            case "SQL_SERVER2008":
            case "SQLSERVER2008":
            case "MSSQL":
                return Dialect.SQLSERVER;
            case "POSTGRESQL":
            case "POSTGRES":
            case "KINGBASE":
            case "KINGBASE8":
                return Dialect.POSTGRES;
            case "ORACLE":
            case "ORACLE11":
            case "DM":
            case "DM8":
                return Dialect.ORACLE;
            case "DB2":
            case "H2":
                return Dialect.POSTGRES;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    private enum CursorType { TIME, ID }

    private enum Dialect { MYSQL, POSTGRES, SQLSERVER, ORACLE }
}
