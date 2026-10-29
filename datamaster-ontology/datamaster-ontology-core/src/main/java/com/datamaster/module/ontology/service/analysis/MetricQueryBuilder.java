package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import com.datamaster.module.ontology.service.metric.MetricRules;
import lombok.Getter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** No IO, SQL expressions, client aliases, interpolation of values, or current binding lookups. */
public final class MetricQueryBuilder {
    private static final Set<String> AGGREGATIONS = new HashSet<>(Arrays.asList("SUM", "COUNT", "COUNT_DISTINCT", "AVG", "MIN", "MAX"));
    private static final Set<String> INTEGER_TYPES = new HashSet<>(Arrays.asList("integer", "long", "int", "bigint", "smallint"));
    private static final Map<String, String> OPERATORS = new HashMap<>();
    static {
        OPERATORS.put("EQ", "="); OPERATORS.put("NE", "<>"); OPERATORS.put("GT", ">");
        OPERATORS.put("GE", ">="); OPERATORS.put("LT", "<"); OPERATORS.put("LE", "<=");
    }
    private MetricQueryBuilder() { }

    public static MetricQueryReqVO normalize(MetricQueryReqVO input) {
        require(input != null && input.getMetricId() != null && input.getMetricId() > 0, "指标ID不能为空");
        require(input.getVersion() != null && input.getVersion() > 0, "必须指定大于0的发布版本");
        MetricQueryReqVO copy = new MetricQueryReqVO();
        copy.setMetricId(input.getMetricId()); copy.setVersion(input.getVersion());
        List<Long> dimensions = input.getDimensionPropertyIds() == null ? Collections.emptyList() : input.getDimensionPropertyIds();
        require(dimensions.size() <= 5 && !dimensions.contains(null), "查询维度最多5个且不能为空");
        require(new HashSet<>(dimensions).size() == dimensions.size(), "查询维度不能重复");
        copy.setDimensionPropertyIds(new ArrayList<>(dimensions));
        List<MetricFilterVO> filters = input.getFilters() == null ? Collections.emptyList() : input.getFilters();
        require(filters.size() <= 20, "过滤条件最多20个");
        List<MetricFilterVO> copied = new ArrayList<>();
        for (MetricFilterVO filter : filters) {
            require(filter != null && filter.getPropertyId() != null, "过滤属性不能为空");
            MetricFilterVO value = new MetricFilterVO();
            value.setPropertyId(filter.getPropertyId()); value.setOperator(filter.getOperator());
            if (filter.getValue() instanceof Collection) {
                require(((Collection<?>) filter.getValue()).size() <= 100, "IN最多100项");
                value.setValue(new ArrayList<>((Collection<?>) filter.getValue()));
            } else value.setValue(filter.getValue());
            copied.add(value);
        }
        copy.setFilters(copied);
        copy.setStartTime(input.getStartTime()); copy.setEndTime(input.getEndTime());
        copy.setLimit(input.getLimit() == null ? 100 : input.getLimit());
        require(copy.getLimit() >= 1 && copy.getLimit() <= 500, "limit必须在1到500之间");
        require((copy.getStartTime() == null) == (copy.getEndTime() == null), "开始和结束时间必须同时提供");
        return copy;
    }

    public static void checkIdentity(MetricDO current, MetricRespVO snapshot, Long metricId, Integer version) {
        require(current != null && snapshot != null && Objects.equals(current.getId(), metricId)
                && Objects.equals(snapshot.getId(), metricId)
                && Objects.equals(snapshot.getOntologyId(), current.getOntologyId())
                && Objects.equals(snapshot.getConceptId(), current.getConceptId())
                && Objects.equals(snapshot.getCode(), current.getCode())
                && Objects.equals(snapshot.getPublishedVersion(), version)
                && "PUBLISHED".equals(snapshot.getStatus()), "指标发布快照身份不一致");
    }

    public static Plan compile(MetricRespVO snapshot, MetricQueryReqVO input) {
        MetricQueryReqVO req = normalize(input);
        require(snapshot != null && Objects.equals(snapshot.getId(), req.getMetricId())
                && Objects.equals(snapshot.getPublishedVersion(), req.getVersion())
                && "PUBLISHED".equals(snapshot.getStatus()), "指标发布版本不一致");
        require(snapshot.getOntologyId() != null && snapshot.getConceptId() != null && snapshot.getConceptTableId() != null,
                "发布快照缺少归属信息");
        require(MetricRules.notBlank(snapshot.getUnit()) && MetricRules.notBlank(snapshot.getGrain()), "发布快照缺少单位或粒度");
        String aggregation = snapshot.getAggregation();
        require(AGGREGATIONS.contains(aggregation), "不支持的聚合方式");
        List<Long> publishedDimensions = snapshot.getDimensionPropertyIds();
        List<MetricFilterVO> fixed = snapshot.getFixedFilters();
        require(publishedDimensions != null && publishedDimensions.size() <= 5 && !publishedDimensions.contains(null)
                && new HashSet<>(publishedDimensions).size() == publishedDimensions.size(), "发布维度无效或超限");
        require(fixed != null && fixed.size() + req.getFilters().size() <= 20, "固定及用户过滤条件合计最多20个");
        require(publishedDimensions.containsAll(req.getDimensionPropertyIds()), "只能选择已发布维度");
        Map<String, Object> source = snapshot.getSourceSnapshot();
        require(source != null && "DORIS".equalsIgnoreCase(text(source.get("datasourceType")))
                && "METADATA_ONLY".equals(source.get("validationScope")), "不支持的来源快照");
        Long datasourceId = positiveId(source.get("datasourceId"));
        String database = identifier(text(source.get("databaseName")));
        String table = identifier(text(source.get("tableName")));
        String schema = text(source.get("schemaName"));
        require(schema == null || schema.isEmpty() || schema.equals(database), "仅支持与数据库相同或空的schema");
        require(MetricRules.adsSource(database, table), "仅支持已批准的Doris ADS表或视图");
        Object rawColumns = source.get("propertyColumns");
        require(rawColumns instanceof Map && ((Map<?, ?>) rawColumns).size() <= 32, "发布字段映射无效或超限");
        Map<?, ?> mappings = (Map<?, ?>) rawColumns;
        // Validate the entire definition, even dimensions omitted from this particular grouping.
        Set<Long> definitionIds = new LinkedHashSet<>(publishedDimensions);
        if (snapshot.getValuePropertyId() != null) definitionIds.add(snapshot.getValuePropertyId());
        if (snapshot.getTimePropertyId() != null) definitionIds.add(snapshot.getTimePropertyId());
        for (MetricFilterVO filter : fixed) {
            require(filter != null && filter.getPropertyId() != null, "固定条件无效");
            definitionIds.add(filter.getPropertyId());
        }
        for (Long id : definitionIds) field(mappings, id);
        List<String> select = new ArrayList<>();
        List<String> group = new ArrayList<>();
        List<MetricQueryColumnVO> columns = new ArrayList<>();
        Set<String> usedColumns = new LinkedHashSet<>();
        for (Long id : req.getDimensionPropertyIds()) {
            Field f = field(mappings, id); usedColumns.add(f.column);
            String key = "d_" + id;
            select.add(quote(f.column) + " AS " + quote(key)); group.add(quote(f.column));
            columns.add(column(key, f.label, f.type));
        }
        Field valueField = snapshot.getValuePropertyId() == null ? null : field(mappings, snapshot.getValuePropertyId());
        require(valueField != null || "COUNT".equals(aggregation), "聚合缺少度量属性");
        if ("SUM".equals(aggregation) || "AVG".equals(aggregation)) {
            require(valueField != null && MetricRules.numeric(valueField.type), "SUM/AVG只支持数值属性");
        }
        String value = valueField == null ? "*" : quote(valueField.column);
        if (valueField != null) usedColumns.add(valueField.column);
        String expression = "COUNT_DISTINCT".equals(aggregation) ? "COUNT(DISTINCT " + value + ")" : aggregation + "(" + value + ")";
        select.add(expression + " AS `metric_value`");
        columns.add(column("metric_value", snapshot.getName(), aggregation.startsWith("COUNT") ? "long"
                : ("AVG".equals(aggregation) || "SUM".equals(aggregation)) ? "decimal" : valueField.type));
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        for (MetricFilterVO filter : fixed) appendFilter(filter, mappings, usedColumns, conditions, parameters);
        Set<Long> filterable = new HashSet<>(publishedDimensions);
        if (snapshot.getTimePropertyId() != null) filterable.add(snapshot.getTimePropertyId());
        for (MetricFilterVO filter : req.getFilters()) {
            require(filterable.contains(filter.getPropertyId()), "用户只能过滤已发布维度或时间属性");
            appendFilter(filter, mappings, usedColumns, conditions, parameters);
        }
        if (snapshot.getTimePropertyId() != null) {
            Field time = field(mappings, snapshot.getTimePropertyId()); usedColumns.add(time.column);
            require(MetricRules.temporal(time.type), "时间属性类型无效");
            require(req.getStartTime() != null, "含时间属性的指标必须提供开始和结束时间");
            Object start = typedValue(req.getStartTime(), time.type);
            Object end = typedValue(req.getEndTime(), time.type);
            boolean ordered = start instanceof java.sql.Date ? ((java.sql.Date) start).before((java.sql.Date) end)
                    : ((Timestamp) start).before((Timestamp) end);
            require(ordered, "开始时间必须小于结束时间");
            conditions.add(quote(time.column) + " >= ?"); parameters.add(start);
            conditions.add(quote(time.column) + " < ?"); parameters.add(end);
        } else require(req.getStartTime() == null, "该指标未发布可用时间属性");
        String sql = "SELECT " + String.join(", ", select) + " FROM " + quote(database) + "." + quote(table);
        if (!conditions.isEmpty()) sql += " WHERE " + String.join(" AND ", conditions);
        // Total-only queries have neither GROUP BY nor raw-dimension ORDER BY.
        if (!group.isEmpty()) sql += " GROUP BY " + String.join(", ", group) + " ORDER BY " + String.join(", ", group);
        sql += " LIMIT ?"; parameters.add(req.getLimit() + 1);
        return new Plan(sql, parameters, usedColumns, columns, datasourceId, database, table, req);
    }

    private static void appendFilter(MetricFilterVO filter, Map<?, ?> mappings, Set<String> used,
                                     List<String> conditions, List<Object> parameters) {
        require(filter != null && filter.getPropertyId() != null, "过滤属性无效");
        Field f = field(mappings, filter.getPropertyId()); used.add(f.column);
        String name = quote(f.column), op = filter.getOperator();
        if ("IS_NULL".equals(op) || "IS_NOT_NULL".equals(op)) {
            require(filter.getValue() == null, "NULL条件不得携带过滤值");
            conditions.add(name + ("IS_NULL".equals(op) ? " IS NULL" : " IS NOT NULL"));
        } else if ("IN".equals(op)) {
            require(filter.getValue() instanceof Collection, "IN必须为非空数组");
            Collection<?> values = (Collection<?>) filter.getValue();
            require(!values.isEmpty() && values.size() <= 100, "IN必须为1到100项");
            List<String> placeholders = new ArrayList<>();
            for (Object item : values) { parameters.add(typedValue(item, f.type)); placeholders.add("?"); }
            conditions.add(name + " IN (" + String.join(", ", placeholders) + ")");
        } else {
            require(OPERATORS.containsKey(op), "不支持的过滤运算符");
            parameters.add(typedValue(filter.getValue(), f.type));
            conditions.add(name + " " + OPERATORS.get(op) + " ?");
        }
    }

    public static Object typedValue(Object value, String dataType) {
        require(value instanceof String || value instanceof Number || value instanceof Boolean, "过滤值必须为非空标量");
        require(!(value instanceof String) || ((String) value).length() <= 2000, "过滤字符串最多2000字符");
        String type = type(dataType);
        try {
            if (MetricRules.numeric(type)) {
                require(value instanceof String || value instanceof Number, "数值过滤值格式错误");
                String text = String.valueOf(value);
                require(text.length() <= 2000, "数值过滤值过长");
                BigDecimal number = new BigDecimal(text);
                require(number.precision() <= 1000 && Math.abs((long) number.scale()) <= 1000, "数值过滤值超限");
                require(!INTEGER_TYPES.contains(type) || number.stripTrailingZeros().scale() <= 0, "整数属性不接受小数");
                return number;
            }
            if ("boolean".equals(type)) {
                require(value instanceof Boolean || "true".equals(value) || "false".equals(value), "布尔值只能为true/false");
                return Boolean.valueOf(String.valueOf(value));
            }
            if (MetricRules.temporal(type)) {
                require(value instanceof String, "日期必须为严格ISO字符串");
                return "date".equals(type) ? java.sql.Date.valueOf(LocalDate.parse((String) value))
                        : Timestamp.valueOf(LocalDateTime.parse((String) value));
            }
            require(value instanceof String, "文本属性只接受字符串");
            return value;
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new ServiceException("过滤值或时间范围格式不正确");
        }
    }

    public static String identifier(String value) {
        require(value != null && value.matches("^[A-Za-z_][A-Za-z0-9_]*$"), "物理标识符格式不安全");
        return value;
    }
    private static String quote(String value) { return "`" + identifier(value) + "`"; }
    private static String text(Object value) { require(value == null || value instanceof String, "快照字段类型无效"); return (String) value; }
    private static Long positiveId(Object value) {
        try {
            require(value instanceof Number || value instanceof String, "快照数据源ID无效");
            Long id = new BigDecimal(String.valueOf(value)).longValueExact();
            require(id > 0, "快照数据源ID无效"); return id;
        } catch (ArithmeticException | NumberFormatException ex) { throw new ServiceException("快照数据源ID无效"); }
    }
    private static String type(String value) {
        require(value != null, "发布属性缺少数据类型"); String type = value.toLowerCase(Locale.ROOT);
        require(MetricRules.numeric(type) || MetricRules.temporal(type) || Arrays.asList("boolean", "string", "text", "varchar", "char").contains(type), "不支持的发布属性类型");
        return type;
    }
    private static Field field(Map<?, ?> mappings, Long id) {
        require(id != null && id > 0, "属性ID无效"); Object item = mappings.get(String.valueOf(id));
        require(item instanceof Map, "发布快照缺少属性字段映射"); Map<?, ?> row = (Map<?, ?>) item;
        require(Objects.equals(positiveId(row.get("propertyId")), id), "发布快照属性身份不一致");
        return new Field(identifier(text(row.get("columnName"))), type(text(row.get("dataType"))), text(row.get("propertyName")));
    }
    private static MetricQueryColumnVO column(String key, String label, String type) {
        MetricQueryColumnVO result = new MetricQueryColumnVO(); result.setKey(key); result.setLabel(label); result.setDataType(type); return result;
    }
    private static final class Field {
        private final String column, type, label;
        private Field(String column, String type, String label) { this.column = column; this.type = type; this.label = label; }
    }
    @Getter
    public static final class Plan {
        private final String sql;
        private final List<Object> parameters;
        private final Set<String> usedColumns;
        private final List<MetricQueryColumnVO> columns;
        private final Long datasourceId;
        private final String databaseName, tableName;
        private final MetricQueryReqVO request;
        private Plan(String sql, List<Object> parameters, Set<String> usedColumns, List<MetricQueryColumnVO> columns,
                     Long datasourceId, String databaseName, String tableName, MetricQueryReqVO request) {
            this.sql = sql; this.parameters = Collections.unmodifiableList(parameters);
            this.usedColumns = Collections.unmodifiableSet(usedColumns); this.columns = Collections.unmodifiableList(columns);
            this.datasourceId = datasourceId; this.databaseName = databaseName; this.tableName = tableName; this.request = request;
        }
    }
}
