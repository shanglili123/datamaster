package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.*;
import com.datamaster.metadata.dal.mapper.lineage.LineageMapper;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Source-only focused state-machine tests. No database or Spring context needed. */
public class JdbcLineageServiceTest {
    @Test public void emptyCompleteClearsButFailureKeepsLastgoodAndDeleteNeverResurrects() throws Exception {
        Harness harness = new Harness();
        harness.extraction("COMPLETE", relation());
        assertEquals("APPLIED", harness.service.sync(snapshot(1, "UPSERT")).getStatus());
        assertEquals(1, harness.edges.size());
        harness.extraction("FAILED");
        harness.service.sync(snapshot(2, "UPSERT"));
        assertEquals(1, harness.edges.size());
        assertTrue(harness.producer.getStale());
        assertEquals(Long.valueOf(1), harness.producer.getLastgoodSequence());
        harness.extraction("COMPLETE");
        harness.service.sync(snapshot(3, "UPSERT"));
        assertTrue(harness.edges.isEmpty());
        assertFalse(harness.producer.getStale());
        harness.service.sync(snapshot(4, "DELETE"));
        harness.extraction("COMPLETE", relation());
        assertEquals("IGNORED", harness.service.sync(snapshot(5, "ACTIVE")).getStatus());
        assertTrue(harness.edges.isEmpty());
    }

    @Test public void partialReplacesProvedOnlyDuplicateSequenceDoesNotReapply() throws Exception {
        Harness harness = new Harness();
        harness.extraction("COMPLETE", relation());
        harness.service.sync(snapshot(1, "UPSERT"));
        harness.extraction("PARTIAL");
        harness.service.sync(snapshot(2, "UPSERT"));
        assertTrue(harness.edges.isEmpty());
        assertEquals("PARTIAL", harness.producer.getStatus());
        harness.extraction("COMPLETE", relation());
        assertEquals("IGNORED", harness.service.sync(snapshot(2, "UPSERT")).getStatus());
        assertTrue(harness.edges.isEmpty());
        assertEquals(2, harness.versions);
    }

    @Test public void inactiveRetainsFactsButDefaultReadsFilterAndFailureStaleSurvives() throws Exception {
        Harness harness = new Harness();
        harness.extraction("COMPLETE", relation()); harness.service.sync(snapshot(1, "UPSERT"));
        harness.extraction("FAILED"); harness.service.sync(snapshot(2, "UPSERT"));
        harness.service.sync(snapshot(3, "INACTIVE"));
        assertFalse(harness.producer.getActive()); assertTrue(harness.producer.getStale());
        assertEquals(1, harness.edges.size());
    }

    private static LineageSnapshotDTO snapshot(long sequence, String event) {
        LineageSnapshotDTO snapshot = new LineageSnapshotDTO(); snapshot.setTaskId(1L); snapshot.setSpaceId(2L);
        snapshot.setSpaceCode("space"); snapshot.setSequence(sequence); snapshot.setEventType(event);
        snapshot.setCaptureStatus("COMPLETE"); snapshot.setUnits(Collections.emptyList()); return snapshot;
    }
    private static LineageRelationDTO relation() {
        LineageRelationDTO relation = new LineageRelationDTO();
        LineageEndpointDTO source = new LineageEndpointDTO(); source.setDatasourceId(1L); source.setTable("a");
        LineageEndpointDTO target = new LineageEndpointDTO(); target.setDatasourceId(1L); target.setTable("b");
        relation.setSource(source); relation.setTarget(target); relation.setUnitKey("unit"); relation.setRouteKey("route");
        relation.setKind("TABLE"); relation.setConfidence("EXACT"); return relation;
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static final class Harness implements InvocationHandler {
        final JdbcLineageService service = new JdbcLineageService();
        final List<EdgeRow> edges = new ArrayList<>();
        ProducerRow producer; LineageExtractionDTO extraction; int versions;
        Harness() throws Exception {
            inject(service, "enabled", true); inject(service, "identity", new LineageIdentity());
            inject(service, "mapper", Proxy.newProxyInstance(LineageMapper.class.getClassLoader(), new Class<?>[]{LineageMapper.class}, this));
            inject(service, "extractor", (LineageExtractor) snapshot -> extraction);
        }
        void extraction(String status, LineageRelationDTO... relations) {
            extraction = new LineageExtractionDTO(); extraction.setStatus(status);
            extraction.setRelations(Arrays.asList(relations));
        }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "ensureProducer":
                    if (producer == null) { producer = (ProducerRow) args[0]; producer.setSequence(-1L); producer.setDeleted(false); }
                    return 1;
                case "lockProducer": return producer;
                case "clearEdges": edges.clear(); return 1;
                case "insertEdge": edges.add((EdgeRow) args[0]); return 1;
                case "insertVersion": versions++; return 1;
                case "updateProducer": case "insertObject": return 1;
                default: throw new AssertionError("Unexpected mapper call " + method.getName());
            }
        }
    }
}
