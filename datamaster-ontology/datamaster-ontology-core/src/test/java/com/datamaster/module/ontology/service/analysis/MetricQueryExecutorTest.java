package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Proxy-only tests: no Spring context, network, driver, database, or credentials. */
public class MetricQueryExecutorTest {
    @Test public void closesResourcesBoundsRowsAndPreservesDecimalPrecision() {
        Fixture f = new Fixture();
        MetricQueryPolicy policy = new MetricQueryPolicy(true, "40", "reader", 15, 1);
        MetricQueryExecutor executor = new MetricQueryExecutor(f.factory(), policy);
        MetricQueryReqVO req = MetricQueryBuilderTest.request(); req.setLimit(1);
        MetricRespVO snapshot = MetricQueryBuilderTest.snapshot();
        MetricQueryRespVO result = executor.execute(snapshot, MetricQueryBuilder.compile(snapshot, req), property());
        assertEquals(Boolean.TRUE, result.getTruncated()); assertEquals(1, result.getRows().size());
        assertEquals("9007199254740993.25", result.getRows().get(0).get("metric_value"));
        assertEquals(15, f.timeout.get()); assertEquals(2, f.maxRows.get());
        assertTrue(f.connectionClosed.get()); assertTrue(f.statementClosed.get()); assertTrue(f.resultClosed.get()); assertTrue(f.queryClosed.get());
        assertTrue(policy.tryAcquire()); policy.release();
    }
    @Test public void disabledAndUnapprovedAccountsNeverCreateExternalQuery() {
        Fixture f = new Fixture(); MetricRespVO snapshot = MetricQueryBuilderTest.snapshot();
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot, MetricQueryBuilderTest.request());
        MetricQueryBuilderTest.rejected(() -> new MetricQueryExecutor(f.factory(), new MetricQueryPolicy(false, "40", "reader", 15, 1)).execute(snapshot, plan, property()));
        MetricQueryBuilderTest.rejected(() -> new MetricQueryExecutor(f.factory(), new MetricQueryPolicy(true, "40", "other", 15, 1)).execute(snapshot, plan, property()));
        assertEquals(0, f.factoryCalls.get());
    }
    @Test public void unsupportedReadOnlyClosesConnectionAndReleasesPermitWithoutSql() {
        Fixture f = new Fixture(); f.failReadOnly = true;
        MetricQueryPolicy policy = new MetricQueryPolicy(true, "40", "reader", 15, 1);
        MetricQueryBuilderTest.rejected(() -> new MetricQueryExecutor(f.factory(), policy).execute(MetricQueryBuilderTest.snapshot(),
                MetricQueryBuilder.compile(MetricQueryBuilderTest.snapshot(), MetricQueryBuilderTest.request()), property()));
        assertTrue(f.connectionClosed.get()); assertTrue(f.queryClosed.get()); assertEquals(0, f.prepares.get());
        assertTrue(policy.tryAcquire()); policy.release();
    }
    @Test public void rawSqlExceptionIsNotReturnedAndAllResourcesAreClosed() {
        Fixture f = new Fixture(); f.failExecute = true;
        MetricQueryPolicy policy = new MetricQueryPolicy(true, "40", "reader", 15, 1);
        try {
            new MetricQueryExecutor(f.factory(), policy).execute(MetricQueryBuilderTest.snapshot(),
                    MetricQueryBuilder.compile(MetricQueryBuilderTest.snapshot(), MetricQueryBuilderTest.request()), property());
            fail("Expected rejection");
        } catch (ServiceException ex) { assertFalse(ex.getMessage().contains("SECRET")); }
        assertTrue(f.connectionClosed.get()); assertTrue(f.statementClosed.get()); assertTrue(f.queryClosed.get());
        assertTrue(policy.tryAcquire()); policy.release();
    }
    private static DbQueryProperty property() {
        return new DbQueryProperty("DORIS", "unused", "reader", null, 1, "ads", null);
    }
    private static class Fixture {
        final AtomicBoolean connectionClosed = new AtomicBoolean(), statementClosed = new AtomicBoolean(), resultClosed = new AtomicBoolean(), queryClosed = new AtomicBoolean();
        final AtomicInteger timeout = new AtomicInteger(), maxRows = new AtomicInteger(), cursor = new AtomicInteger(), prepares = new AtomicInteger(), factoryCalls = new AtomicInteger();
        boolean failReadOnly, failExecute;
        DataSourceFactory factory() {
            return property -> {
                factoryCalls.incrementAndGet();
                return proxy(DbQuery.class, (object, method, args) -> {
                    if ("getConnection".equals(method.getName())) return connection();
                    if ("close".equals(method.getName())) { queryClosed.set(true); return null; }
                    return defaultValue(method.getReturnType());
                });
            };
        }
        Connection connection() {
            return proxy(Connection.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "setReadOnly": if (failReadOnly) throw new SQLFeatureNotSupportedException("SECRET"); return null;
                    case "isReadOnly": return true;
                    case "prepareStatement": prepares.incrementAndGet(); return statement();
                    case "close": connectionClosed.set(true); return null;
                    default: return defaultValue(method.getReturnType());
                }
            });
        }
        PreparedStatement statement() {
            return proxy(PreparedStatement.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "setQueryTimeout": timeout.set((Integer) args[0]); return null;
                    case "setMaxRows": maxRows.set((Integer) args[0]); return null;
                    case "executeQuery": if (failExecute) throw new SQLException("SECRET SQL jdbc password"); return result();
                    case "close": statementClosed.set(true); return null;
                    default: return defaultValue(method.getReturnType());
                }
            });
        }
        ResultSet result() {
            return proxy(ResultSet.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "next": return cursor.incrementAndGet() <= 2;
                    case "getBigDecimal": return new BigDecimal("9007199254740993.25");
                    case "getMetaData": return proxy(ResultSetMetaData.class, (o, m, a) -> {
                        if ("getColumnCount".equals(m.getName())) return 1;
                        if ("getColumnLabel".equals(m.getName())) return "metric_value";
                        return defaultValue(m.getReturnType());
                    });
                    case "close": resultClosed.set(true); return null;
                    default: return defaultValue(method.getReturnType());
                }
            });
        }
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
