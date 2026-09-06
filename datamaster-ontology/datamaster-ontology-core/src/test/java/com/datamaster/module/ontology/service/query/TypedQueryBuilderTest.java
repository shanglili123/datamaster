package com.datamaster.module.ontology.service.query;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 类型化查询编译器单元测试 — 覆盖运算符 / 组内扁平连接符（且/或/非）/ 组括号 / 排序 / 投影 / 文本列模糊 / 白名单校验。
 *
 * @author datamaster
 */
public class TypedQueryBuilderTest {

    private static final java.util.List<String> WHITELIST =
            Collections.unmodifiableList(Arrays.asList("id", "name", "age", "status"));

    /** 文本列（用于跨列模糊 LIKE）：name、status 为文本，id、age 数值 */
    private static final Set<String> TEXT_COLUMNS =
            Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("name", "status")));

    @Test
    public void testEmptySpecNoClause() {
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(new TypedQuerySpec(), WHITELIST, TEXT_COLUMNS);
        assertFalse(c.hasWhere());
        assertFalse(c.hasOrderBy());
        assertFalse(c.hasProjection());
        assertEquals(0, c.params.size());
    }

    @Test
    public void testEqualityFilter() {
        TypedQuerySpec spec = specWithGroup("AND",
                filter("AND", "name", TypedQueryBuilder.OP_EQ, "张三", null));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertEquals("(name = :p0)", c.where);
        assertEquals("张三", c.params.get("p0"));
    }

    @Test
    public void testFlatOrConnector() {
        TypedQuerySpec spec = specWithGroup("AND",
                filter("AND", "name", TypedQueryBuilder.OP_EQ, "张三", null),
                filter("OR", "status", TypedQueryBuilder.OP_EQ, "1", null));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertEquals("(name = :p0 OR status = :p1)", c.where);
    }

    @Test
    public void testFlatNotConnector() {
        TypedQuerySpec spec = specWithGroup("AND",
                filter("AND", "name", TypedQueryBuilder.OP_EQ, "张三", null),
                filter("NOT", "status", TypedQueryBuilder.OP_EQ, "1", null));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertEquals("(name = :p0 NOT (status = :p1))", c.where);
    }

    @Test
    public void testGroupParensAndConnector() {
        // 组1：(name = :p0)；组2：(age > :p1)；组间 OR
        TypedQuerySpec spec = new TypedQuerySpec();
        TypedQuerySpec.FilterGroup g1 = group("AND",
                filter("AND", "name", TypedQueryBuilder.OP_EQ, "张三", null));
        TypedQuerySpec.FilterGroup g2 = group("AND",
                filter("AND", "age", TypedQueryBuilder.OP_GT, 18, null));
        g2.setConnector("OR");
        spec.setGroups(Arrays.asList(g1, g2));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertEquals("(name = :p0) OR (age > :p1)", c.where);
    }

    @Test
    public void testComparisonOperators() {
        TypedQueryBuilder.Compiled gt = compileSingle("age", TypedQueryBuilder.OP_GT, 18, null);
        assertEquals("(age > :p0)", gt.where);
        TypedQueryBuilder.Compiled ne = compileSingle("age", TypedQueryBuilder.OP_NE, 18, null);
        assertEquals("(age <> :p0)", ne.where);
        TypedQueryBuilder.Compiled le = compileSingle("age", TypedQueryBuilder.OP_LE, 60, null);
        assertEquals("(age <= :p0)", le.where);
    }

    @Test
    public void testLikeOperator() {
        TypedQueryBuilder.Compiled c = compileSingle("name", TypedQueryBuilder.OP_LIKE, "张", null);
        assertEquals("(name LIKE :p0)", c.where);
        assertEquals("%张%", c.params.get("p0"));
    }

    @Test
    public void testInOperator() {
        TypedQueryBuilder.Compiled c = compileSingle("status", TypedQueryBuilder.OP_IN, null,
                Arrays.asList("1", "2", "3"));
        assertEquals("(status IN (:p0, :p1, :p2))", c.where);
        assertEquals("1", c.params.get("p0"));
        assertEquals("3", c.params.get("p2"));
    }

    @Test
    public void testBetweenOperator() {
        TypedQueryBuilder.Compiled c = compileSingle("age", TypedQueryBuilder.OP_BETWEEN, null,
                Arrays.asList(18, 60));
        assertEquals("(age BETWEEN :p0 AND :p1)", c.where);
        assertEquals(18, c.params.get("p0"));
        assertEquals(60, c.params.get("p1"));
    }

    @Test
    public void testIsNullOperator() {
        TypedQueryBuilder.Compiled c = compileSingle("name", TypedQueryBuilder.OP_IS_NULL, null, null);
        assertEquals("(name IS NULL)", c.where);
        assertEquals(0, c.params.size());
    }

    @Test
    public void testIsNotNullOperator() {
        TypedQueryBuilder.Compiled c = compileSingle("name", TypedQueryBuilder.OP_IS_NOT_NULL, null, null);
        assertEquals("(name IS NOT NULL)", c.where);
        assertEquals(0, c.params.size());
    }

    @Test
    public void testOrderByAndProjection() {
        TypedQuerySpec spec = new TypedQuerySpec();
        TypedQuerySpec.OrderByField of = new TypedQuerySpec.OrderByField();
        of.setField("id");
        of.setDir("desc");
        spec.setOrderBy(Collections.singletonList(of));
        spec.setColumns(Arrays.asList("id", "name"));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertEquals("id DESC", c.orderBy);
        assertEquals("id, name", c.projection);
        assertTrue(c.hasOrderBy());
        assertTrue(c.hasProjection());
    }

    @Test
    public void testKeywordOnlyOnTextColumns() {
        TypedQuerySpec spec = new TypedQuerySpec();
        spec.setKeyword("张");
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        // 仅文本列 name/status 参与模糊，且 LOWER 包裹、%kw% 参数绑定
        assertEquals("(LOWER(name) LIKE LOWER(:kw0) OR LOWER(status) LIKE LOWER(:kw1))", c.where);
        assertEquals("%张%", c.params.get("kw0"));
        assertEquals("%张%", c.params.get("kw1"));
    }

    @Test
    public void testKeywordNoTextColumnsSkipsFuzzy() {
        TypedQuerySpec spec = new TypedQuerySpec();
        spec.setKeyword("张");
        // 无文本列（全部数值），模糊条件被跳过，退化为无 WHERE
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, Collections.emptySet());
        assertFalse(c.hasWhere());
        assertEquals(0, c.params.size());
    }

    @Test
    public void testUnknownColumnDropped() {
        TypedQuerySpec spec = specWithGroup("AND",
                filter("AND", "evil_col; DROP TABLE t", TypedQueryBuilder.OP_EQ, "x", null));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        // 未知列不在白名单，被丢弃，不进入 where
        assertFalse(c.hasWhere());
        assertEquals(0, c.params.size());
    }

    @Test
    public void testEmptyWhitelistDropsAll() {
        TypedQuerySpec spec = specWithGroup("AND",
                filter("AND", "name", TypedQueryBuilder.OP_EQ, "张三", null));
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, Collections.emptyList(), TEXT_COLUMNS);
        assertFalse(c.hasWhere());
        assertEquals(0, c.params.size());
    }

    @Test
    public void testParseRoundTrip() {
        String json = "{\"groups\":[{\"connector\":\"OR\","
                + "\"filters\":[{\"connector\":\"AND\",\"field\":\"name\",\"op\":\"like\",\"value\":\"张\"},"
                + "{\"connector\":\"NOT\",\"field\":\"age\",\"op\":\"gt\",\"value\":18}]}],"
                + "\"orderBy\":[{\"field\":\"id\",\"dir\":\"asc\"}],"
                + "\"columns\":[\"id\",\"name\"]}";
        TypedQuerySpec spec = TypedQueryBuilder.parse(json);
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertEquals("(name LIKE :p0 NOT (age > :p1))", c.where);
        assertEquals("id ASC", c.orderBy);
        assertEquals("id, name", c.projection);
        assertEquals("%张%", c.params.get("p0"));
        assertEquals(18, c.params.get("p1"));
    }

    @Test
    public void testMalformedJsonDegradesToEmpty() {
        TypedQuerySpec spec = TypedQueryBuilder.parse("{not valid json");
        // 非法 JSON 退化为空 spec，无任何语句
        assertTrue(spec.getGroups() == null || spec.getGroups().isEmpty());
        TypedQueryBuilder.Compiled c = TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
        assertFalse(c.hasWhere());
    }

    @Test
    public void testIsTextTypeDetection() {
        assertTrue(TypedQueryBuilder.isTextType("VARCHAR"));
        assertTrue(TypedQueryBuilder.isTextType("nvarchar2"));
        assertTrue(TypedQueryBuilder.isTextType("TEXT"));
        assertTrue(TypedQueryBuilder.isTextType("CLOB"));
        assertTrue(TypedQueryBuilder.isTextType("longtext"));
        assertFalse(TypedQueryBuilder.isTextType("INT"));
        assertFalse(TypedQueryBuilder.isTextType("NUMBER"));
        assertFalse(TypedQueryBuilder.isTextType("DATE"));
        assertFalse(TypedQueryBuilder.isTextType(null));
    }

    // ==================== 测试工具 ====================

    private static TypedQuerySpec specWithGroup(String groupConnector, TypedFilterHolder... filtersArr) {
        TypedQuerySpec spec = new TypedQuerySpec();
        spec.setGroups(Collections.singletonList(group(groupConnector, filtersArr)));
        return spec;
    }

    private static TypedQuerySpec.FilterGroup group(String groupConnector, TypedFilterHolder... filtersArr) {
        TypedQuerySpec.FilterGroup g = new TypedQuerySpec.FilterGroup();
        g.setConnector(groupConnector);
        java.util.List<TypedFilter> fs = new java.util.ArrayList<>();
        for (TypedFilterHolder h : filtersArr) {
            fs.add(toFilter(h));
        }
        g.setFilters(fs);
        return g;
    }

    private static TypedFilter toFilter(TypedFilterHolder h) {
        TypedFilter f = new TypedFilter();
        f.setConnector(h.connector);
        f.setField(h.field);
        f.setOp(h.op);
        f.setValue(h.value);
        f.setValues(h.values);
        return f;
    }

    private static TypedFilterHolder filter(String connector, String field, String op, Object value,
                                            java.util.List<Object> values) {
        return new TypedFilterHolder(connector, field, op, value, values);
    }

    private static TypedQueryBuilder.Compiled compileSingle(String field, String op, Object value,
                                                            java.util.List<Object> values) {
        TypedQuerySpec spec = specWithGroup("AND", filter("AND", field, op, value, values));
        return TypedQueryBuilder.compile(spec, WHITELIST, TEXT_COLUMNS);
    }

    /** 简单 holder，避免测试里写多个参数数组 */
    private static class TypedFilterHolder {
        final String connector;
        final String field;
        final String op;
        final Object value;
        final java.util.List<Object> values;

        TypedFilterHolder(String connector, String field, String op, Object value, java.util.List<Object> values) {
            this.connector = connector;
            this.field = field;
            this.op = op;
            this.value = value;
            this.values = values;
        }
    }
}
