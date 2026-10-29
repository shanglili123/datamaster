package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class DecisionEvaluationServiceTest {
    @Test
    public void modelCannotCertifyConstraintOrInventComputedBenefit() {
        Candidate candidate = new Candidate();
        Constraint constraint = new Constraint(); constraint.setName("产能"); constraint.setStatus("PASS");
        candidate.getConstraints().add(constraint);
        Impact impact = new Impact(); impact.setValue("100000"); impact.setMethod("computed"); impact.setEvidenceId("forged");
        candidate.getImpacts().add(impact); candidate.getEvidenceIds().add("forged");
        new DecisionEvaluationService().evaluate(candidate, Collections.emptyList());
        assertEquals("UNKNOWN", constraint.getStatus());
        assertNull(impact.getValue()); assertNull(impact.getEvidenceId());
        assertEquals("unverified", impact.getMethod());
        assertTrue(candidate.getEvidenceIds().isEmpty());
        assertEquals("UNKNOWN", candidate.getEvaluation().getStatus());
    }

    @Test
    public void failedConstraintRemainsBlocked() {
        Candidate candidate = new Candidate();
        Constraint constraint = new Constraint(); constraint.setName("预算"); constraint.setStatus("FAIL");
        candidate.getConstraints().add(constraint);
        new DecisionEvaluationService().evaluate(candidate, Collections.emptyList());
        assertEquals("FAIL", candidate.getEvaluation().getStatus());
    }
}
