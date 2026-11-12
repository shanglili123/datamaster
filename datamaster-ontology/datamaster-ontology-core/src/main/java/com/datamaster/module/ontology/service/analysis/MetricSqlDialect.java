package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.metric.MetricRules;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Closed dialect set. No driver installation, arbitrary expressions or client-selected connections. */
public final class MetricSqlDialect {
    private final String type;
    private MetricSqlDialect(String type) { this.type = type; }
    public static MetricSqlDialect of(String type) {
        require(type != null, "指标来源类型不能为空");
        if ("DORIS".equalsIgnoreCase(type)) return new MetricSqlDialect("DORIS");
        if ("MYSQL".equalsIgnoreCase(type)) return new MetricSqlDialect("MYSQL");
        if ("POSTGRESQL".equalsIgnoreCase(type)) return new MetricSqlDialect("POSTGRESQL");
        throw new com.datamaster.common.exception.ServiceException("指标仅支持Doris、MySQL和PostgreSQL数据源");
    }
    public static MetricSqlDialect forSource(MetricRespVO snapshot) {
        require(snapshot != null && snapshot.getSourceSnapshot() != null, "指标来源快照不能为空");
        Object type = snapshot.getSourceSnapshot().get("datasourceType");
        return of(type == null ? null : String.valueOf(type));
    }
    public boolean sameType(String other) { return other != null && type.equalsIgnoreCase(other); }
    public boolean postgres() { return "POSTGRESQL".equals(type); }
    public String quote(String identifier) {
        require(identifier != null && identifier.matches("[A-Za-z_][A-Za-z0-9_]{0,127}"), "指标物理标识符无效");
        return postgres() ? "\"" + identifier + "\"" : "`" + identifier + "`";
    }
    public void validateSource(String database, String schema, String table, boolean legacyRestricted) {
        quote(database); quote(table);
        if (schema != null && !schema.isBlank()) quote(schema);
        if (legacyRestricted) require("DORIS".equals(type) && MetricRules.adsSource(database, table), "历史指标快照仅支持已批准的Doris ADS来源");
        if (!postgres()) require(schema == null || schema.isBlank() || schema.equals(database), "该数据源schema必须为空或等于数据库名");
        else require(schema != null && !schema.isBlank(), "PostgreSQL指标必须明确schema，禁止依赖可变search_path");
    }
    public String table(String database, String schema, String table) {
        return quote(postgres() ? schema : database) + "." + quote(table);
    }
    public String limitClause() { return " LIMIT ?"; }
    public String shiftMinutes(String column, int minutes) {
        require(Math.abs((long) minutes) <= 28 * 60, "时间偏移超限");
        if (minutes == 0) return column;
        return postgres() ? "(" + column + " + INTERVAL '" + minutes + " minutes')"
                : "DATE_ADD(" + column + ", INTERVAL " + minutes + " MINUTE)";
    }
    public String bucket(String column, String grain) {
        String format;
        if ("DAY".equals(grain)) format = postgres() ? "YYYY-MM-DD" : "%Y-%m-%d";
        else if ("MONTH".equals(grain)) format = postgres() ? "YYYY-MM" : "%Y-%m";
        else if ("YEAR".equals(grain)) format = postgres() ? "YYYY" : "%Y";
        else throw new com.datamaster.common.exception.ServiceException("不支持的时间分桶");
        return (postgres() ? "TO_CHAR(" : "DATE_FORMAT(") + column + ", '" + format + "')";
    }
}
