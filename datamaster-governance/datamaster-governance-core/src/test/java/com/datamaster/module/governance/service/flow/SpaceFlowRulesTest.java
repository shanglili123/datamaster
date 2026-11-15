package com.datamaster.module.governance.service.flow;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SpaceFlowRulesTest {
    @Test public void acceptsLightAndStrictChains() {
        SpaceFlowRules.validate(chain("METADATA"));
        SpaceFlowRules.validate(chain("METADATA", "ASSET"));
        SpaceFlowRules.validate(chain("METADATA", "QUALITY"));
        SpaceFlowRules.validate(chain("METADATA", "ASSET", "QUALITY"));
    }
    @Test public void rejectsBypassCycleDisconnectedAndWarehouseLayers() {
        Definition bypass = chain("METADATA", "ASSET", "QUALITY"); bypass.getEdges().add(edge("METADATA", "API")); reject(bypass);
        Definition cycle = chain("METADATA", "ASSET"); cycle.getEdges().add(edge("ASSET", "METADATA")); reject(cycle);
        Definition disconnected = chain("METADATA"); disconnected.getNodes().add("QUALITY"); reject(disconnected);
        Definition layer = chain("METADATA"); layer.getNodes().add("ADS"); reject(layer);
    }
    @Test public void rejectsMissingConsumerOrDuplicateEdges() {
        Definition missing = chain("METADATA"); missing.getNodes().remove("ANALYSIS"); reject(missing);
        Definition duplicate = chain("METADATA"); duplicate.getEdges().add(edge("METADATA", "API")); reject(duplicate);
    }
    @Test public void rejectsNullNodesWithBusinessValidation() {
        Definition invalid = chain("METADATA"); invalid.getNodes().add(null); reject(invalid);
    }
    @Test public void publicationDiffReportsAddedAndRemovedRequirements() {
        Map<String,Object> first = SpaceFlowRules.changes(null, chain("METADATA"));
        assertEquals(true, first.get("firstPublication")); assertEquals(true, first.get("definitionChanged"));
        Map<String,Object> tightened = SpaceFlowRules.changes(chain("METADATA"), chain("METADATA", "ASSET", "QUALITY"));
        assertEquals(List.of("ASSET", "QUALITY"), tightened.get("addedRequirements"));
        assertEquals(List.of(), tightened.get("removedRequirements"));
        Map<String,Object> relaxed = SpaceFlowRules.changes(chain("METADATA", "ASSET", "QUALITY"), chain("METADATA", "ASSET"));
        assertEquals(List.of("QUALITY"), relaxed.get("removedRequirements"));
    }
    @Test public void publicationDiffIgnoresListOrderingButDetectsGateOrderChanges() {
        Definition before = chain("METADATA", "ASSET", "QUALITY");
        Definition reordered = chain("METADATA", "ASSET", "QUALITY");
        Collections.reverse(reordered.getNodes()); Collections.reverse(reordered.getEdges());
        assertEquals(false, SpaceFlowRules.changes(before, reordered).get("definitionChanged"));
        Map<String,Object> changed = SpaceFlowRules.changes(before, chain("METADATA", "QUALITY", "ASSET"));
        assertEquals(true, changed.get("definitionChanged"));
        assertEquals(List.of(), changed.get("addedRequirements"));
        assertEquals(List.of(), changed.get("removedRequirements"));
        assertEquals(List.of("ASSET → ANALYSIS", "ASSET → API", "METADATA → QUALITY", "QUALITY → ASSET"), changed.get("addedEdges"));
        assertEquals(List.of("ASSET → QUALITY", "METADATA → ASSET", "QUALITY → ANALYSIS", "QUALITY → API"), changed.get("removedEdges"));
    }
    @Test public void validatesBoundedLayoutWithoutTreatingItAsPolicyChange() {
        Definition draft = chain("METADATA", "ASSET"); Position position = new Position(); position.setX(20D); position.setY(50D);
        draft.getLayout().put("ASSET", position); SpaceFlowRules.validate(draft);
        assertEquals(false, SpaceFlowRules.changes(chain("METADATA", "ASSET"), draft).get("definitionChanged"));
        position.setX(Double.NaN); reject(draft); position.setX(700D); reject(draft);
        position.setX(20D); draft.getLayout().put("UNKNOWN", position); reject(draft);
    }
    private static Definition chain(String... names) {
        Definition d = new Definition(); d.setNodes(new ArrayList<>(Arrays.asList(names)));
        d.getNodes().addAll(List.of("API", "ANALYSIS"));
        for (int i=1;i<names.length;i++) d.getEdges().add(edge(names[i-1], names[i]));
        d.getEdges().add(edge(names[names.length-1], "API")); d.getEdges().add(edge(names[names.length-1], "ANALYSIS")); return d;
    }
    private static Edge edge(String from, String to) { Edge e = new Edge(); e.setFrom(from); e.setTo(to); return e; }
    private static void reject(Definition d) { try { SpaceFlowRules.validate(d); fail("Must reject invalid capability graph"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); } }
}
