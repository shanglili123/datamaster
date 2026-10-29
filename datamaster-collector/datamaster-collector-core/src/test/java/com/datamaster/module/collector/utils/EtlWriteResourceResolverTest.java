package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.exception.ServiceException;
import org.junit.Test;

import java.util.SortedSet;

import static org.junit.Assert.*;

public class EtlWriteResourceResolverTest {
    @Test
    public void aliasesAndCredentialsDoNotChangePhysicalIdentity() {
        JSONObject a = writer("PostgreSQL", "public.orders");
        JSONObject b = writer("PostgreSQL", "orders");
        datasource(a).put("datasourceId", 1);
        datasource(b).put("datasourceId", 999);
        datasource(b).put("ip", "DB.EXAMPLE");
        config(b).put("username", "other-user");
        config(b).put("password", "another-secret");
        assertEquals(resolve(a), resolve(b));
        String key = resolve(a).first();
        assertFalse(key.contains("secret"));
        assertFalse(key.contains("login-user"));
    }

    @Test
    public void resolvesAllSavedWriterShapesAndDeduplicates() {
        JSONObject a = writer("PostgreSQL", "public.orders");
        JSONObject split = new JSONObject();
        split.put("componentType", "37");
        JSONObject splitParams = new JSONObject();
        JSONArray targets = new JSONArray();
        targets.add(params(a));
        targets.add(params(writer("PostgreSQL", "public.customers")));
        splitParams.put("targetTables", targets);
        split.put("taskParams", splitParams);
        JSONObject pipeline = new JSONObject();
        pipeline.put("componentType", "100");
        JSONObject pipelineParams = new JSONObject();
        JSONObject stage = new JSONObject();
        stage.put("output", params(a));
        pipelineParams.put("pipelineNode", stage);
        pipeline.put("taskParams", pipelineParams);
        assertEquals(2, resolve(a, split, pipeline).size());
    }

    @Test
    public void postgresqlPreservesQuotedCaseAndNormalizesUnquotedNames() {
        assertEquals(resolve(writer("PostgreSQL", "PUBLIC.Orders")),
                resolve(writer("PostgreSQL", "\"public\".\"orders\"")));
        assertNotEquals(resolve(writer("PostgreSQL", "public.orders")),
                resolve(writer("PostgreSQL", "public.\"Orders\"")));
        JSONObject a = writer("PostgreSQL", "orders");
        config(a).put("schema", "\"Mixed.Schema\"");
        assertEquals(resolve(a), resolve(writer("PostgreSQL", "\"Mixed.Schema\".orders")));
    }

    @Test
    public void oracleNormalizesSchemaAndTableButPreservesQuotedCase() {
        JSONObject a = writer("Oracle11", "orders");
        config(a).put("schema", "sales");
        assertEquals(resolve(a), resolve(writer("Oracle", "SALES.ORDERS")));
        assertNotEquals(resolve(a), resolve(writer("Oracle", "SALES.\"orders\"")));
        expectFailure(writer("Oracle", "orders"));
    }

    @Test
    public void oracleAcceptsSidAndNeverUsesUsernameAsSchema() {
        JSONObject a = writer("Oracle", "SALES.ORDERS");
        config(a).remove("dbname");
        config(a).put("sid", "ORCL");
        assertEquals(1, resolve(a).size());
        a.getJSONObject("taskParams").put("target_table_name", "ORDERS");
        expectFailure(a);
    }

    @Test
    public void mysqlConservativelyFoldsCaseAndQualifiedDatabase() {
        assertEquals(resolve(writer("MySql", "Orders")),
                resolve(writer("MySql", "`warehouse`.`ORDERS`")));
        JSONObject a = writer("MySql", "orders");
        datasource(a).put("port", "03306");
        JSONObject b = writer("MySql", "orders");
        datasource(b).put("port", 3306);
        assertEquals(resolve(a), resolve(b));
    }

    @Test
    public void differentEndpointsDatabasesSchemasAndTablesStayDistinct() {
        JSONObject a = writer("PostgreSQL", "public.orders");
        JSONObject b = writer("PostgreSQL", "audit.orders");
        assertNotEquals(resolve(a), resolve(b));
        b = writer("PostgreSQL", "public.orders");
        datasource(b).put("ip", "elsewhere.example");
        assertNotEquals(resolve(a), resolve(b));
        b = writer("PostgreSQL", "public.orders");
        config(b).put("dbname", "other");
        assertNotEquals(resolve(a), resolve(b));
        b = writer("PostgreSQL", "public.orders");
        datasource(b).put("port", "5433");
        assertNotEquals(resolve(a), resolve(b));
    }

    @Test
    public void datasourceConfigCanBeAnEncodedJsonSnapshotAndAssetNameFallbackWorks() {
        JSONObject a = writer("PostgreSQL", "public.orders");
        JSONObject b = writer("PostgreSQL", "public.orders");
        datasource(b).put("datasourceConfig", JSON.toJSONString(config(b)));
        params(b).remove("target_table_name");
        params(b).put("target_asset_id", "public.orders");
        assertEquals(resolve(a), resolve(b));
    }

    @Test
    public void skipsPureLogicAndReadOnlyComponents() {
        JSONObject logic = new JSONObject();
        logic.put("componentType", "100");
        JSONObject params = new JSONObject();
        JSONObject stage = new JSONObject();
        stage.put("mode", "CONDITION");
        params.put("pipelineNode", stage);
        logic.put("taskParams", params);
        JSONObject reader = new JSONObject();
        reader.put("componentType", "1");
        assertTrue(resolve(logic, reader).isEmpty());
    }

    @Test
    public void rejectsIncompleteSnapshotsAndNonRelationalOutputs() {
        expectFailure(writer("Kafka", "orders"));
        JSONObject unknownOutput = new JSONObject();
        unknownOutput.put("componentType", "999");
        JSONObject outputParams = new JSONObject();
        outputParams.put("type", 2);
        unknownOutput.put("taskParams", outputParams);
        expectFailure(unknownOutput);
        JSONObject fileOutput = new JSONObject();
        fileOutput.put("componentType", "93");
        expectFailure(fileOutput);
        JSONObject a = writer("PostgreSQL", "orders");
        datasource(a).remove("ip");
        expectFailure(a);
        a = writer("PostgreSQL", "orders");
        config(a).remove("dbname");
        expectFailure(a);
        a = writer("PostgreSQL", "orders");
        datasource(a).put("port", "65536");
        expectFailure(a);
        expectFailure(writer("PostgreSQL", "\"unterminated"));
        expectFailure(writer("PostgreSQL", "a.b.c"));
        expectFailure(writer("PostgreSQL", "orders; DROP TABLE x"));
    }

    @Test
    public void malformedJsonDoesNotLeakCredentialsThroughException() {
        try {
            EtlWriteResourceResolver.resolve("[{\"password\":\"super-secret\"");
            fail("Expected ServiceException");
        } catch (ServiceException ex) {
            assertFalse(ex.getMessage().contains("super-secret"));
            assertNull(ex.getCause());
        }
    }

    @Test
    public void appendAndUpsertDoNotRequestTargetOwnership() {
        JSONObject append = writer("PostgreSQL", "orders"); params(append).put("writeModeType", 2);
        JSONObject upsert = writer("PostgreSQL", "orders"); params(upsert).put("writeModeType", 3);
        assertTrue(resolve(append, upsert).isEmpty());
        params(upsert).put("preSql", "TRUNCATE TABLE orders");
        assertEquals(1, resolve(upsert).size());
    }

    private static JSONObject writer(String type, String table) {
        JSONObject config = new JSONObject();
        config.put("dbname", "warehouse");
        config.put("username", "login-user");
        config.put("password", "secret");
        JSONObject datasource = new JSONObject();
        datasource.put("datasourceType", type);
        datasource.put("ip", "db.example");
        datasource.put("datasourceConfig", config);
        JSONObject params = new JSONObject();
        params.put("writerDatasource", datasource);
        params.put("target_table_name", table);
        params.put("writeModeType", 1);
        JSONObject node = new JSONObject();
        node.put("componentType", "91");
        node.put("taskParams", params);
        return node;
    }

    private static JSONObject params(JSONObject node) {
        return node.getJSONObject("taskParams");
    }

    private static JSONObject datasource(JSONObject node) {
        return params(node).getJSONObject("writerDatasource");
    }

    private static JSONObject config(JSONObject node) {
        return datasource(node).getJSONObject("datasourceConfig");
    }

    private static SortedSet<String> resolve(JSONObject... nodes) {
        return EtlWriteResourceResolver.resolve(JSON.toJSONString(nodes));
    }

    private static void expectFailure(JSONObject node) {
        try {
            resolve(node);
            fail("Expected ServiceException");
        } catch (ServiceException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
