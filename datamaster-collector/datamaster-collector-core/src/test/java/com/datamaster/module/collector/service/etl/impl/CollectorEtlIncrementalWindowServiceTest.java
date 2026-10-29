package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.*;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/** Service logic only: mapper fakes do not simulate database locks or rollback. */
public class CollectorEtlIncrementalWindowServiceTest {
    private CollectorEtlIncrementalWindowService service;
    private final Map<Long, CollectorEtlNodeInstanceDO> records = new TreeMap<>();
    private Long owner;

    @Before public void setUp() throws Exception {
        service = new CollectorEtlIncrementalWindowService(); records.clear(); owner = 101L;
        inject("stageMapper", proxy(CollectorEtlStageRecordMapper.class, (method, args) -> {
            if ("lockTask".equals(method)) return 7L;
            if ("current".equals(method)) return records.get(args[2]);
            if ("latest".equals(method)) return records.isEmpty() ? null : records.get(Collections.max(records.keySet()));
            if ("insertWindow".equals(method)) {
                CollectorEtlNodeInstanceDO row = (CollectorEtlNodeInstanceDO) args[0];
                records.put(row.getTaskInstanceId(), row); return 1;
            }
            if ("finish".equals(method)) {
                for (CollectorEtlNodeInstanceDO row : records.values()) if (row.getId().equals(args[0])) {
                    if (!"PREPARED".equals(row.getIncrementalWindowStatus())) return 0;
                    row.setIncrementalWindowStatus((String) args[1]); row.setIncrementalSuccessfulValue((String) args[2]); return 1;
                }
                return 0;
            }
            throw new AssertionError(method);
        }));
        inject("nodeMapper", proxy(CollectorEtlNodeInstanceMapper.class, (method, args) -> {
            if ("findByParentCode".equals(method)) return null;
            throw new AssertionError(method);
        }));
        inject("instanceMapper", proxy(CollectorEtlTaskInstanceMapper.class, (method, args) -> {
            if ("selectById".equals(method)) return CollectorEtlTaskInstanceDO.builder().id((Long) args[0])
                    .taskId(7L).dsId(9000L + (Long) args[0]).spaceId(1L).spaceCode("space").taskType("1").build();
            throw new AssertionError(method);
        }));
        inject("runMapper", proxy(CollectorEtlRunMapper.class, (method, args) -> {
            if ("taskOwner".equals(method)) return owner;
            if ("lockById".equals(method)) {
                CollectorEtlRunDO run = new CollectorEtlRunDO(); run.setId((Long) args[0]); run.setTaskId(7L); run.setStatus("ACTIVE"); return run;
            }
            throw new AssertionError(method);
        }));
        inject("extService", proxy(ICollectorEtlTaskExtService.class, (method, args) -> {
            CollectorEtlTaskExtDO ext = new CollectorEtlTaskExtDO();
            ext.setFlinkxJobTemplateJson("{\"stages\":{\"stage\":{\"dsNodeCode\":\"100\"}}}"); return ext;
        }));
        inject("transactionManager", new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object value, TransactionDefinition definition) { }
            @Override protected void doCommit(DefaultTransactionStatus status) { }
            @Override protected void doRollback(DefaultTransactionStatus status) { }
        });
    }

    @Test public void sameOwnerRetryDoesNotRereadMax() {
        AtomicInteger calls = new AtomicInteger();
        service.prepare(7L,"stage",101L,"H",values("10"),types(),() -> { calls.incrementAndGet(); return values("20"); },old -> false);
        service.prepare(7L,"stage",101L,"H",values("10"),types(),() -> { throw new AssertionError(); },old -> false);
        assertEquals(1,calls.get()); assertEquals(1,records.size());
    }

    @Test public void failedWindowIsReplayedAndSuccessfulWatermarkIsOnStageRecord() {
        prepare(101L,"10","20"); service.finish(7L,"stage",101L,"H",false);
        owner = 102L;
        service.prepare(7L,"stage",102L,"H",values("10"),types(),() -> { throw new AssertionError(); },old -> true);
        assertEquals(records.get(101L).getId(),records.get(102L).getIncrementalReplayFromId());
        assertEquals(records.get(101L).getIncrementalEndValue(),records.get(102L).getIncrementalEndValue());
        service.finish(7L,"stage",102L,"H",true);
        assertEquals(values("20"),CollectorEtlIncrementalWindowService.parse(records.get(102L).getIncrementalSuccessfulValue()));
        owner = 103L; prepare(103L,"0","30");
        assertEquals(values("20"),CollectorEtlIncrementalWindowService.parse(records.get(103L).getIncrementalStartValue()));
    }

    @Test public void oldOwnerCannotCommitAfterTakeover() {
        prepare(101L,"10","20"); owner = 102L;
        rejected(() -> service.finish(7L,"stage",101L,"H",true));
        assertEquals("PREPARED",records.get(101L).getIncrementalWindowStatus());
    }

    @Test public void nullKeysSurviveEmptyWindowAndCommit() {
        prepare(101L,null,null); service.finish(7L,"stage",101L,"H",true);
        assertEquals(values(null),CollectorEtlIncrementalWindowService.parse(records.get(101L).getIncrementalSuccessfulValue()));
        assertTrue(CollectorEtlIncrementalWindowService.parse(records.get(101L).getIncrementalEndValue()).containsKey("src"));
    }

    @Test public void sourceMaximumRegressionDoesNotMoveWatermark() {
        prepare(101L,"10","100"); service.finish(7L,"stage",101L,"H",true);
        owner = 102L; prepare(102L,"0","99"); service.finish(7L,"stage",102L,"H",true);
        assertEquals(values("100"),CollectorEtlIncrementalWindowService.parse(records.get(102L).getIncrementalSuccessfulValue()));
        assertTrue(records.get(102L).getIncrementalWindowEmpty());
    }

    @Test public void configChangeAndUnconfirmedFailureCannotReuseWindow() {
        prepare(101L,"10","20"); owner = 102L;
        rejected(() -> service.prepare(7L,"stage",102L,"OTHER",values("10"),types(),() -> values("30"),old -> true));
        rejected(() -> service.prepare(7L,"stage",102L,"H",values("10"),types(),() -> values("30"),old -> false));
        assertEquals(1,records.size());
    }

    private void prepare(Long instance,String initial,String end) {
        service.prepare(7L,"stage",instance,"H",values(initial),types(),() -> values(end),old -> true);
    }
    private static Map<String,String> values(String value) { Map<String,String> map = new TreeMap<>(); map.put("src",value); return map; }
    private static Map<String,String> types() { return Collections.singletonMap("src","ID"); }
    private static void rejected(Runnable action) { try { action.run(); fail("Expected rejection"); } catch (ServiceException expected) { } }
    private void inject(String name,Object value) throws Exception { Field f = CollectorEtlIncrementalWindowService.class.getDeclaredField(name); f.setAccessible(true); f.set(service,value); }
    private interface Call { Object invoke(String method,Object[] args); }
    private static <T> T proxy(Class<T> type,Call call) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a) -> call.invoke(m.getName(),a))); }
}
