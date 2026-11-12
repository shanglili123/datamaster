package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IActionApprovalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import static org.junit.Assert.*;

/** JUnit4 + JDK proxies only. No Spring, database, network, or new dependencies. */
public class ActionImpactPreviewTest {
    @Test public void countsWithoutDmlAndClosesEveryResource() throws Exception {
        Fixture f = new Fixture();
        f.rows = 3;
        ActionImpactPreviewVO result = f.service.previewImpact(f.request());
        assertEquals("AVAILABLE", result.getStatus());
        assertEquals("MATCHED_ROWS", result.getSummaryType());
        assertEquals(Long.valueOf(3), result.getRowCount());
        assertFalse(result.isTruncated());
        assertEquals(5, f.timeout);
        assertEquals(1001, f.maxRows);
        assertTrue(f.readOnly);
        assertTrue(f.sql.startsWith("SELECT 1 FROM orders WHERE "));
        assertFalse(f.sql.contains(" SET "));
        assertTrue(f.connectionClosed && f.statementClosed && f.resultClosed && f.queryClosed);
        assertEquals(2, f.permissionChecks);
        String json = new ObjectMapper().writeValueAsString(result);
        assertFalse(json.contains("orders"));
        assertFalse(json.contains("SECRET"));
        assertFalse(json.contains("generatedSql"));
    }

    @Test public void cappedSummaryIsAnHonestLowerBoundAndDigestDrifts() throws Exception {
        Fixture f = new Fixture(); f.rows = 1005;
        ActionImpactPreviewVO capped = f.service.previewImpact(f.request());
        assertEquals(Long.valueOf(1000), capped.getRowCount());
        assertTrue(capped.isTruncated());
        assertEquals(1001, f.cursor);
        assertEquals(capped.getDigest(), f.service.previewImpact(f.request()).getDigest());
        f.rows = 999;
        assertNotEquals(capped.getDigest(), f.service.previewImpact(f.request()).getDigest());
    }

    @Test public void createIsOnlyPlannedInsertNotAffectedRows() throws Exception {
        Fixture f = new Fixture(); f.action.setActionType("CREATE");
        ActionImpactPreviewVO result = f.service.previewImpact(f.request());
        assertEquals("PLANNED_INSERT", result.getSummaryType());
        assertEquals("INSERT_INTENT_NOT_EXECUTION", result.getReason());
        assertEquals(Long.valueOf(1), result.getRowCount());
        assertEquals(0, f.connections);
    }

    @Test public void deleteReusesWhereAndIgnoresKeywordsInsideLiterals() throws Exception {
        Fixture f = new Fixture(); f.action.setActionType("DELETE");
        ExecutionSubmitReqVO req = f.request();
        req.setInputParams("{\"id\":\"literal WHERE ; -- ' text\"}");
        assertEquals("AVAILABLE", f.service.previewImpact(req).getStatus());
        assertTrue(f.sql.contains("literal WHERE ; -- '' text"));
        assertFalse(f.sql.startsWith("DELETE"));
    }

    @Test public void unsupportedTypesNeverResolveOrExecuteTheirCode() throws Exception {
        for (String type : Arrays.asList("FUNCTION", "WEBHOOK", "COMPOSITE", "WORKFLOW", "SELECT")) {
            Fixture f = new Fixture(); f.action.setActionType(type);
            // Deliberately malformed executable config must not even be prepared.
            f.action.setExecutionSteps("SECRET invalid executable config");
            ActionImpactPreviewVO result = f.service.previewImpact(f.request());
            assertEquals("UNKNOWN", result.getStatus());
            assertEquals("NOT_EVALUATED", result.getPreconditionStatus());
            assertNull(result.getRowCount());
            assertEquals(0, f.connections);
            assertEquals(0, f.criteriaChecks);
        }
    }

    @Test public void versionAndPermissionDenialsAreNotSwallowed() throws Exception {
        Fixture f = new Fixture(); ExecutionSubmitReqVO req = f.request(); req.setExpectedVersion(2);
        try { f.service.previewImpact(req); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(0, f.permissionChecks);
        f.deny = true;
        try { f.service.previewImpact(f.request()); fail(); } catch (SecurityException expected) { }
        assertEquals(0, f.connections);
        assertEquals(0, f.criteriaChecks);
    }

    @Test public void failedPreconditionsBlockWithoutLeakingDetail() throws Exception {
        Fixture f = new Fixture(); f.criteria = "{\"passed\":false,\"detail\":\"SECRET\"}";
        ActionImpactPreviewVO result = f.service.previewImpact(f.request());
        assertEquals("BLOCKED", result.getStatus());
        assertEquals("REJECT", result.getPreconditionStatus());
        assertEquals(0, f.connections);
        assertFalse(new ObjectMapper().writeValueAsString(result).contains("SECRET"));
        f.criteria = "malformed";
        assertEquals("BLOCKED", f.service.previewImpact(f.request()).getStatus());
    }

    @Test public void unsafeAndUnboundedDefinitionsDoNotReadSource() throws Exception {
        Fixture f = new Fixture();
        f.action.setParamConfig("[{\"propertyCode\":\"name\",\"valueMode\":\"expression\",\"valueTemplate\":\"danger()\"}]");
        assertEquals("UNKNOWN", f.service.previewImpact(f.request()).getStatus());
        f.action.setParamConfig("[{\"propertyCode\":\"id\",\"condition\":true,\"valueMode\":\"placeholder\",\"valueTemplate\":\"${id}\"}]");
        assertEquals("UNBOUNDED_OBJECT_RESOLUTION", f.service.previewImpact(f.request()).getReason());
        f.action.setParamConfig(null);
        f.action.setSubmissionCriteria("{\"conditions\":[{\"field\":\"object.stock\"}]}");
        assertEquals("UNKNOWN", f.service.previewImpact(f.request()).getStatus());
        assertEquals(0, f.connections);
    }

    @Test public void unsupportedReadOnlyStopsBeforeAnySql() throws Exception {
        Fixture f = new Fixture(); f.failReadOnly = true;
        assertEquals("UNKNOWN", f.service.previewImpact(f.request()).getStatus());
        assertNull(f.sql);
        assertTrue(f.connectionClosed && f.queryClosed);
    }

    @Test public void explicitParameterConditionsReuseConfiguration() throws Exception {
        Fixture f = new Fixture();
        f.action.setParamConfig("[{\"propertyCode\":\"id\",\"condition\":true,\"conditionValueSource\":\"PARAM\",\"valueMode\":\"placeholder\",\"valueTemplate\":\"${id}\"}]");
        assertEquals("AVAILABLE", f.service.previewImpact(f.request()).getStatus());
        assertTrue(f.sql.contains("id = '1'"));
        ExecutionSubmitReqVO workflow = f.request(); workflow.setWorkflowRunId(9L);
        assertEquals("UNKNOWN", f.service.previewImpact(workflow).getStatus());
    }

    @Test public void sourceFailureIsUnknownNotZeroAndDoesNotLeakDriverError() throws Exception {
        Fixture f = new Fixture(); f.failQuery = true;
        ActionImpactPreviewVO result = f.service.previewImpact(f.request());
        assertEquals("UNKNOWN", result.getStatus());
        assertEquals("READ_UNAVAILABLE_OR_TIMEOUT", result.getReason());
        assertNull(result.getRowCount());
        assertFalse(new ObjectMapper().writeValueAsString(result).contains("SECRET"));
        assertTrue(f.connectionClosed && f.statementClosed && f.queryClosed);
    }

    private static class Fixture {
        final ActionExecutionServiceImpl service = new ActionExecutionServiceImpl();
        final ActionDO action = new ActionDO();
        int rows, cursor, timeout, maxRows, connections, permissionChecks, criteriaChecks;
        boolean readOnly, connectionClosed, statementClosed, resultClosed, queryClosed, deny, failQuery, failReadOnly;
        String sql, criteria = "{\"decision\":\"PASS\",\"passed\":true}";

        Fixture() throws Exception {
            action.setId(1L); action.setVersion(1); action.setConceptId(2L); action.setActionType("UPDATE");
            ConceptTableDO table = new ConceptTableDO();
            table.setId(3L); table.setDatasourceId(4L); table.setTableName("orders");
            PropertyColumnDO id = column(5L, "id"), name = column(6L, "name");
            PropertyDO idProp = property("id", true), nameProp = property("name", false);
            inject("objectMapper", new ObjectMapper());
            inject("actionMapper", proxy(ActionMapper.class, (o,m,a) -> {
                if (m.getName().equals("selectById")) return action;
                throw new AssertionError("Unexpected action operation " + m.getName());
            }));
            inject("conceptTableMapper", proxy(ConceptTableMapper.class, (o,m,a) -> Collections.singletonList(table)));
            inject("propertyColumnMapper", proxy(PropertyColumnMapper.class, (o,m,a) -> Arrays.asList(id,name)));
            inject("propertyMapper", proxy(PropertyMapper.class, (o,m,a) -> Long.valueOf(5).equals(a[0]) ? idProp : nameProp));
            inject("tableGovernanceApiService", proxy(IAssetsTableGovernanceApiService.class, (o,m,a) -> {
                permissionChecks++; if (deny) throw new SecurityException("denied"); return null;
            }));
            inject("approvalService", proxy(IActionApprovalService.class, (o,m,a) -> {
                if (!m.getName().equals("evaluateCriteria")) throw new AssertionError("Approval write");
                criteriaChecks++; return criteria;
            }));
            inject("executionMapper", proxy(ActionExecutionMapper.class, (o,m,a) -> { throw new AssertionError("Execution record access"); }));
            DatasourceRespDTO ds = new DatasourceRespDTO();
            ds.setDatasourceType("MYSQL"); ds.setDatasourceConfig("{}");
            inject("datasourceApiService", proxy(IDatasourceApiService.class, (o,m,a) -> ds));
            inject("dataSourceFactory", (DataSourceFactory) property -> proxy(DbQuery.class, (o,m,a) -> {
                if (m.getName().equals("getConnection")) { connections++; cursor = 0; return connection(); }
                if (m.getName().equals("close")) { queryClosed = true; return null; }
                throw new AssertionError("Unexpected DbQuery call " + m.getName());
            }));
        }
        Connection connection() {
            return proxy(Connection.class, (o,m,a) -> {
                switch (m.getName()) {
                    case "setReadOnly":
                        if (failReadOnly) throw new SQLFeatureNotSupportedException("SECRET");
                        readOnly = (Boolean) a[0]; return null;
                    case "isReadOnly": return readOnly;
                    case "createStatement": return statement();
                    case "close": connectionClosed = true; return null;
                    default: throw new AssertionError("Unexpected connection call " + m.getName());
                }
            });
        }
        Statement statement() {
            return proxy(Statement.class, (o,m,a) -> {
                switch (m.getName()) {
                    case "setQueryTimeout": timeout = (Integer) a[0]; return null;
                    case "setMaxRows": maxRows = (Integer) a[0]; return null;
                    case "executeQuery":
                        sql = (String) a[0];
                        if (failQuery) throw new SQLException("SECRET SQL jdbc password");
                        return proxy(ResultSet.class, (r,method,args) -> {
                            if (method.getName().equals("next")) return ++cursor <= rows;
                            if (method.getName().equals("close")) { resultClosed = true; return null; }
                            throw new AssertionError("No row contents may be read");
                        });
                    case "close": statementClosed = true; return null;
                    default: throw new AssertionError("No DML allowed: " + m.getName());
                }
            });
        }
        ExecutionSubmitReqVO request() {
            ExecutionSubmitReqVO req = new ExecutionSubmitReqVO();
            req.setActionId(1L); req.setExpectedVersion(1); req.setObjectKey("key");
            req.setInputParams("{\"id\":\"1\",\"name\":\"SECRET\"}");
            req.setSpaceId(10L); req.setSpaceCode("space"); return req;
        }
        void inject(String field, Object value) throws Exception {
            Field f = ActionExecutionServiceImpl.class.getDeclaredField(field); f.setAccessible(true); f.set(service,value);
        }
        static PropertyColumnDO column(Long propertyId, String column) {
            PropertyColumnDO value = new PropertyColumnDO(); value.setPropertyId(propertyId); value.setColumnName(column); return value;
        }
        static PropertyDO property(String code, boolean primary) {
            PropertyDO value = new PropertyDO(); value.setCode(code); value.setIsPrimary(primary); return value;
        }
    }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
