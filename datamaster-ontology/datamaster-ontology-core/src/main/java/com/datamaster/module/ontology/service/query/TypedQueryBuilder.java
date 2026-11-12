package com.datamaster.module.ontology.service.query;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型化查询编译器 — 把 {@link TypedQuerySpec} 编译为白名单校验过、NamedParameter 参数化的安全 SQL 片段。
 *
 * 安全策略（d5/d6）：
 *   - strict消费入口拒绝未知字段/无效条件；旧compile兼容入口仍保留丢弃行为供分步迁移；
 *   - 值一律经 NamedParameter 占位符绑定，杜绝字符串拼接注入；
 *   - 不产生跨表物理 JOIN（关系跳转由上层用目标表值过滤实现，见 d8）。
 *
 * @author datamaster
 */
public class TypedQueryBuilder {

    /** 运算符常量 */
    public static final String OP_EQ = "eq";
    public static final String OP_NE = "ne";
    public static final String OP_GT = "gt";
    public static final String OP_GE = "ge";
    public static final String OP_LT = "lt";
    public static final String OP_LE = "le";
    public static final String OP_LIKE = "like";
    public static final String OP_IN = "in";
    public static final String OP_BETWEEN = "between";
    public static final String OP_IS_NULL = "isnull";
    public static final String OP_IS_NOT_NULL = "isnotnull";

    /** 连接符常量 */
    public static final String CONN_AND = "AND";
    public static final String CONN_OR = "OR";
    public static final String CONN_NOT = "NOT";

    /** 编译产物 */
    public static class Compiled {
        /** 列投影前缀（不含 FROM）：为空时用 SELECT * */
        public final String projection;
        /** WHERE 子句（不含 WHERE 关键字，可为空串） */
        public final String where;
        /** ORDER BY 子句（不含 ORDER BY 关键字，可为空串） */
        public final String orderBy;
        /** NamedParameter 参数 */
        public final Map<String, Object> params;

        Compiled(String projection, String where, String orderBy, Map<String, Object> params) {
            this.projection = projection;
            this.where = where;
            this.orderBy = orderBy;
            this.params = params;
        }

        public boolean hasProjection() {
            return StringUtils.isNotBlank(projection);
        }

        public boolean hasWhere() {
            return StringUtils.isNotBlank(where);
        }

        public boolean hasOrderBy() {
            return StringUtils.isNotBlank(orderBy);
        }
    }

    private TypedQueryBuilder() {
        // 纯静态工具
    }

    /**
     * 解析结构化 JSON 字符串为查询说明。
     *
     * @param json filters JSON，形如：
     *   {"groups":[{"connector":"AND","negate":false,"filters":[{"field":"name","op":"eq","value":"张三"}]}],
     *    "orderBy":[{"field":"id","dir":"desc"}],"columns":["id","name"],"keyword":"张"}
     * @return 为空/非法返回不含条件的空 spec
     */
    public static TypedQuerySpec parse(String json) {
        if (!StringUtils.hasText(json)) {
            return new TypedQuerySpec();
        }
        try {
            return JSON.parseObject(json, TypedQuerySpec.class);
        } catch (Exception e) {
            // 非法时退化为全量查询
            return new TypedQuerySpec();
        }
    }

    /** Consumption entrances must not silently drop malformed or unauthorized predicates. */
    public static TypedQuerySpec parseStrict(String json) {
        if (!StringUtils.hasText(json)) return new TypedQuerySpec();
        if (json.length() > 50000) throw new com.datamaster.common.exception.ServiceException("查询条件过长");
        try {
            if (!json.trim().startsWith("{")) throw new IllegalArgumentException();
            com.alibaba.fastjson2.JSONObject raw = JSON.parseObject(json);
            rejectUnknownKeys(raw, java.util.Set.of("groups", "orderBy", "columns", "keyword"));
            if (raw.get("keyword") != null && !(raw.get("keyword") instanceof String)) throw new IllegalArgumentException();
            if (raw.get("columns") != null) {
                if (!(raw.get("columns") instanceof java.util.List)) throw new IllegalArgumentException();
                for (Object column : (java.util.List<?>) raw.get("columns")) if (!(column instanceof String)) throw new IllegalArgumentException();
            }
            if (raw.get("groups") != null) {
                if (!(raw.get("groups") instanceof java.util.List)) throw new IllegalArgumentException();
                for (Object item : (java.util.List<?>) raw.get("groups")) {
                    if (!(item instanceof com.alibaba.fastjson2.JSONObject)) throw new IllegalArgumentException();
                    com.alibaba.fastjson2.JSONObject group = (com.alibaba.fastjson2.JSONObject) item;
                    rejectUnknownKeys(group, java.util.Set.of("connector", "filters"));
                    rawText(group, "connector");
                    if (group.get("filters") != null) {
                        if (!(group.get("filters") instanceof java.util.List)) throw new IllegalArgumentException();
                        for (Object condition : (java.util.List<?>) group.get("filters")) {
                            if (!(condition instanceof com.alibaba.fastjson2.JSONObject)) throw new IllegalArgumentException();
                            com.alibaba.fastjson2.JSONObject filter = (com.alibaba.fastjson2.JSONObject) condition;
                            rejectUnknownKeys(filter, java.util.Set.of("connector", "field", "op", "value", "values"));
                            rawText(filter, "connector"); rawText(filter, "field"); rawText(filter, "op");
                            if (filter.get("values") != null && !(filter.get("values") instanceof java.util.List)) throw new IllegalArgumentException();
                        }
                    }
                }
            }
            if (raw.get("orderBy") != null) {
                if (!(raw.get("orderBy") instanceof java.util.List)) throw new IllegalArgumentException();
                for (Object item : (java.util.List<?>) raw.get("orderBy")) {
                    if (!(item instanceof com.alibaba.fastjson2.JSONObject)) throw new IllegalArgumentException();
                    rejectUnknownKeys((com.alibaba.fastjson2.JSONObject) item, java.util.Set.of("field", "dir"));
                    rawText((com.alibaba.fastjson2.JSONObject) item, "field"); rawText((com.alibaba.fastjson2.JSONObject) item, "dir");
                }
            }
            TypedQuerySpec spec = raw.toJavaObject(TypedQuerySpec.class);
            if (spec == null) throw new IllegalArgumentException();
            return spec;
        } catch (RuntimeException invalid) { throw new com.datamaster.common.exception.ServiceException("查询条件JSON无效，不能降级为无条件查询"); }
    }
    private static void rawText(com.alibaba.fastjson2.JSONObject object, String field) {
        if (object.get(field) != null && !(object.get(field) instanceof String)) throw new IllegalArgumentException("Query member must be text");
    }
    private static void rejectUnknownKeys(com.alibaba.fastjson2.JSONObject object, java.util.Set<String> allowed) {
        if (object == null || !allowed.containsAll(object.keySet())) throw new IllegalArgumentException("Unknown structured query member");
    }
    public static Compiled compileStrict(TypedQuerySpec spec, List<String> whitelist, java.util.Set<String> textColumns) {
        if (whitelist == null || whitelist.isEmpty()) throw new com.datamaster.common.exception.ServiceException("缺少查询字段白名单");
        if (spec == null) throw new com.datamaster.common.exception.ServiceException("查询条件不能为空");
        // Isolate from caller mutation while validating/compiling the same structured request.
        spec = JSON.parseObject(JSON.toJSONString(spec, com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue), TypedQuerySpec.class);
        if (spec.getColumns() != null) {
            if (spec.getColumns().size() > 200) throw new com.datamaster.common.exception.ServiceException("投影字段超过200项");
            for (String column : spec.getColumns()) strictField(column, whitelist);
        }
        if (spec.getOrderBy() != null) {
            if (spec.getOrderBy().size() > 10) throw new com.datamaster.common.exception.ServiceException("排序最多10项");
            for (TypedQuerySpec.OrderByField order : spec.getOrderBy()) {
                if (order == null || order.getDir() == null || !("asc".equalsIgnoreCase(order.getDir()) || "desc".equalsIgnoreCase(order.getDir()))) throw new com.datamaster.common.exception.ServiceException("查询排序无效");
                strictField(order.getField(), whitelist);
            }
        }
        int total = 0;
        if (spec.getGroups() != null) {
            if (spec.getGroups().size() > 20) throw new com.datamaster.common.exception.ServiceException("过滤组最多20项");
            for (TypedQuerySpec.FilterGroup group : spec.getGroups()) {
                if (group == null) throw new com.datamaster.common.exception.ServiceException("过滤组不能为空");
                strictConnector(group.getConnector(), false);
                if (group.getFilters() == null) throw new com.datamaster.common.exception.ServiceException("过滤组条件列表不能为空");
                // UI editors retain one explicit empty placeholder group before any filter is added.
                if (group.getFilters().isEmpty()) continue;
                for (TypedFilter filter : group.getFilters()) {
                    if (++total > 100 || filter == null || filter.getOp() == null) throw new com.datamaster.common.exception.ServiceException("过滤条件无效或超过100项");
                    strictField(filter.getField(), whitelist); strictConnector(filter.getConnector(), true);
                    String op = filter.getOp().toLowerCase(java.util.Locale.ROOT);
                    filter.setOp(op);
                    if (!java.util.Set.of("eq", "ne", "gt", "ge", "lt", "le", "like", "in", "between", "isnull", "isnotnull").contains(op)) throw new com.datamaster.common.exception.ServiceException("过滤运算符无效");
                    if ("isnull".equals(op) || "isnotnull".equals(op)) {
                        if (filter.getValue() != null || filter.getValues() != null && !filter.getValues().isEmpty()) throw new com.datamaster.common.exception.ServiceException("空值判断不能携带其他条件值");
                    } else if ("in".equals(op) || "between".equals(op)) {
                        if (filter.getValue() != null) throw new com.datamaster.common.exception.ServiceException("多值运算不能同时携带单值条件");
                        if (filter.getValues() == null || filter.getValues().isEmpty() || filter.getValues().size() > 100 || "between".equals(op) && filter.getValues().size() != 2) throw new com.datamaster.common.exception.ServiceException("IN/BETWEEN条件值无效");
                        for (Object value : filter.getValues()) strictScalar(value);
                    } else {
                        if (filter.getValues() != null && !filter.getValues().isEmpty()) throw new com.datamaster.common.exception.ServiceException("单值运算不能携带values数组");
                        strictScalar(filter.getValue());
                    }
                }
            }
        }
        if (spec.getKeyword() != null && spec.getKeyword().length() > 2000) throw new com.datamaster.common.exception.ServiceException("关键字超过2000字符");
        if (StringUtils.hasText(spec.getKeyword()) && textColumns != null && textColumns.isEmpty()) throw new com.datamaster.common.exception.ServiceException("当前对象无可用文本字段，不能静默忽略关键字");
        String keyword = spec.getKeyword(); spec.setKeyword(null);
        Compiled grouped = compile(spec, whitelist, textColumns);
        if (!StringUtils.hasText(keyword)) return grouped;
        TypedQuerySpec keywordOnly = new TypedQuerySpec(); keywordOnly.setKeyword(keyword);
        Compiled keywordClause = compile(keywordOnly, whitelist, textColumns);
        if (!keywordClause.hasWhere()) throw new com.datamaster.common.exception.ServiceException("关键字未形成有效查询条件");
        // Existing group OR terms must be parenthesized before AND keyword restriction.
        Map<String,Object> params = new LinkedHashMap<>(grouped.params);
        String condition = keywordClause.where;
        for (Map.Entry<String,Object> item : keywordClause.params.entrySet()) {
            String distinct = "strict_" + item.getKey();
            condition = condition.replaceAll(java.util.regex.Pattern.quote(":" + item.getKey()) + "(?![A-Za-z0-9_])", ":" + distinct);
            params.put(distinct, item.getValue());
        }
        String where = grouped.hasWhere() ? "(" + grouped.where + ") AND " + condition : condition;
        return new Compiled(grouped.projection, where, grouped.orderBy, params);
    }
    private static void strictField(String field, List<String> whitelist) {
        if (field == null || !field.matches("[A-Za-z_][A-Za-z0-9_$]*") || !whitelist.contains(field)) throw new com.datamaster.common.exception.ServiceException("查询字段不属于当前已授权白名单");
    }
    private static void strictConnector(String connector, boolean allowNot) {
        if (connector != null && !connector.isBlank() && !("AND".equalsIgnoreCase(connector) || "OR".equalsIgnoreCase(connector) || allowNot && "NOT".equalsIgnoreCase(connector))) throw new com.datamaster.common.exception.ServiceException("过滤连接符无效");
    }
    private static void strictScalar(Object value) {
        if (!(value instanceof String || value instanceof Number || value instanceof Boolean) || String.valueOf(value).length() > 2000) throw new com.datamaster.common.exception.ServiceException("过滤值必须为有效有界标量");
        if (value instanceof Number) {
            try {
                java.math.BigDecimal number = new java.math.BigDecimal(String.valueOf(value));
                if (number.precision() > 1000 || Math.abs((long) number.scale()) > 1000) throw new IllegalArgumentException();
            } catch (RuntimeException invalid) { throw new com.datamaster.common.exception.ServiceException("过滤数值无效或超限"); }
        }
    }

    /**
     * 按白名单编译查询说明为安全 SQL 片段。
     *
     * @param spec        解析后的查询说明（可为空 spec）
     * @param whitelist   物理列白名单；为空（元数据不可得）时不构造任何过滤/排序/投影，退化为 SELECT * 全量
     * @param textColumns 文本列子集（用于跨列模糊 LIKE）。为 null 时退化为全白名单列。
     * @return 编译产物
     */
    public static Compiled compile(TypedQuerySpec spec, List<String> whitelist, java.util.Set<String> textColumns) {
        List<String> cols = whitelist == null ? Collections.emptyList() : whitelist;
        boolean hasWhitelist = !cols.isEmpty();

        Map<String, Object> params = new LinkedHashMap<>();
        List<String> whereParts = new ArrayList<>();
        IntRef idx = new IntRef();

        if (spec != null && spec.getGroups() != null) {
            // 组 = 括号：组间按 group.connector（AND/OR，缺省 AND）连接
            boolean firstGroup = true;
            for (TypedQuerySpec.FilterGroup group : spec.getGroups()) {
                if (group == null || group.getFilters() == null || group.getFilters().isEmpty()) {
                    continue;
                }
                String groupSql = buildGroupSql(group, cols, hasWhitelist, params, idx);
                if (StringUtils.isNotBlank(groupSql)) {
                    if (firstGroup) {
                        whereParts.add(groupSql);
                        firstGroup = false;
                    } else {
                        String conn = "AND".equalsIgnoreCase(group.getConnector())
                                || "OR".equalsIgnoreCase(group.getConnector())
                                ? group.getConnector().toUpperCase(java.util.Locale.ROOT) : "AND";
                        whereParts.add(" " + conn + " " + groupSql);
                    }
                }
            }
        }

        // 关键字模糊：仅对文本列做 LOWER(col) LIKE LOWER(:kw)，避免数值/日期列类型错误且大小写不敏感
        if (spec != null && StringUtils.isNotBlank(spec.getKeyword()) && hasWhitelist) {
            List<String> kwCols = textColumns == null ? cols
                    : new ArrayList<>(textColumns);
            List<String> kwLike = new ArrayList<>();
            for (String col : kwCols) {
                if (!cols.contains(col)) {
                    continue;
                }
                String p = "kw" + (idx.v++);
                kwLike.add("LOWER(" + col + ") LIKE LOWER(:" + p + ")");
                params.put(p, "%" + spec.getKeyword() + "%");
            }
            if (!kwLike.isEmpty()) {
                whereParts.add("(" + String.join(" OR ", kwLike) + ")");
            }
        }

        // 排序
        String orderBy = "";
        if (spec != null && spec.getOrderBy() != null && hasWhitelist) {
            List<String> orderParts = new ArrayList<>();
            for (TypedQuerySpec.OrderByField of : spec.getOrderBy()) {
                if (of == null || StringUtils.isBlank(of.getField()) || !cols.contains(of.getField())) {
                    continue;
                }
                String dir = "desc".equalsIgnoreCase(of.getDir()) ? "DESC" : "ASC";
                orderParts.add(of.getField() + " " + dir);
            }
            if (!orderParts.isEmpty()) {
                orderBy = String.join(", ", orderParts);
            }
        }

        // 列投影
        String projection = "";
        if (spec != null && spec.getColumns() != null && !spec.getColumns().isEmpty() && hasWhitelist) {
            List<String> projParts = new ArrayList<>();
            for (String c : spec.getColumns()) {
                if (StringUtils.isNotBlank(c) && cols.contains(c)) {
                    projParts.add(c);
                }
            }
            if (!projParts.isEmpty()) {
                projection = String.join(", ", projParts);
            }
        }

        String where = String.join("", whereParts);
        return new Compiled(projection, where, orderBy, params);
    }

    /** 兼容便捷重载：不区分文本列，跨列模糊时按全部白名单列处理（调用方最好显式传 textColumns） */
    public static Compiled compile(TypedQuerySpec spec, List<String> whitelist) {
        return compile(spec, whitelist, null);
    }

    /**
     * 判断列类型是否为文本类（用于跨列模糊 LIKE）。基于 JDBC 返回的类型名（大写，含前缀）做宽松匹配。
     */
    public static boolean isTextType(String dataType) {
        if (dataType == null) {
            return false;
        }
        String t = dataType.toUpperCase();
        if (t.contains("CHAR") || t.contains("CLOB") || t.contains("TEXT") || t.contains("STRING")
                || t.contains("NCHAR") || t.contains("JSON") || t.contains("UUID")) {
            return true;
        }
        return false;
    }

    /**
     * 构建单个逻辑组（括号）：组内为扁平条件列表，每条条件自带 且/或/非 连接符。
     */
    private static String buildGroupSql(TypedQuerySpec.FilterGroup group, List<String> cols,
                                        boolean hasWhitelist, Map<String, Object> params, IntRef idx) {
        List<String> conds = new ArrayList<>();
        boolean first = true;
        for (TypedFilter filter : group.getFilters()) {
            if (filter == null || StringUtils.isBlank(filter.getField())) {
                continue;
            }
            // 白名单校验：未知列一律丢弃
            if (!hasWhitelist || !cols.contains(filter.getField())) {
                continue;
            }
            String cond = buildCondition(filter, params, idx);
            if (StringUtils.isBlank(cond)) {
                continue;
            }
            if (first) {
                conds.add(CONN_NOT.equalsIgnoreCase(filter.getConnector()) ? "NOT (" + cond + ")" : cond);
                first = false;
            } else {
                String fConn = filter.getConnector() == null
                        ? CONN_AND : filter.getConnector().toUpperCase(java.util.Locale.ROOT);
                if (CONN_NOT.equals(fConn)) {
                    conds.add(" AND NOT (" + cond + ")");
                } else {
                    String sep = CONN_OR.equals(fConn) ? " OR " : " AND ";
                    conds.add(sep + cond);
                }
            }
        }
        if (conds.isEmpty()) {
            return null;
        }
        return "(" + String.join("", conds) + ")";
    }

    /**
     * 单条条件编译为 "col op :param" 片段。
     */
    private static String buildCondition(TypedFilter filter, Map<String, Object> params, IntRef idx) {
        String col = filter.getField();
        String op = filter.getOp() == null ? OP_EQ : filter.getOp().toLowerCase(java.util.Locale.ROOT);
        switch (op) {
            case OP_IS_NULL:
                return col + " IS NULL";
            case OP_IS_NOT_NULL:
                return col + " IS NOT NULL";
            case OP_EQ:
            case OP_NE:
            case OP_GT:
            case OP_GE:
            case OP_LT:
            case OP_LE: {
                Object val = filter.getValue();
                if (val == null) {
                    return null;
                }
                String p = "p" + (idx.v++);
                params.put(p, val);
                return col + " " + symbol(op) + " :" + p;
            }
            case OP_LIKE: {
                Object val = filter.getValue();
                if (val == null) {
                    return null;
                }
                String p = "p" + (idx.v++);
                params.put(p, "%" + val + "%");
                return col + " LIKE :" + p;
            }
            case OP_IN: {
                List<Object> values = filter.getValues();
                if (values == null || values.isEmpty()) {
                    return null;
                }
                List<String> placeholders = new ArrayList<>();
                for (Object v : values) {
                    String p = "p" + (idx.v++);
                    params.put(p, v);
                    placeholders.add(":" + p);
                }
                return col + " IN (" + String.join(", ", placeholders) + ")";
            }
            case OP_BETWEEN: {
                List<Object> values = filter.getValues();
                if (values == null || values.size() < 2) {
                    return null;
                }
                String p1 = "p" + (idx.v++);
                String p2 = "p" + (idx.v++);
                params.put(p1, values.get(0));
                params.put(p2, values.get(1));
                return col + " BETWEEN :" + p1 + " AND :" + p2;
            }
            default:
                return null;
        }
    }

    private static String symbol(String op) {
        switch (op) {
            case OP_NE: return "<>";
            case OP_GT: return ">";
            case OP_GE: return ">=";
            case OP_LT: return "<";
            case OP_LE: return "<=";
            default: return "=";
        }
    }

    /** int 引用容器（lambda 内可变） */
    private static class IntRef {
        int v;

        IntRef() {
        }
    }
}
