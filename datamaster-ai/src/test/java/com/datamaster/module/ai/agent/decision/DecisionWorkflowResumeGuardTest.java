package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.service.impl.WorkflowApprovalResumeGuard;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Pure readiness checks; no worker, database, approval or business execution. */
public class DecisionWorkflowResumeGuardTest {
    @Test public void completedApprovalHasStableTokenAndNextWaitHasDifferentToken() {
        ActionWorkflowRunDO run = run(); ActionWorkflowStepDO step = step(10L);
        ActionExecutionDO execution = execution(step);
        String token = WorkflowApprovalResumeGuard.token(run, Collections.singletonList(step), id -> execution);
        assertEquals(token, WorkflowApprovalResumeGuard.token(run, Collections.singletonList(step), id -> execution));
        ActionWorkflowStepDO next = step(11L);
        assertNotEquals(token, WorkflowApprovalResumeGuard.token(run, Collections.singletonList(next), id -> execution(next)));
    }
    @Test public void pendingRunningAndFailedActionsCannotBeResumed() {
        for (String status : Arrays.asList("PENDING_APPROVAL", "APPROVED", "RUNNING", "FAILED", "REJECTED", "RECONCILIATION_REQUIRED")) {
            ActionWorkflowStepDO step = step(10L); ActionExecutionDO execution = execution(step); execution.setStatus(status);
            try { WorkflowApprovalResumeGuard.token(run(), Collections.singletonList(step), id -> execution); fail(status); }
            catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
        }
    }
    @Test(expected = ServiceException.class) public void mismatchedExecutionLinkIsRejected() {
        ActionWorkflowStepDO step = step(10L); ActionExecutionDO execution = execution(step); execution.setWorkflowStepId(99L);
        WorkflowApprovalResumeGuard.token(run(), Collections.singletonList(step), id -> execution);
    }
    @Test(expected = ServiceException.class) public void failedRunIsNotAnApprovalContinuation() {
        ActionWorkflowRunDO run = run(); run.setStatus("FAILED");
        WorkflowApprovalResumeGuard.token(run, Collections.singletonList(step(10L)), id -> execution(step(10L)));
    }
    @Test(expected = ServiceException.class) public void mixedUnfinishedStepCannotBeResumed() {
        ActionWorkflowStepDO waiting = step(10L), unfinished = step(11L); unfinished.setNodeKey("other"); unfinished.setStatus("RUNNING");
        WorkflowApprovalResumeGuard.token(run(), Arrays.asList(waiting, unfinished), id -> execution(waiting));
    }
    private ActionWorkflowRunDO run() {
        ActionWorkflowRunDO r = new ActionWorkflowRunDO(); r.setId(1L); r.setCreatorId(7L); r.setStatus("WAITING_APPROVAL"); return r;
    }
    private ActionWorkflowStepDO step(Long id) {
        ActionWorkflowStepDO s = new ActionWorkflowStepDO(); s.setId(id); s.setRunId(1L); s.setNodeKey("approval");
        s.setActionId(9L); s.setExecutionId(id + 100); s.setStatus("WAITING_APPROVAL"); return s;
    }
    private ActionExecutionDO execution(ActionWorkflowStepDO step) {
        ActionExecutionDO e = new ActionExecutionDO(); e.setId(step.getExecutionId()); e.setActionId(step.getActionId());
        e.setWorkflowRunId(1L); e.setWorkflowStepId(step.getId()); e.setCreatorId(7L); e.setStatus("EXECUTED"); return e;
    }
}
