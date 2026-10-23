package com.datamaster.module.collector.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JoinQueryBuilderTest {

    @Test
    public void buildsStructuredJoinAndFixedSourceLabel() {
        assertEquals("SELECT `a`.`id` AS `customer_id`, `b`.`name` AS `order_name`, "
                        + "'JOIN[Sales.Customer,Sales.Order]' AS `source_table` FROM `Sales`.`Customer` `a`"
                        + " LEFT JOIN `Sales`.`Order` `b` ON (`a`.`id` = `b`.`customer_id`)",
                JoinQueryBuilder.build(inputs(), config(), "MYSQL", metadata()));
    }

    @Test
    public void quotesIdentifiersForEachDialectWithoutChangingCase() {
        String postgres = JoinQueryBuilder.build(inputs(), config(), "POSTGRE_SQL", metadata());
        assertTrue(postgres.contains("FROM \"Sales\".\"Customer\" \"a\""));
        assertTrue(postgres.contains("\"b\".\"name\" AS \"order_name\""));
        String sqlServer = JoinQueryBuilder.build(inputs(), config(), "SQL_SERVER", metadata());
        assertTrue(sqlServer.contains("FROM [Sales].[Customer] [a]"));
        for (String database : Arrays.asList("DORIS", "CLICK_HOUSE", "HIVE", "MARIADB")) {
            assertTrue(JoinQueryBuilder.build(inputs(), config(), database, metadata())
                    .contains("FROM `Sales`.`Customer` `a`"));
        }
    }

    @Test
    public void rejectsDuplicateAliasesIgnoringCase() {
        List<Map<String, Object>> inputs = inputs();
        inputs.get(1).put("alias", "A");
        Map<String, Object> config = config();
        config.put("sources", Arrays.asList(obj("nodeId", "n1", "alias", "a"),
                obj("nodeId", "n2", "alias", "A")));
        rejects(() -> JoinQueryBuilder.build(inputs, config, "MYSQL", metadata()));
    }

    @Test
    public void rejectsUnknownProjectionAndConditionColumns() {
        Map<String, Object> projection = config();
        projection.put("fields", Collections.singletonList(obj("nodeId", "n1", "column", "missing", "outputName", "out")));
        rejects(() -> JoinQueryBuilder.build(inputs(), projection, "MYSQL", metadata()));
        Map<String, Object> condition = config();
        condition.put("joins", Collections.singletonList(join("n2", "LEFT", "n1", "id", "missing")));
        rejects(() -> JoinQueryBuilder.build(inputs(), condition, "MYSQL", metadata()));
    }

    @Test
    public void fullJoinUsesExplicitCapabilityWhitelist() {
        Map<String, Object> config = config();
        config.put("joins", Collections.singletonList(join("n2", "FULL", "n1", "id", "customer_id")));
        for (String database : Arrays.asList("MYSQL", "MARIADB", "HIVE", "DORIS", "unknown")) {
            rejects(() -> JoinQueryBuilder.build(inputs(), config, database, metadata()));
        }
        for (String database : Arrays.asList("POSTGRE_SQL", "ORACLE", "SQL_SERVER", "KINGBASE8", "DM8")) {
            assertTrue(JoinQueryBuilder.build(inputs(), config, database, metadata()).contains(" FULL JOIN "));
        }
    }

    @Test
    public void rejectsUnsafeIdentifiersAndArbitraryExpressions() {
        List<Map<String, Object>> inputs = inputs();
        inputs.get(0).put("table_name", "Sales.Customer;DROP TABLE x");
        rejects(() -> JoinQueryBuilder.build(inputs, config(), "MYSQL", metadata()));
        Map<String, Object> config = config();
        config.put("fields", Collections.singletonList(obj("nodeId", "n1", "column", "id + 1", "outputName", "out")));
        rejects(() -> JoinQueryBuilder.build(inputs(), config, "MYSQL", metadata()));
    }

    @Test
    public void rejectsDuplicateEmptyAndReservedOutputNames() {
        for (String output : Arrays.asList("customer_id", "CUSTOMER_ID", "", "source_table", "WRITE_TIME")) {
            Map<String, Object> config = config();
            config.put("fields", Arrays.asList(
                    obj("nodeId", "n1", "column", "id", "outputName", "customer_id"),
                    obj("nodeId", "n2", "column", "name", "outputName", output)));
            rejects(() -> JoinQueryBuilder.build(inputs(), config, "MYSQL", metadata()));
        }
    }

    @Test
    public void validatesJoinCountConnectivityAndNonemptyConditions() {
        Map<String, Object> missing = config();
        missing.put("joins", Collections.emptyList());
        rejects(() -> JoinQueryBuilder.build(inputs(), missing, "MYSQL", metadata()));
        Map<String, Object> reused = config();
        reused.put("joins", Collections.singletonList(join("n1", "LEFT", "n1", "id", "id")));
        rejects(() -> JoinQueryBuilder.build(inputs(), reused, "MYSQL", metadata()));
        Map<String, Object> unknown = config();
        unknown.put("joins", Collections.singletonList(join("n2", "LEFT", "n3", "id", "customer_id")));
        rejects(() -> JoinQueryBuilder.build(inputs(), unknown, "MYSQL", metadata()));
        Map<String, Object> emptyJoin = join("n2", "LEFT", "n1", "id", "customer_id");
        emptyJoin.put("conditions", Collections.emptyList());
        Map<String, Object> empty = config();
        empty.put("joins", Collections.singletonList(emptyJoin));
        rejects(() -> JoinQueryBuilder.build(inputs(), empty, "MYSQL", metadata()));
    }

    @Test
    public void joinsInListOrderAndAllowsPreviouslyJoinedLeftSource() {
        List<Map<String, Object>> inputs = Arrays.asList(inputs().get(0), inputs().get(1),
                obj("refNodeId", "n3", "table_name", "Region", "alias", "c", "columns", Arrays.asList("id", "region")));
        Map<String, Object> config = config();
        config.put("sources", Arrays.asList(obj("nodeId", "n1", "alias", "a"),
                obj("nodeId", "n2", "alias", "b"), obj("nodeId", "n3", "alias", "c")));
        config.put("joins", Arrays.asList(join("n3", "INNER", "n1", "id", "id"),
                join("n2", "RIGHT", "n3", "id", "customer_id")));
        String sql = JoinQueryBuilder.build(inputs, config, "MYSQL", metadata());
        assertTrue(sql.indexOf("INNER JOIN `Region`") < sql.indexOf("RIGHT JOIN `Sales`.`Order`"));
        assertTrue(sql.contains("ON (`c`.`id` = `b`.`customer_id`)"));
        assertFalse(sql.contains(" AS `write_time`"));
        assertFalse(sql.contains("hash"));
    }

    @Test
    public void supportsMultipleConditionsButRejectsInjectedOperatorAndRelation() {
        Map<String, Object> join = join("n2", "INNER", "n1", "id", "customer_id");
        join.put("conditions", Arrays.asList(
                obj("leftNodeId", "n1", "leftColumn", "id", "operator", ">=", "rightColumn", "customer_id"),
                obj("leftNodeId", "n1", "leftColumn", "id", "operator", "!=", "rightColumn", "id")));
        join.put("relation", "OR");
        Map<String, Object> config = config();
        config.put("joins", Collections.singletonList(join));
        assertTrue(JoinQueryBuilder.build(inputs(), config, "MYSQL", metadata())
                .contains("ON (`a`.`id` >= `b`.`customer_id` OR `a`.`id` != `b`.`id`)"));
        join.put("relation", "OR 1=1 --");
        rejects(() -> JoinQueryBuilder.build(inputs(), config, "MYSQL", metadata()));
        join.put("relation", "AND");
        join.put("conditions", Collections.singletonList(
                obj("leftNodeId", "n1", "leftColumn", "id", "operator", "= 1 OR", "rightColumn", "id")));
        rejects(() -> JoinQueryBuilder.build(inputs(), config, "MYSQL", metadata()));
    }

    @Test
    public void supportsFourSourcesWithChainedJoinConditions() {
        List<Map<String, Object>> sources = new ArrayList<>(inputs());
        sources.add(obj("refNodeId", "n3", "table_name", "Region", "alias", "c", "columns", Arrays.asList("id")));
        sources.add(obj("refNodeId", "n4", "table_name", "Area", "alias", "d", "columns", Arrays.asList("id")));
        Map<String, Object> config = config();
        config.put("sources", Arrays.asList(obj("nodeId", "n1", "alias", "a"), obj("nodeId", "n2", "alias", "b"),
                obj("nodeId", "n3", "alias", "c"), obj("nodeId", "n4", "alias", "d")));
        config.put("joins", Arrays.asList(join("n2", "LEFT", "n1", "id", "customer_id"),
                join("n3", "INNER", "n2", "id", "id"), join("n4", "LEFT", "n3", "id", "id")));
        String sql = JoinQueryBuilder.build(sources, config, "POSTGRESQL", metadata());
        assertTrue(sql.contains("INNER JOIN \"Region\" \"c\" ON (\"b\".\"id\" = \"c\".\"id\")"));
        assertTrue(sql.contains("LEFT JOIN \"Area\" \"d\" ON (\"c\".\"id\" = \"d\".\"id\")"));
    }

    private static List<Map<String, Object>> inputs() {
        return Arrays.asList(
                obj("refNodeId", "n1", "table_name", "Sales.Customer", "alias", "a", "columns", Arrays.asList("id", "name"),
                        "sourceTableColumn", "upstream_source"),
                obj("refNodeId", "n2", "table_name", "Sales.Order", "alias", "b", "columns", Arrays.asList("id", "customer_id", "name"),
                        "sourceTableColumn", "upstream_source"));
    }

    private static Map<String, Object> config() {
        return obj("sources", Arrays.asList(obj("nodeId", "n1", "alias", "a"), obj("nodeId", "n2", "alias", "b")),
                "joins", Collections.singletonList(join("n2", "LEFT", "n1", "id", "customer_id")),
                "fields", Arrays.asList(obj("nodeId", "n1", "column", "id", "outputName", "customer_id"),
                        obj("nodeId", "n2", "column", "name", "outputName", "order_name")));
    }

    private static Map<String, Object> join(String right, String type, String left, String leftColumn, String rightColumn) {
        return obj("rightNodeId", right, "type", type, "relation", "AND", "conditions", Collections.singletonList(
                obj("leftNodeId", left, "leftColumn", leftColumn, "operator", "=", "rightColumn", rightColumn)));
    }

    private static Map<String, Object> metadata() {
        return obj("sourceTableColumn", "source_table", "writeTimeColumn", "write_time");
    }

    private static Map<String, Object> obj(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String) entries[i], entries[i + 1]);
        }
        return result;
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }
}
