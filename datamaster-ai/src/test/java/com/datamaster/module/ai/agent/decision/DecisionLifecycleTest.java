package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DecisionLifecycleTest {
    @Test public void noActionRecordsDecisionWithoutDispatchAndClearsPreview() {
        Proposal p = proposal(); DispositionRequest r = request("ACCEPT_NO_ACTION");
        Proposal next = DecisionLifecycle.apply(p, r, new Date());
        assertEquals("ACCEPTED_NO_ACTION", next.getStatus()); assertEquals("BASELINE", next.getSelectedCandidateId());
        assertNull(next.getBinding()); assertNull(next.getPreviewDigest()); assertNull(next.getImpactPreview());
        assertEquals(Integer.valueOf(3), next.getRevision()); assertEquals("PREVIEW_READY", p.getStatus());
    }
    @Test public void deferredRequiresFutureReviewAndCanReopenWithoutRefreshingEvidence() {
        Proposal p = proposal(); Date expiredEvidence = new Date(1000); p.setEvidenceExpiresAt(expiredEvidence);
        DispositionRequest r = request("DEFER"); reject(() -> DecisionLifecycle.apply(p, r, new Date()));
        r.setReviewAfter(new Date(System.currentTimeMillis() + 100000));
        Proposal deferred = DecisionLifecycle.apply(p, r, new Date()); assertEquals("DEFERRED", deferred.getStatus());
        Proposal reopened = DecisionLifecycle.apply(deferred, request("REOPEN"), new Date());
        assertEquals("PROPOSED", reopened.getStatus()); assertEquals(expiredEvidence, reopened.getEvidenceExpiresAt());
        assertNull(reopened.getPreviewExpiresAt());
    }
    @Test public void submittedOrInvalidStateCannotBecomeNoAction() {
        Proposal p = proposal(); p.setExecutionStatus("SUBMITTED");
        reject(() -> DecisionLifecycle.apply(p, request("ABANDON"), new Date()));
        p.setExecutionStatus(null); p.setStatus("ANALYZING");
        reject(() -> DecisionLifecycle.apply(p, request("ACCEPT_NO_ACTION"), new Date()));
    }
    private static Proposal proposal() { Proposal p = new Proposal(); p.setStatus("PREVIEW_READY"); p.setRevision(2); p.setBinding(new Binding()); p.setPreviewDigest("old"); Candidate c = new Candidate(); c.setId("BASELINE"); c.setBaseline(true); p.getCandidates().add(c); return p; }
    private static DispositionRequest request(String action) { DispositionRequest r = new DispositionRequest(); r.setDisposition(action); r.setReason("业务确认"); return r; }
    private static void reject(Runnable f) { try { f.run(); fail(); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); } }
}
