package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DecisionAmendmentTest {
    @Test public void amendmentCreatesRevisionAndInvalidatesEveryCalculationAndPreview() {
        Proposal original = proposal();
        Binding binding = DecisionAmendment.binding(request());
        Proposal result = DecisionAmendment.apply(original, "C1", binding);
        assertEquals(Integer.valueOf(4), result.getRevision());
        assertEquals("PROPOSED", result.getStatus());
        assertNull(result.getPreviewDigest()); assertNull(result.getBinding());
        assertNull(result.getSelectedCandidateId()); assertNull(result.getComparison());
        for (Candidate candidate : result.getCandidates()) {
            assertNull(candidate.getCalculation()); assertNull(candidate.getCalculationSpec());
        }
        assertEquals("2100228457732182017", result.getCandidates().get(0).getSuggestedBinding().getTargetId());
        assertEquals("ORD-1", result.getCandidates().get(0).getSuggestedBinding().getObjectKey());
        assertEquals("old-preview", original.getPreviewDigest());
        assertNotNull(original.getCandidates().get(0).getCalculation());
        assertEquals(original.getExpiresAt(), result.getExpiresAt());
    }
    @Test(expected = ServiceException.class) public void submittedProposalCannotBeAmended() {
        Proposal p = proposal(); p.setExecutionStatus("QUEUED");
        DecisionAmendment.apply(p, "C1", DecisionAmendment.binding(request()));
    }
    @Test(expected = ServiceException.class) public void baselineCannotBeAmended() {
        Proposal p = proposal(); p.getCandidates().get(0).setBaseline(true);
        DecisionAmendment.apply(p, "C1", DecisionAmendment.binding(request()));
    }
    @Test(expected = ServiceException.class) public void arrayIsNotExecutionInput() {
        AmendmentRequest r = request(); r.setInputParams("[]"); DecisionAmendment.binding(r);
    }
    @Test(expected = ServiceException.class) public void arbitraryBindingTypeIsRejected() {
        AmendmentRequest r = request(); r.setType("SQL"); DecisionAmendment.binding(r);
    }
    private AmendmentRequest request() {
        AmendmentRequest r = new AmendmentRequest(); r.setType("ACTION"); r.setTargetId("2100228457732182017");
        r.setObjectKey("ORD-1"); r.setInputParams("{\"quantity\":\"3\"}"); return r;
    }
    private Proposal proposal() {
        Proposal p = new Proposal(); p.setId("decision"); p.setRevision(3); p.setStatus("PREVIEW_READY");
        p.setExpiresAt(new Date(System.currentTimeMillis() + 60000)); p.setPreviewDigest("old-preview");
        p.setBinding(new Binding()); p.setSelectedCandidateId("C1"); p.setComparison(new Comparison());
        Candidate c = new Candidate(); c.setId("C1"); c.setCalculation(new CalculationResult()); c.setCalculationSpec(new CalculationSpec());
        p.setCandidates(new ArrayList<>(Collections.singletonList(c))); return p;
    }
}
