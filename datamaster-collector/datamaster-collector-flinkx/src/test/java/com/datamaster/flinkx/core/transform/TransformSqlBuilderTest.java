package com.datamaster.flinkx.core.transform;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.flinkx.core.FlinkxEtlTaskConverter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TransformSqlBuilderTest {

    @Test
    public void appliesNullSafeBranchFilterAndWriterProjection() {
        JSONObject metadata = new JSONObject(); metadata.put("enabled", true); metadata.put("sourceTableValue", "a1");
        JSONArray transitions = new JSONArray();
        JSONObject filter = new JSONObject(); filter.put("componentType", "ROW_FILTER");
        JSONObject condition = new JSONObject(); condition.put("condition", "COALESCE((amount > 10), FALSE)"); filter.put("parameter", condition); transitions.add(filter);
        JSONObject projection = new JSONObject(); projection.put("componentType", "OUTPUT_PROJECTION");
        JSONObject columns = new JSONObject(); columns.put("columns", Arrays.asList("amount", "id", "source_table", "write_time"));
        projection.put("parameter", columns); transitions.add(projection);
        String sql = TransformSqlBuilder.build(transitions, Arrays.asList("id", "amount", "unused"), "sourceTable", metadata);
        Assert.assertTrue(sql.startsWith("SELECT `amount`, `id`,"));
        Assert.assertTrue(sql.contains("WHERE (COALESCE((amount > 10), FALSE))"));
        Assert.assertFalse(sql.contains("`unused`"));
        Assert.assertTrue(sql.contains("CURRENT_TIMESTAMP"));
    }

    @Test
    public void shouldApplyFinalProjectionAfterGeneratingMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.put("enabled", true);
        JSONObject projection = new JSONObject();
        projection.put("componentType", "OUTPUT_PROJECTION");
        JSONObject parameter = new JSONObject();
        parameter.put("columns", Arrays.asList("amount", "id", "source_table", "write_time"));
        projection.put("parameter", parameter);
        JSONArray transitions = new JSONArray();
        transitions.add(projection);

        String sql = TransformSqlBuilder.build(transitions,
                Arrays.asList("source_table", "id", "unused", "amount", "write_time"), "sourceTable", metadata);

        Assert.assertTrue(sql.startsWith("SELECT `amount`, `id`, `source_table`, CAST(CURRENT_TIMESTAMP AS TIMESTAMP(3)) AS `write_time`"));
        Assert.assertFalse(sql.contains("`unused`"));
    }

    @Test
    public void shouldApplyProjectionToCustomSql() {
        String sql = TransformSqlBuilder.projectCustomSql("SELECT id, amount, unused FROM sourceTable;",
                Arrays.asList("amount", "id"));

        Assert.assertEquals("SELECT `amount`, `id` FROM (SELECT id, amount, unused FROM sourceTable) AS __dm_output__", sql);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingMappedSourceColumn() {
        JSONObject projection = new JSONObject();
        projection.put("componentType", "OUTPUT_PROJECTION");
        JSONObject parameter = new JSONObject();
        parameter.put("columns", Collections.singletonList("missing"));
        projection.put("parameter", parameter);
        JSONArray transitions = new JSONArray();
        transitions.add(projection);

        TransformSqlBuilder.build(transitions, Arrays.asList("id", "amount"), "sourceTable");
    }

    @Test
    public void shouldAppendRequiredAndOptionalSourceMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.put("enabled", true);
        metadata.put("includeNaturalKey", true);
        metadata.put("naturalKeyColumns", Arrays.asList("ID"));
        metadata.put("includeRowHash", true);
        metadata.put("sourceTableValue", "crm.customer");

        String sql = TransformSqlBuilder.build(new JSONArray(),
                Arrays.asList("ID", "NAME"), "sourceTable", metadata);

        Assert.assertTrue(sql.contains("`source_table`"));
        Assert.assertTrue(sql.contains("CURRENT_TIMESTAMP"));
        Assert.assertTrue(sql.contains("MD5(CONCAT_WS('|', CAST('crm.customer' AS STRING)"));
        Assert.assertTrue(sql.contains("`source_row_hash`"));
        Assert.assertTrue(sql.contains("COALESCE(CAST(`ID` AS STRING), '')"));
        Assert.assertFalse("row hash must not include governance columns",
                sql.contains("COALESCE(CAST(`source_table` AS STRING), '')"));
    }

    @Test
    public void shouldPreserveSourceTableWhenWrappingUnionSql() {
        JSONObject metadata = new JSONObject();
        metadata.put("enabled", true);
        metadata.put("includeNaturalKey", true);
        metadata.put("naturalKeyColumns", Arrays.asList("ID"));

        String sql = TransformSqlBuilder.wrapCustomSql(
                "SELECT ID, NAME, source_table FROM ods_a UNION ALL SELECT ID, NAME, source_table FROM ods_b",
                Arrays.asList("ID", "NAME", "source_table"), "sourceTable", metadata);

        Assert.assertTrue(sql.contains("FROM (SELECT ID, NAME, source_table FROM ods_a UNION ALL"));
        Assert.assertTrue(sql.contains("__dm_custom__.`source_table`"));
        Assert.assertTrue(sql.contains("CAST(CURRENT_TIMESTAMP AS TIMESTAMP(3)) AS `write_time`"));
    }

    @Test
    public void shouldExcludePreservedCustomGovernanceColumnFromRowHash() {
        JSONObject metadata = new JSONObject();
        metadata.put("enabled", true);
        metadata.put("includeRowHash", true);
        metadata.put("preservedMetadataColumns", Arrays.asList("origin_key"));

        String sql = TransformSqlBuilder.build(new JSONArray(),
                Arrays.asList("ID", "NAME", "origin_key"), "sourceTable", metadata);

        Assert.assertTrue(sql.contains("`origin_key`"));
        Assert.assertFalse(sql.contains("COALESCE(CAST(`origin_key` AS STRING), '')"));
    }

    @Test
    public void shouldBuildFlinkxJobForUnionPipelineOutput() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", true);
        metadata.put("preserveExistingSource", true);
        metadata.put("preservedMetadataColumns", Arrays.asList("source_table", "write_time"));

        Map<String, Object> readerParameter = new LinkedHashMap<>();
        readerParameter.put("dbType", "MYSQL");
        readerParameter.put("username", "user");
        readerParameter.put("password", "password");
        readerParameter.put("column", Arrays.asList("ID", "NAME", "source_table", "write_time"));
        readerParameter.put("sourceMetadataColumns", Arrays.asList("ID", "NAME", "source_table", "write_time"));
        readerParameter.put("sourceMetadata", metadata);
        Map<String, Object> connection = new LinkedHashMap<>();
        connection.put("jdbcUrl", "jdbc:mysql://127.0.0.1:3306/ods");
        connection.put("querySql", "SELECT ID, NAME, source_table, write_time FROM ods_a UNION ALL SELECT ID, NAME, source_table, write_time FROM ods_b");
        readerParameter.put("connection", connection);
        Map<String, Object> reader = new LinkedHashMap<>();
        reader.put("componentType", "38");
        reader.put("parameter", readerParameter);

        Map<String, Object> writerParameter = new LinkedHashMap<>();
        writerParameter.put("dbType", "MYSQL");
        writerParameter.put("preSql", new String[]{"UPDATE audit SET a=1,b=2"});
        writerParameter.put("postSql", "UPDATE audit SET note='a;b'; SELECT 1,2;");
        writerParameter.put("username", "user");
        writerParameter.put("password", "password");
        writerParameter.put("column", Arrays.asList("ID", "NAME", "source_table", "write_time"));
        writerParameter.put("target_column", Arrays.asList("ID", "NAME", "source_table", "write_time"));
        Map<String, Object> writerConnection = new LinkedHashMap<>();
        writerConnection.put("jdbcUrl", "jdbc:mysql://127.0.0.1:3306/dwd");
        writerConnection.put("table", Collections.singletonList("dwd_c"));
        writerParameter.put("connection", writerConnection);
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("componentType", "91");
        writer.put("parameter", writerParameter);

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("reader", reader);
        args.put("writer", writer);
        args.put("transition", Collections.emptyList());
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("taskInfo", Collections.singletonMap("name", "C_DWD"));
        args.put("config", config);

        String jobJson = FlinkxEtlTaskConverter.convertToFlinkxJobJson(args);

        Assert.assertTrue(jobJson.contains("customSql"));
        Assert.assertTrue(jobJson.contains("source_table"));
        Assert.assertTrue(jobJson.contains("CURRENT_TIMESTAMP"));
        Assert.assertTrue(jobJson.contains("ods_a UNION ALL SELECT"));
        JSONObject parameter = JSONObject.parseObject(jobJson).getJSONObject("job")
                .getJSONArray("content").getJSONObject(0).getJSONObject("writer")
                .getJSONObject("parameter");
        Assert.assertEquals("UPDATE audit SET a=1,b=2", parameter.getJSONArray("preSql").getString(0));
        Assert.assertEquals(2, parameter.getJSONArray("postSql").size());
        Assert.assertEquals("UPDATE audit SET note='a;b'", parameter.getJSONArray("postSql").getString(0));
    }
}
