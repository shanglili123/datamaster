package com.datamaster.module.ontology.service.query;

import com.datamaster.common.exception.ServiceException;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TypedQueryStrictTest {
    @Test public void explicitEmptyUiGroupIsAllowedButMissingFilterListIsRejected() {
        TypedQuerySpec empty = TypedQueryBuilder.parseStrict("{\"groups\":[{\"connector\":\"AND\",\"filters\":[]}],\"columns\":[],\"orderBy\":[],\"keyword\":\"\"}");
        assertFalse(TypedQueryBuilder.compileStrict(empty, List.of("id"), Set.of()).hasWhere());
        TypedQuerySpec missing = TypedQueryBuilder.parseStrict("{\"groups\":[{\"connector\":\"AND\"}]}");
        reject(() -> TypedQueryBuilder.compileStrict(missing, List.of("id"), Set.of()));
    }
    @Test public void malformedJsonAndUnknownFieldsNeverBecomeUnfilteredQuery() {
        reject(() -> TypedQueryBuilder.parseStrict("broken")); reject(() -> TypedQueryBuilder.parseStrict("null"));
        reject(() -> TypedQueryBuilder.parseStrict("[]"));
        reject(() -> TypedQueryBuilder.parseStrict("{\"filter\":[{\"field\":\"id\",\"value\":1}]}"));
        reject(() -> TypedQueryBuilder.parseStrict("{\"groups\":[{\"negate\":true,\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":1}]}]}"));
        reject(() -> TypedQueryBuilder.parseStrict("{\"groups\":[{\"filters\":[{\"field\":\"id\",\"operator\":\"eq\",\"value\":1}]}]}"));
        reject(() -> TypedQueryBuilder.parseStrict("{\"keyword\":{\"unexpected\":true}}"));
        reject(() -> TypedQueryBuilder.parseStrict("{\"columns\":[1]}"));
        reject(() -> TypedQueryBuilder.parseStrict("{\"groups\":[{\"filters\":[{\"field\":1,\"op\":\"eq\",\"value\":1}]}]}"));
        TypedQuerySpec request = TypedQueryBuilder.parseStrict("{\"groups\":[{\"filters\":[{\"field\":\"secret\",\"op\":\"eq\",\"value\":\"x\"}]}]}");
        reject(() -> TypedQueryBuilder.compileStrict(request, List.of("id"), Set.of()));
        assertFalse(TypedQueryBuilder.compileStrict(TypedQueryBuilder.parseStrict(null), List.of("id"), Set.of()).hasWhere());
    }
    @Test public void invalidInNullAndSortNeverSilentlyDropPredicates() {
        for (String json : List.of(
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"in\",\"values\":[]}]}]}",
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":null}]}]}",
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"SQL\",\"value\":1}]}]}",
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"isnull\",\"value\":1}]}]}",
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":1,\"values\":[2]}]}]}",
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"in\",\"value\":1,\"values\":[2]}]}]}",
                "{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"between\",\"value\":1,\"values\":[2,3]}]}]}",
                "{\"orderBy\":[{\"field\":\"id\",\"dir\":\"desc;DROP\"}]}",
                "{\"columns\":[\"id\",\"secret\"]}")) {
            reject(() -> TypedQueryBuilder.compileStrict(TypedQueryBuilder.parseStrict(json), List.of("id"), Set.of()));
        }
    }
    @Test public void uiValueShapesRemainAcceptedWithoutAmbiguousUnusedValues() {
        TypedQuerySpec request = TypedQueryBuilder.parseStrict("{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"in\",\"values\":[1,2]},{\"field\":\"id\",\"op\":\"between\",\"values\":[1,3]},{\"field\":\"id\",\"op\":\"isnotnull\"}]}]}");
        TypedQueryBuilder.Compiled result = TypedQueryBuilder.compileStrict(request, List.of("id"), Set.of());
        assertTrue(result.where.contains(" IN ")); assertTrue(result.where.contains(" BETWEEN "));
        assertTrue(result.where.contains("IS NOT NULL")); assertEquals(4, result.params.size());
    }
    @Test public void notHasValidBooleanSemanticsForFirstAndLaterCondition() {
        TypedQuerySpec request = TypedQueryBuilder.parseStrict("{\"groups\":[{\"filters\":[{\"connector\":\"NOT\",\"field\":\"id\",\"op\":\"eq\",\"value\":1},{\"connector\":\"NOT\",\"field\":\"id\",\"op\":\"eq\",\"value\":2}]}]}");
        TypedQueryBuilder.Compiled result = TypedQueryBuilder.compileStrict(request, List.of("id"), Set.of());
        assertEquals("(NOT (id = :p0) AND NOT (id = :p1))", result.where); assertEquals(2, result.params.size());
        assertEquals("NOT", request.getGroups().get(0).getFilters().get(0).getConnector());
    }
    @Test public void keywordIsAndedWithWholeOrGroupExpression() {
        TypedQuerySpec request = TypedQueryBuilder.parseStrict("{\"groups\":[{\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":1}]},{\"connector\":\"OR\",\"filters\":[{\"field\":\"id\",\"op\":\"eq\",\"value\":2}]}],\"keyword\":\"Alice\"}");
        TypedQueryBuilder.Compiled result = TypedQueryBuilder.compileStrict(request, List.of("id", "name"), Set.of("name"));
        assertEquals("((id = :p0) OR (id = :p1)) AND (LOWER(name) LIKE LOWER(:strict_kw0))", result.where);
        assertEquals("%Alice%", result.params.get("strict_kw0")); assertEquals(3, result.params.size());
        assertEquals("Alice", request.getKeyword());
        reject(() -> TypedQueryBuilder.compileStrict(request, List.of("id"), Set.of()));
    }
    @Test public void keywordParametersRemainDistinctBeyondTenColumns() {
        List<String> columns = new ArrayList<>(); for (int i = 0; i < 12; i++) columns.add("text_" + i);
        TypedQuerySpec request = new TypedQuerySpec(); request.setKeyword("needle");
        TypedQueryBuilder.Compiled result = TypedQueryBuilder.compileStrict(request, columns, new LinkedHashSet<>(columns));
        assertTrue(result.where.contains(":strict_kw10)")); assertFalse(result.where.contains("strict_strict"));
        assertEquals(12, result.params.size());
    }
    @Test public void injectionStaysBoundAndInputDoesNotGetRewritten() {
        TypedQuerySpec request = new TypedQuerySpec(); TypedFilter filter = new TypedFilter(); filter.setField("id"); filter.setOp("eq"); filter.setValue("x' OR 1=1--");
        TypedQuerySpec.FilterGroup group = new TypedQuerySpec.FilterGroup(); group.setFilters(List.of(filter)); request.setGroups(List.of(group));
        TypedQueryBuilder.Compiled result = TypedQueryBuilder.compileStrict(request, List.of("id"), Set.of());
        assertFalse(result.where.contains("OR 1=1")); assertEquals("x' OR 1=1--", result.params.get("p0"));
    }
    private static void reject(Runnable action) { try { action.run(); fail("Must reject invalid consumption request"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); } }
}
