package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LineageIdentityTest {
    private final LineageIdentity identity = new LineageIdentity();

    @Test public void structuredIdentityRespectsQuotedCaseAndDatasource() {
        LineageEndpointDTO a = endpoint(7L, "Orders", false);
        LineageEndpointDTO b = endpoint(7L, "orders", false);
        assertEquals(identity.objectId(a), identity.objectId(b));
        a.setTableQuoted(true);
        assertNotEquals(identity.objectId(a), identity.objectId(b));
        b.setDatasourceId(8L);
        assertNotEquals(identity.objectId(a), identity.objectId(b));
    }

    @Test public void routeAndColumnIdsCannotBeConfusedWithMetadataOrAssetIds() {
        LineageEndpointDTO table = endpoint(7L, "orders", false);
        LineageColumnDTO column = new LineageColumnDTO(); column.setName("id");
        assertTrue(identity.objectId(table).startsWith("obj:"));
        assertTrue(identity.columnId(table, column).startsWith("col:"));
        assertNotEquals(identity.routeId(1L, "a", "bc"), identity.routeId(1L, "ab", "c"));
        assertNotEquals(identity.routeId(1L, "unit", "writer1"), identity.routeId(1L, "unit", "writer2"));
    }

    private static LineageEndpointDTO endpoint(Long datasource, String name, boolean quoted) {
        LineageEndpointDTO endpoint = new LineageEndpointDTO();
        endpoint.setKind("TABLE"); endpoint.setDatasourceId(datasource); endpoint.setTable(name);
        endpoint.setCaseRule("LOWER"); endpoint.setTableQuoted(quoted); return endpoint;
    }
}
