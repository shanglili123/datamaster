package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.*;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.mapper.lineage.LineageMapper;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class LineageGraphQueryTest {
    @Test public void unauthorizedBridgeIsNeitherReturnedNorTraversed() throws Exception {
        Harness harness = new Harness(); harness.denied.add(harness.b.getId());
        LineageGraphDTO graph = harness.query.query(harness.request(5));
        assertEquals(1, graph.getNodes().size()); assertTrue(graph.getEdges().isEmpty());
        assertEquals(1, harness.adjacentCalls); assertEquals(graph.getCenterId(), graph.getNodes().get(0).getId());
    }

    @Test public void tableTaskTableCountsOneHopAndOpaqueIdsRemainDistinct() throws Exception {
        Harness harness = new Harness();
        LineageGraphDTO graph = harness.query.query(harness.request(1));
        assertEquals(3, graph.getNodes().size()); assertEquals(2, graph.getEdges().size());
        assertEquals(1, harness.adjacentCalls); assertEquals("OK", graph.getStatus());
        Set<String> ids = new HashSet<>(); for (LineageNodeDTO node : graph.getNodes()) assertTrue(ids.add(node.getId()));
        for (LineageEdgeDTO edge : graph.getEdges()) { assertTrue(ids.contains(edge.getSource())); assertTrue(ids.contains(edge.getTarget())); }
    }

    @Test public void visibleTablesDoNotGrantHiddenFieldEndpoints() throws Exception {
        Harness harness = new Harness(); harness.fieldFacts = true;
        LineageQueryReqDTO request = harness.request(1); request.setGranularity("FIELD");
        LineageGraphDTO graph = harness.query.query(request);
        assertEquals("OK", graph.getStatus()); assertEquals(3, graph.getNodes().size());
        assertEquals(2, graph.getEdges().size());
        assertTrue(graph.getDiagnostics().contains("FIELD_VISIBILITY_UNRESOLVED"));
        for (LineageNodeDTO node : graph.getNodes()) assertNotEquals("COLUMN", node.getType());
        harness.columnsAllowed = true;
        graph = harness.query.query(request);
        assertEquals(5, graph.getNodes().size()); assertEquals(3, graph.getEdges().size());
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static final class Harness implements InvocationHandler {
        final LineageIdentity identity = new LineageIdentity();
        final LineageGraphQuery query = new LineageGraphQuery();
        final ObjectRow a = object("a"), b = object("b"), c = object("c");
        final ObjectRow ac = column(a), bc = column(b);
        final Set<String> denied = new HashSet<>(); int adjacentCalls;
        boolean fieldFacts, columnsAllowed;
        Harness() throws Exception {
            inject(query, "identity", identity);
            inject(query, "mapper", Proxy.newProxyInstance(LineageMapper.class.getClassLoader(), new Class<?>[]{LineageMapper.class}, this));
            inject(query, "guard", new LineageGuard() {
                @Override public Visible visible(ObjectRow object, LineageQueryReqDTO request, boolean assetMode) {
                    if (object == null || denied.contains(object.getId())) return null;
                    CatalogTableDO table = new CatalogTableDO(); table.setId((long)object.getObjectName().charAt(0));
                    table.setTableName(object.getObjectName()); table.setDatasourceId(1L); table.setSpaceId(2L);
                    return new Visible(table, null);
                }
                @Override public boolean columnVisible(ObjectRow column, Visible parent, LineageQueryReqDTO request, boolean assetMode) {
                    return columnsAllowed;
                }
            });
        }
        ObjectRow object(String name) {
            LineageEndpointDTO endpoint = new LineageEndpointDTO(); endpoint.setDatasourceId(1L); endpoint.setTable(name);
            ObjectRow row = new ObjectRow(); row.setId(identity.objectId(endpoint)); row.setKind("TABLE");
            row.setDatasourceId(1L); row.setObjectName(name); return row;
        }
        ObjectRow column(ObjectRow parent) {
            ObjectRow row = new ObjectRow(); row.setId("col:" + LineageIdentity.hash(parent.getId())); row.setKind("COLUMN");
            row.setDatasourceId(1L); row.setParentId(parent.getId()); row.setColumnName("secret"); return row;
        }
        LineageQueryReqDTO request(int depth) {
            LineageQueryReqDTO request = new LineageQueryReqDTO(); request.setObjectId(a.getId()); request.setSpaceId(2L);
            request.setSpaceCode("space"); request.setDirection("DOWNSTREAM"); request.setDepth(depth); return request;
        }
        EdgeRow fact(ObjectRow from, ObjectRow to) {
            EdgeRow row = new EdgeRow(); row.setId(from.getId() + to.getId()); row.setSourceId(from.getId()); row.setTargetId(to.getId());
            row.setTaskId(1L); row.setKind("TABLE"); row.setConfidence("EXACT"); row.setUnitKey("u"); row.setRouteKey("r");
            row.setStatus("COMPLETE"); row.setStale(false);
            if (fieldFacts) { row.setSourceColumnId(ac.getId()); row.setTargetColumnId(bc.getId()); row.setKind("FIELD"); }
            return row;
        }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "object": return a;
                case "objects": return Arrays.asList(a,b,c,ac,bc);
                case "diagnostics": return Collections.emptyList();
                case "adjacent":
                    adjacentCalls++;
                    Collection<?> frontier = (Collection<?>)args[0];
                    if (frontier.contains(a.getId())) return Collections.singletonList(fact(a,b));
                    if (frontier.contains(b.getId())) return Collections.singletonList(fact(b,c));
                    return Collections.emptyList();
                default: throw new AssertionError(method.getName());
            }
        }
    }
}
