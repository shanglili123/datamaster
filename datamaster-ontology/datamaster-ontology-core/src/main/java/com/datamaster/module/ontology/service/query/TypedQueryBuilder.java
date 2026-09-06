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
 *   - 所有字段（过滤列 / 排序列 / 投影列）必须命中物理表列白名单，否则丢弃该条件（保持既有"取不到元数据则退化为全量"语义）；
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
                                ? group.getConnector().toUpperCase() : "AND";
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
                conds.add(cond);
                first = false;
            } else {
                String fConn = filter.getConnector() == null
                        ? CONN_AND : filter.getConnector().toUpperCase();
                if (CONN_NOT.equals(fConn)) {
                    conds.add(" NOT (" + cond + ")");
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
        String op = filter.getOp() == null ? OP_EQ : filter.getOp().toLowerCase();
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
