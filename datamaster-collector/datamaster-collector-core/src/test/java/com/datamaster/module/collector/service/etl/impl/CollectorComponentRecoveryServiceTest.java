package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.*;
import com.datamaster.module.collector.service.etl.*;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

/** Event lifecycle unit logic, not a substitute for transactional/DS integration tests. */
public class CollectorComponentRecoveryServiceTest {
    private CollectorComponentRecoveryService service;
    private CollectorEtlTaskOpsEventDO event;
    private int inserted;
    private int changed;
    private int scheduled;
    private long lastDelayMillis;
    @Before public void setUp() throws Exception {
        service = new CollectorComponentRecoveryService(); event = null; inserted = 0; changed = 0; scheduled = 0; lastDelayMillis = -1;
        inject("events", proxy(CollectorEtlTaskOpsEventMapper.class, (name,args) -> {
            if ("latestComponent".equals(name) || "selectById".equals(name)) return event;
            if ("insert".equals(name)) { event = (CollectorEtlTaskOpsEventDO) args[0]; event.setId(701L); inserted++; return 1; }
            if ("transition".equals(name)) { changed++; return 1; }
            throw new AssertionError(name);
        }));
        inject("transactionManager", new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition definition) { }
            @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) { }
            @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) { }
        });
        inject("executor", proxy(java.util.concurrent.ScheduledExecutorService.class, (name,args) -> {
            if ("execute".equals(name)) { ((Runnable) args[0]).run(); return null; }
            if ("schedule".equals(name)) {
                scheduled++;
                lastDelayMillis = (Long) args[1];
                return proxy(java.util.concurrent.ScheduledFuture.class, (operation,values) -> {
                if ("cancel".equals(operation)) return true;
                if ("isDone".equals(operation) || "isCancelled".equals(operation)) return false;
                return null;
                });
            }
            throw new AssertionError(name);
        }));
        inject("instances", proxy(CollectorEtlTaskInstanceMapper.class, (name,args) -> CollectorEtlTaskInstanceDO.builder()
                .id((Long) args[0]).taskId(7L).taskCode("900").taskType("1").status("6").dsId(101L).build()));
        inject("policies", proxy(ICollectorEtlTaskOpsPolicyService.class, (name,args) -> {
            CollectorEtlTaskOpsPolicyDO policy = new CollectorEtlTaskOpsPolicyDO(); policy.setAiManaged(true); return policy;
        }));
        inject("extensions", proxy(ICollectorEtlTaskExtService.class, (name,args) -> {
            CollectorEtlTaskExtDO ext = new CollectorEtlTaskExtDO(); ext.setEtlTaskCode("900"); ext.setEtlTaskVersion(1); return ext;
        }));
    }
    @Test public void diagnosisIsScheduledOnlyAfterCommit() {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.finished(run(501L), "FAILURE");
            assertEquals(0, scheduled);
            for (org.springframework.transaction.support.TransactionSynchronization callback : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()) callback.afterCommit();
            assertEquals(1, scheduled);
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test public void rollbackDoesNotScheduleDiagnosis() {
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.finished(run(501L), "FAILURE");
            for (org.springframework.transaction.support.TransactionSynchronization callback : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()) callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
            assertEquals(0, scheduled);
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test public void waitingSchedulesOnlyItsDueEventWithoutScanningTable() throws Exception {
        service.finished(run(501L), "FAILURE");
        event.setActionStatus("WAITING");
        JSONObject data = JSONObject.parseObject(event.getAiRawResult());
        data.getJSONObject("recovery").put("nextRetryAt", System.currentTimeMillis() + 60000L);
        event.setAiRawResult(data.toJSONString());
        java.lang.reflect.Method schedule = CollectorComponentRecoveryService.class.getDeclaredMethod("scheduleEvent", Long.class);
        schedule.setAccessible(true); schedule.invoke(service, event.getId());
        assertEquals(2, scheduled); assertTrue(lastDelayMillis > 0 && lastDelayMillis <= 60000L);
    }
    @Test public void runningStateDoesNotSchedulePeriodicChecks() throws Exception {
        service.finished(run(501L), "FAILURE"); event.setActionStatus("RUNNING");
        java.lang.reflect.Method schedule = CollectorComponentRecoveryService.class.getDeclaredMethod("scheduleEvent", Long.class);
        schedule.setAccessible(true); schedule.invoke(service, event.getId());
        assertEquals(1, scheduled);
    }
    @Test public void finalResultSchedulesNoticeInsteadOfWaitingForOldDeadline() throws Exception {
        service.finished(run(501L), "FAILURE"); event.setActionStatus("STARTING");
        JSONObject data = JSONObject.parseObject(event.getAiRawResult());
        data.getJSONObject("recovery").put("submittedAt", System.currentTimeMillis());
        event.setAiRawResult(data.toJSONString());
        java.lang.reflect.Method schedule = CollectorComponentRecoveryService.class.getDeclaredMethod("scheduleEvent", Long.class);
        schedule.setAccessible(true); schedule.invoke(service, event.getId());
        assertTrue(lastDelayMillis > 590000L);
        service.admitted(run(502L), 1); service.finished(run(502L), "SUCCESS");
        assertEquals(3, scheduled); assertEquals(0L, lastDelayMillis);
    }
    @Test public void duplicateFailureDoesNotRegisterTwice() {
        service.finished(run(501L),"FAILURE"); service.finished(run(501L),"FAILURE");
        assertEquals(1,inserted); assertEquals("PENDING_ANALYSIS",event.getActionStatus());
    }
    @Test public void admittedNewLocalExecutionIsNotAlreadyRecoverySuccess() {
        service.finished(run(501L),"FAILURE"); event.setActionStatus("WAITING");
        service.admitted(run(502L),1);
        assertEquals("RUNNING",event.getActionStatus());
        assertEquals(Long.valueOf(502),CollectorComponentRecoveryService.recovery(event).getLong("executionId"));
        service.finished(run(502L),"SUCCESS");
        assertEquals("SUCCESS",event.getActionStatus());
        assertTrue(CollectorComponentRecoveryService.recovery(event).getBooleanValue("notificationPending"));
    }
    @Test public void anotherFailureKeepsIncidentAttemptBudget() {
        service.finished(run(501L),"FAILURE"); event.setActionStatus("STARTING");
        JSONObject data = JSONObject.parseObject(event.getAiRawResult()); data.getJSONObject("recovery").put("attempt",2);
        event.setAiRawResult(data.toJSONString()); service.admitted(run(502L),1); service.finished(run(502L),"FAILURE");
        assertEquals(1,inserted); assertEquals("PENDING_ANALYSIS",event.getActionStatus());
        assertEquals(2,CollectorComponentRecoveryService.recovery(event).getIntValue("attempt"));
        assertEquals(Long.valueOf(502),CollectorComponentRecoveryService.recovery(event).getLong("failedExecutionId"));
    }
    @Test public void manualStopCancelsRatherThanRestarts() {
        service.finished(run(501L),"FAILURE"); service.admitted(run(502L),1); service.finished(run(502L),"STOP");
        assertEquals("CANCELLED",event.getActionStatus());
    }
    @Test public void versionChangeCannotAdoptNewRun() {
        service.finished(run(501L),"FAILURE"); service.admitted(run(502L),2);
        assertEquals("CANCELLED",event.getActionStatus());
        assertNull(CollectorComponentRecoveryService.recovery(event).getLong("executionId"));
    }
    @Test public void successEndsBudgetAndNextFailureCreatesNewIncident() {
        service.finished(run(501L),"FAILURE"); service.admitted(run(502L),1); service.finished(run(502L),"SUCCESS");
        service.finished(run(503L),"FAILURE");
        assertEquals(2,inserted); assertEquals(0,CollectorComponentRecoveryService.recovery(event).getIntValue("attempt"));
    }
    @Test public void pendingRecoveryCanBeCancelledWithoutSubmitting() {
        service.finished(run(501L),"FAILURE"); event.setActionStatus("WAITING");
        service.cancelPending(7L,"用户下线");
        assertEquals("CANCELLED",event.getActionStatus());
        service.admitted(run(502L),1);
        assertNull(CollectorComponentRecoveryService.recovery(event).getLong("executionId"));
    }
    @Test public void staleOriginalFailureDoesNotResetRunningAttempt() {
        service.finished(run(501L),"FAILURE"); service.admitted(run(502L),1);
        int before=changed; service.finished(run(501L),"FAILURE");
        assertEquals(before,changed); assertEquals(1,inserted);
        assertEquals("RUNNING",event.getActionStatus());
    }
    @Test public void ordinaryStopDoesNotCreateRecoveryIncident() {
        service.finished(run(501L),"STOP");
        assertEquals(0,inserted); assertNull(event);
    }
    private CollectorEtlRunDO run(Long id) { CollectorEtlRunDO run=new CollectorEtlRunDO();run.setId(id);run.setTaskId(7L);run.setWorkflowCode(900L);return run; }
    private void inject(String field,Object value) throws Exception { Field f=CollectorComponentRecoveryService.class.getDeclaredField(field);f.setAccessible(true);f.set(service,value); }
    private interface Call { Object invoke(String name,Object[] args); }
    private <T> T proxy(Class<T> type,Call call) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a) -> call.invoke(m.getName(),a))); }
}
