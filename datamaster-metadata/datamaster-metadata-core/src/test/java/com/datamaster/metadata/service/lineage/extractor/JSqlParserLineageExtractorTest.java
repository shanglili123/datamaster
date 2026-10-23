package com.datamaster.metadata.service.lineage.extractor;

import com.datamaster.metadata.api.lineage.dto.*;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/** Focused offline contract tests. No Spring, database, collector or external queries. */
public class JSqlParserLineageExtractorTest {
    private final JSqlParserLineageExtractor extractor = new JSqlParserLineageExtractor();

    @Test
    public void joinsFunctionsAliasesAndPredicatesHaveOnlyValueColumnSources() {
        LineageUnitDTO unit = sql("INSERT INTO result (total, label, fixed) "
                + "SELECT a.price + b.tax, upper(a.name), 'secret-value' "
                + "FROM orders a JOIN rates b ON a.rate_id = b.id WHERE a.hidden = 9");
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.price>result.total", "rates.tax>result.total", "orders.name>result.label"), fields(result));
        assertEquals(set("orders>result", "rates>result"), tables(result));
        assertNull(result.getErrorCode());
    }

    @Test
    public void ctesAndNestedSubqueriesResolveOutputAliasesAndUnionBranchesByPosition() {
        LineageUnitDTO unit = sql("WITH first_cte(x) AS (SELECT id FROM first_source), "
                + "second_cte AS (SELECT x AS renamed FROM first_cte) "
                + "INSERT INTO result (id) SELECT d.renamed FROM (SELECT renamed FROM second_cte) d "
                + "UNION ALL SELECT other_id FROM second_source");
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("first_source.id>result.id", "second_source.other_id>result.id"), fields(result));
        assertEquals(set("first_source>result", "second_source>result"), tables(result));
    }

    @Test
    public void readerQueryTransformAndWriterComposeWithoutPersistingVirtualSource() {
        LineageUnitDTO unit = route();
        unit.setReaderSql("SELECT physical_id AS input_id, name AS input_name FROM orders");
        unit.setTransformSql("SELECT upper(input_name) AS label, input_id + 1 AS final_id, 'fixed' AS constant_value FROM sourceTable");
        unit.setTargetColumns(columns("label", "identifier", "constant_value"));
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.name>writer.label", "orders.physical_id>writer.identifier"), fields(result));
        assertEquals(set("orders>writer"), tables(result));
        assertFalse(result.getRelations().stream().anyMatch(r -> "sourceTable".equals(r.getSource().getTable())));
    }

    @Test
    public void generatedSourceMetadataAndLiteralColumnsHaveNoPhysicalSourceEdges() {
        LineageUnitDTO unit = route();
        unit.setSourceColumns(columns("id", "created_at"));
        unit.setGeneratedColumns(columns("created_at"));
        unit.setTransformSql("SELECT id, created_at, 42 AS fixed FROM sourceTable");
        unit.setTargetColumns(columns("id", "created_at", "fixed"));
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.id>writer.id"), fields(result));
    }

    @Test
    public void reliableRouteColumnsCanExpandVirtualStarButPhysicalSqlStarCannotBeGuessed() {
        LineageUnitDTO unit = route();
        unit.setSourceColumns(columns("id", "name"));
        unit.setTargetColumns(columns("id", "name"));
        unit.setTransformSql("SELECT * FROM sourceTable");
        LineageExtractionDTO reliable = extract(unit);
        assertEquals("COMPLETE", reliable.getStatus());
        assertEquals(set("orders.id>writer.id", "orders.name>writer.name"), fields(reliable));

        unit.setReaderSql("SELECT * FROM orders");
        LineageExtractionDTO unknown = extract(unit);
        assertEquals("PARTIAL", unknown.getStatus());
        assertTrue(unknown.getErrorCode().contains("UNKNOWN_STAR"));
        assertTrue(fields(unknown).isEmpty());
        assertEquals(set("orders>writer"), tables(unknown));
    }

    @Test
    public void noReliableOrdinalMeansNoPositionalWriterMapping() {
        LineageUnitDTO unit = route();
        unit.setReaderSql("SELECT id, name FROM orders");
        unit.setTargetColumns(columns("id", "name"));
        unit.setColumnOrderReliable(false);
        LineageExtractionDTO result = extract(unit);
        assertEquals("PARTIAL", result.getStatus());
        assertTrue(result.getErrorCode().contains("UNRELIABLE_COLUMN_ORDER"));
        assertTrue(fields(result).isEmpty());
        assertEquals(set("orders>writer"), tables(result));
    }

    @Test
    public void explicitPhysicalMappingDoesNotNeedOrdinalOrTreatAliasesAsPhysicalNames() {
        LineageUnitDTO unit = route();
        unit.setColumnOrderReliable(false);
        LineageMappingDTO mapping = new LineageMappingDTO();
        mapping.setSource(unit.getSources().get(0));
        mapping.setSourceColumn(column("physical_id"));
        mapping.setTargetColumn(column("writer_id"));
        unit.setMappings(Collections.singletonList(mapping));
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.physical_id>writer.writer_id"), fields(result));
    }

    @Test
    public void ambiguousJoinColumnsRetainTableEvidenceButNeverGuessAField() {
        LineageExtractionDTO result = extract(sql("INSERT INTO result (id) "
                + "SELECT id FROM first_source a JOIN second_source b ON a.id=b.id"));
        assertEquals("PARTIAL", result.getStatus());
        assertTrue(result.getErrorCode().contains("AMBIGUOUS_COLUMN"));
        assertTrue(fields(result).isEmpty());
        assertEquals(set("first_source>result", "second_source>result"), tables(result));
    }

    @Test
    public void ctasDerivesNamesButInsertWithoutExplicitColumnsDoesNot() {
        LineageExtractionDTO ctas = extract(sql("CREATE TABLE result AS SELECT id AS identifier, 1 AS constant_value FROM orders"));
        assertEquals("COMPLETE", ctas.getStatus());
        assertEquals(set("orders.id>result.identifier"), fields(ctas));
        LineageExtractionDTO insert = extract(sql("INSERT INTO result SELECT id FROM orders"));
        assertEquals("PARTIAL", insert.getStatus());
        assertTrue(insert.getErrorCode().contains("EXPLICIT_TARGET_COLUMNS_REQUIRED"));
        assertTrue(fields(insert).isEmpty());
        assertEquals(set("orders>result"), tables(insert));
    }

    @Test
    public void simpleSingleTargetUpdateUsesOldRhsColumnsAndNotWhereColumns() {
        LineageExtractionDTO result = extract(sql("UPDATE orders SET total=price+tax, label=upper(name), fixed=1 WHERE private_flag=1"));
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.price>orders.total", "orders.tax>orders.total", "orders.name>orders.label"), fields(result));
        assertEquals(set("orders>orders"), tables(result));
    }

    @Test
    public void singleTargetScalarUpdateSubqueryResolvesItsOwnScope() {
        LineageExtractionDTO result = extract(sql("UPDATE orders SET total=(SELECT max(amount) FROM payments) WHERE flag=1"));
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("payments.amount>orders.total"), fields(result));
        assertEquals(set("payments>orders"), tables(result));
    }

    @Test
    public void generatedMappingsCannotCreatePhysicalSourceColumnEdges() {
        LineageUnitDTO unit = route();
        unit.setGeneratedColumns(columns("load_time"));
        LineageMappingDTO mapping = new LineageMappingDTO();
        mapping.setSource(unit.getSources().get(0));
        mapping.setSourceColumn(column("load_time"));
        mapping.setTargetColumn(column("loaded_at"));
        unit.setMappings(Collections.singletonList(mapping));
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertTrue(fields(result).isEmpty());
        assertEquals(set("orders>writer"), tables(result));
    }

    @Test
    public void quoteCaseCatalogSchemaDatasourceAndRouteIdentityArePreserved() {
        LineageUnitDTO unit = sql("INSERT INTO \"Data\".\"Mixed\" (\"Output\") SELECT s.\"Input\" FROM \"Data\".\"Source\" s");
        unit.getContext().setSchema("Data");
        unit.getContext().setSchemaQuoted(true);
        unit.setUnitKey("unit-one");
        unit.setRouteKey("route-two");
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        LineageRelationDTO field = result.getRelations().stream().filter(r -> r.getSourceColumn() != null).findFirst().get();
        assertEquals(Long.valueOf(7L), field.getSource().getDatasourceId());
        assertEquals("catalog", field.getSource().getCatalog());
        assertEquals("Data", field.getSource().getSchema());
        assertEquals(Boolean.TRUE, field.getSource().getSchemaQuoted());
        assertEquals("Source", field.getSource().getTable());
        assertEquals(Boolean.TRUE, field.getSource().getTableQuoted());
        assertEquals("Input", field.getSourceColumn().getName());
        assertEquals(Boolean.TRUE, field.getSourceColumn().getQuoted());
        assertEquals("unit-one", field.getUnitKey());
        assertEquals("route-two", field.getRouteKey());
        assertEquals("EXACT", field.getConfidence());
    }

    @Test
    public void catalogOnlyTwoPartReaderUsesExactStructuredSourceNotDuplicatedSchema() {
        LineageUnitDTO unit = route();
        LineageEndpointDTO source = unit.getSources().get(0);
        source.setCatalog("warehouse");
        source.setSchema(null);
        unit.setReaderSql("SELECT warehouse.orders.id FROM warehouse.orders");
        unit.setTargetColumns(columns("id"));
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.id>writer.id"), fields(result));
        for (LineageRelationDTO relation : result.getRelations()) {
            assertEquals(source, relation.getSource());
            assertEquals("warehouse", relation.getSource().getCatalog());
            assertNull(relation.getSource().getSchema());
        }
    }

    @Test
    public void knownCatalogContextResolvesNewSqlTablesWithoutInventingSchema() {
        LineageUnitDTO unit = sql("INSERT INTO warehouse.result (id) SELECT id FROM warehouse.orders");
        unit.getContext().setCatalog("warehouse");
        unit.getContext().setSchema(null);
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.id>result.id"), fields(result));
        for (LineageRelationDTO relation : result.getRelations()) {
            assertEquals("warehouse", relation.getSource().getCatalog());
            assertNull(relation.getSource().getSchema());
            assertEquals("warehouse", relation.getTarget().getCatalog());
            assertNull(relation.getTarget().getSchema());
        }
    }

    @Test
    public void knownSchemaContextKeepsPostgresTwoPartNamesAsSchema() {
        LineageExtractionDTO result = extract(sql("INSERT INTO public.result (id) SELECT id FROM public.orders"));
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.id>result.id"), fields(result));
        for (LineageRelationDTO relation : result.getRelations()) {
            assertEquals("catalog", relation.getSource().getCatalog());
            assertEquals("public", relation.getSource().getSchema());
            assertEquals("catalog", relation.getTarget().getCatalog());
            assertEquals("public", relation.getTarget().getSchema());
        }
    }

    @Test
    public void competingCatalogAndSchemaSourceIdentitiesArePartialWithoutGuessedEdges() {
        LineageUnitDTO unit = sql("INSERT INTO result (id) SELECT id FROM warehouse.orders");
        LineageEndpointDTO catalogSource = endpoint("orders");
        catalogSource.setCatalog("warehouse");
        catalogSource.setSchema(null);
        LineageEndpointDTO schemaSource = endpoint("orders");
        schemaSource.setSchema("warehouse");
        unit.setSources(Arrays.asList(catalogSource, schemaSource));
        // SQL task has no physical reader route; each SQL reference must bind on its own.
        unit.setReaderSql(null);
        LineageExtractionDTO result = extract(unit);
        assertEquals("PARTIAL", result.getStatus());
        assertTrue(result.getErrorCode().contains("AMBIGUOUS_TABLE_NAMESPACE"));
        assertTrue(result.getRelations().isEmpty());
    }

    @Test
    public void sameDatasourceSourceCanConfirmCatalogNamespaceForAnotherTable() {
        LineageUnitDTO unit = sql("INSERT INTO result (id) SELECT id FROM warehouse.orders");
        LineageEndpointDTO source = endpoint("another_table");
        source.setCatalog("warehouse");
        source.setSchema(null);
        unit.setSources(Collections.singletonList(source));
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.id>result.id"), fields(result));
        for (LineageRelationDTO relation : result.getRelations()) {
            assertEquals("warehouse", relation.getSource().getCatalog());
            assertNull(relation.getSource().getSchema());
        }
    }

    @Test
    public void contextAndSourceNamespaceDisagreementIsPartial() {
        LineageUnitDTO unit = sql("INSERT INTO result (id) SELECT id FROM warehouse.orders");
        unit.getContext().setSchema("warehouse");
        LineageEndpointDTO source = endpoint("another_table");
        source.setCatalog("warehouse");
        source.setSchema(null);
        unit.setSources(Collections.singletonList(source));
        LineageExtractionDTO result = extract(unit);
        assertEquals("PARTIAL", result.getStatus());
        assertEquals("AMBIGUOUS_TABLE_NAMESPACE", result.getErrorCode());
        assertTrue(result.getRelations().isEmpty());
    }

    @Test
    public void unknownTwoPartNamespaceDoesNotAssumeSchemaOrCatalog() {
        LineageExtractionDTO result = extract(sql("INSERT INTO result (id) SELECT id FROM unknown_db.orders"));
        assertEquals("PARTIAL", result.getStatus());
        assertEquals("UNKNOWN_TWO_PART_NAMESPACE", result.getErrorCode());
        assertTrue(result.getRelations().isEmpty());
    }

    @Test
    public void quotedCatalogPrefixPreservesCaseAndDoesNotBecomeSchema() {
        LineageUnitDTO unit = sql("INSERT INTO \"Warehouse\".result (id) SELECT id FROM \"Warehouse\".orders");
        unit.getContext().setCatalog("Warehouse");
        unit.getContext().setCatalogQuoted(true);
        unit.getContext().setSchema(null);
        LineageExtractionDTO result = extract(unit);
        assertEquals("COMPLETE", result.getStatus());
        for (LineageRelationDTO relation : result.getRelations()) {
            assertEquals("Warehouse", relation.getSource().getCatalog());
            assertEquals(Boolean.TRUE, relation.getSource().getCatalogQuoted());
            assertNull(relation.getSource().getSchema());
            assertEquals("Warehouse", relation.getTarget().getCatalog());
            assertEquals(Boolean.TRUE, relation.getTarget().getCatalogQuoted());
            assertNull(relation.getTarget().getSchema());
        }
    }

    @Test
    public void explicitThreePartNameStillCarriesBothCatalogAndSchema() {
        LineageExtractionDTO result = extract(sql("INSERT INTO db.public.result (id) SELECT id FROM db.public.orders"));
        assertEquals("COMPLETE", result.getStatus());
        assertEquals(set("orders.id>result.id"), fields(result));
        for (LineageRelationDTO relation : result.getRelations()) {
            assertEquals("db", relation.getSource().getCatalog());
            assertEquals("public", relation.getSource().getSchema());
            assertEquals("db", relation.getTarget().getCatalog());
            assertEquals("public", relation.getTarget().getSchema());
        }
    }

    @Test
    public void predicateSubqueryAddsTableEvidenceButNoValueColumnSource() {
        LineageExtractionDTO result = extract(sql("INSERT INTO result (id) SELECT a.id FROM orders a "
                + "WHERE EXISTS (SELECT 1 FROM blocked b WHERE b.id=a.id)"));
        assertEquals(set("orders.id>result.id"), fields(result));
        assertEquals(set("orders>result", "blocked>result"), tables(result));
    }

    @Test
    public void nestedAliasScopeDoesNotLeakIntoOuterQuery() {
        LineageExtractionDTO result = extract(sql("INSERT INTO result (id) "
                + "SELECT hidden.id FROM (SELECT id FROM orders hidden) visible"));
        assertEquals("PARTIAL", result.getStatus());
        assertTrue(result.getErrorCode().contains("UNRESOLVED_QUALIFIER"));
        assertTrue(fields(result).isEmpty());
        assertEquals(set("orders>result"), tables(result));
    }

    @Test
    public void unsupportedAnalyticsAndDistinctUnionDoNotPretendComplete() {
        LineageExtractionDTO window = extract(sql("INSERT INTO result (id) SELECT row_number() OVER (ORDER BY id) FROM orders"));
        assertEquals("PARTIAL", window.getStatus());
        assertTrue(window.getErrorCode().contains("UNSUPPORTED_EXPRESSION"));
        assertTrue(fields(window).isEmpty());
        LineageExtractionDTO union = extract(sql("INSERT INTO result (id) SELECT id FROM a UNION SELECT id FROM b"));
        assertEquals("PARTIAL", union.getStatus());
        assertTrue(union.getErrorCode().contains("UNSUPPORTED_SET_OPERATION"));
        assertTrue(fields(union).isEmpty());
    }

    @Test
    public void recursiveDynamicMalformedAndMultiTargetStatementsNeverFallbackToTextMatching() {
        assertFailure("WITH RECURSIVE c AS (SELECT id FROM orders UNION ALL SELECT id FROM c) INSERT INTO result (id) SELECT id FROM c", "RECURSIVE_CTE");
        assertFailure("INSERT INTO result (id) SELECT id FROM ${table}", "DYNAMIC_SQL");
        assertFailure("INSERT INTO result (id) SELECT id FROM", "PARSE_FAILED");
        assertFailure("UPDATE a JOIN b ON a.id=b.id SET a.id=b.id", "UNSUPPORTED_UPDATE");
    }

    @Test
    public void adHocReadOnlyAndBareDdlHaveNoPersistableTarget() {
        LineageExtractionDTO select = extract(sql("SELECT id FROM orders"));
        assertEquals("COMPLETE", select.getStatus());
        assertTrue(select.getRelations().isEmpty());
        LineageExtractionDTO ddl = extract(sql("CREATE TABLE result (id INT)"));
        assertEquals("COMPLETE", ddl.getStatus());
        assertTrue(ddl.getRelations().isEmpty());
    }

    @Test
    public void failureInOneRouteDoesNotContaminateOtherRoutesAndDiagnosticsNeverContainSql() {
        LineageUnitDTO valid = sql("INSERT INTO result (id) SELECT id FROM orders");
        LineageUnitDTO invalid = sql("INSERT INTO result (id) SELECT 'sensitive-token' FROM ${table}");
        LineageSnapshotDTO snapshot = snapshot(valid, invalid);
        LineageExtractionDTO result = extractor.extract(snapshot);
        assertEquals("PARTIAL", result.getStatus());
        assertEquals(set("orders.id>result.id"), fields(result));
        assertEquals("DYNAMIC_SQL", result.getErrorCode());
        assertFalse(result.getErrorCode().contains("sensitive-token"));
    }

    @Test
    public void failedCaptureIsNotReportedAsSuccessfulEmptyExtraction() {
        LineageSnapshotDTO snapshot = snapshot();
        snapshot.setCaptureStatus("FAILED");
        LineageExtractionDTO result = extractor.extract(snapshot);
        assertEquals("FAILED", result.getStatus());
        assertEquals("CAPTURE_INCOMPLETE", result.getErrorCode());
    }

    private void assertFailure(String sql, String code) {
        LineageExtractionDTO result = extract(sql(sql));
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getErrorCode(), result.getErrorCode().contains(code));
        assertTrue(result.getRelations().isEmpty());
    }

    private LineageExtractionDTO extract(LineageUnitDTO unit) { return extractor.extract(snapshot(unit)); }

    private static LineageSnapshotDTO snapshot(LineageUnitDTO... units) {
        LineageSnapshotDTO snapshot = new LineageSnapshotDTO();
        snapshot.setTaskId(1L);
        snapshot.setCaptureStatus("COMPLETE");
        snapshot.setUnits(Arrays.asList(units));
        return snapshot;
    }

    private static LineageUnitDTO sql(String sql) {
        LineageUnitDTO unit = new LineageUnitDTO();
        unit.setContext(endpoint("context_table"));
        unit.setTransformSql(sql);
        unit.setUnitKey("unit");
        unit.setRouteKey("route");
        return unit;
    }

    private static LineageUnitDTO route() {
        LineageUnitDTO unit = new LineageUnitDTO();
        unit.setSources(Collections.singletonList(endpoint("orders")));
        unit.setTarget(endpoint("writer"));
        unit.setContext(endpoint("context_table"));
        unit.setUnitKey("unit");
        unit.setRouteKey("route");
        unit.setColumnOrderReliable(true);
        return unit;
    }

    private static LineageEndpointDTO endpoint(String name) {
        LineageEndpointDTO endpoint = new LineageEndpointDTO();
        endpoint.setDatasourceId(7L);
        endpoint.setKind("TABLE");
        endpoint.setCatalog("catalog");
        endpoint.setSchema("public");
        endpoint.setTable(name);
        endpoint.setCaseRule("LOWER");
        return endpoint;
    }

    private static LineageColumnDTO column(String name) {
        LineageColumnDTO column = new LineageColumnDTO();
        column.setName(name);
        column.setQuoted(false);
        return column;
    }

    private static List<LineageColumnDTO> columns(String... names) {
        return Arrays.stream(names).map(JSqlParserLineageExtractorTest::column).collect(Collectors.toList());
    }

    private static Set<String> fields(LineageExtractionDTO result) {
        return result.getRelations().stream().filter(r -> r.getSourceColumn() != null)
                .map(r -> r.getSource().getTable() + "." + r.getSourceColumn().getName() + ">"
                        + r.getTarget().getTable() + "." + r.getTargetColumn().getName()).collect(Collectors.toSet());
    }

    private static Set<String> tables(LineageExtractionDTO result) {
        return result.getRelations().stream().filter(r -> r.getSourceColumn() == null)
                .map(r -> r.getSource().getTable() + ">" + r.getTarget().getTable()).collect(Collectors.toSet());
    }

    private static Set<String> set(String... values) { return new HashSet<>(Arrays.asList(values)); }
}
