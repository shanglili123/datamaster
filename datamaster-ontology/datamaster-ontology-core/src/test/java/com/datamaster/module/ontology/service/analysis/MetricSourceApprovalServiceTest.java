package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.domain.model.LoginUser;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.controller.admin.metricsource.MetricSourceApprovalController;
import com.datamaster.module.ontology.controller.admin.metricsource.vo.MetricSourceApprovalSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

/** In-memory application-JDBC double only. No Spring context, migration execution or external connection. */
public class MetricSourceApprovalServiceTest {
    private MetricSourceApprovalService service;
    private MemoryJdbc jdbc;
    private MetricAnalysisAccessGuard access;
    private OntologyDO ontology;
    private IDatasourceApiService datasources;

    @Before public void setup() throws Exception {
        LoginUser login=new LoginUser(7L,1L,new SysUser(),Set.of("ont:metric:execute"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null));
        service=new MetricSourceApprovalService(); jdbc=new MemoryJdbc(); access=mock(MetricAnalysisAccessGuard.class);
        ontology=new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
        when(access.ontology(20L)).thenReturn(ontology);
        MetricMetadataLockMapper locks=mock(MetricMetadataLockMapper.class); when(locks.lockOntology(20L)).thenReturn(20L);
        datasources=mock(IDatasourceApiService.class); when(datasources.hasDatasourceSpace(50L,1L,"space01")).thenReturn(true);
        inject("jdbc",jdbc); inject("access",access); inject("policy",new MetricQueryPolicy(true,15,4));
        inject("metadataLocks",locks); inject("datasourceApiService",datasources);
    }
    @After public void cleanup() { SecurityContextHolder.clearContext(); }

    @Test public void approveAndRevokeWriteCurrentStateAndAppendEvents() {
        MetricSourceApprovalSaveReqVO request=request(0);
        service.approve(request);
        assertEquals("APPROVED",jdbc.approval.get("status")); assertEquals(1,jdbc.approval.get("revision"));
        assertEquals(1,jdbc.events); assertNotNull(service.authorize(MetricQueryBuilderTest.snapshot()));
        request.setRevision(1); service.revoke(request);
        assertEquals("REVOKED",jdbc.approval.get("status")); assertEquals(2,jdbc.approval.get("revision")); assertEquals(2,jdbc.events);
        rejected(() -> service.authorize(MetricQueryBuilderTest.snapshot()));
    }
    @Test public void absentRevokedAndChangedIdentityDenyAndApprovedCanBeRevokedImmediately() {
        MetricRespVO snapshot=MetricQueryBuilderTest.snapshot();
        rejected(() -> service.authorize(snapshot));
        jdbc.approval=approved(1);
        MetricSourceApprovalService.ExecutionContext execution=service.authorize(snapshot);
        assertEquals("reader",execution.getProperty().getUsername());
        service.recheck(execution,snapshot);
        jdbc.approval.put("status","REVOKED"); jdbc.approval.put("revision",2);
        rejected(() -> service.authorize(snapshot)); rejected(() -> service.recheck(execution,snapshot));
        jdbc.approval=approved(3);
        rejected(() -> service.recheck(execution,snapshot)); // Reapproval never revives an earlier execution context.
        assertNotNull(service.authorize(snapshot));
        jdbc.source.put("datasource_auth_revision",1L);
        rejected(() -> service.authorize(snapshot));
    }
    @Test public void sourceRoundTripRemainsStaleAndPasswordsAreNotPartOfPublicTokens() {
        String original=identity();
        jdbc.source.put("datasource_config","{\"username\":\"reader\",\"password\":\"different-synthetic-secret\",\"dbname\":\"ads\"}");
        assertEquals(original,identity()); // DB trigger, rather than a hash of a password, advances this revision.
        jdbc.source.put("datasource_auth_revision",1L); String changed=identity(); assertNotEquals(original,changed);
        jdbc.source.put("datasource_config","{\"username\":\"reader\",\"password\":\"synthetic-only\",\"dbname\":\"ads\"}");
        jdbc.source.put("datasource_auth_revision",2L); assertNotEquals(original,identity());
        jdbc.approval=approved(1); jdbc.source.put("binding_auth_revision",1L);
        rejected(() -> service.authorize(MetricQueryBuilderTest.snapshot()));
        jdbc.source.put("binding_auth_revision",2L);
        rejected(() -> service.authorize(MetricQueryBuilderTest.snapshot()));
    }
    @Test public void statusNeverLeaksAccountTokenConfigOrAuditAndFailureDoesNotAllow() {
        jdbc.approval=approved(1);
        Map<String,Object> status=service.status(20L,40L);
        assertEquals(Boolean.TRUE,status.get("available"));
        for(String secret:List.of("username","sourceToken","datasourceConfig","reason","approvedBy")) assertFalse(status.containsKey(secret));
        jdbc.fail=true; rejected(() -> service.authorize(MetricQueryBuilderTest.snapshot()));
    }
    @Test public void operatorMembershipManagerAndOptimisticPreconditionsAreRequired() {
        jdbc.managerId=8L; rejected(() -> service.approve(request(0))); assertEquals(0,jdbc.writes);
        jdbc.managerId=7L; jdbc.approval=approved(1);
        rejected(() -> service.approve(request(0))); assertEquals(0,jdbc.writes);
        MetricSourceApprovalSaveReqVO r=request(1); r.setReadonlyConfirmed(false);
        rejected(() -> service.approve(r)); assertEquals(0,jdbc.writes);
        r.setReadonlyConfirmed(true); r.setSourceToken("0".repeat(64));
        rejected(() -> service.approve(r)); assertEquals(0,jdbc.writes);
        when(access.ontology(20L)).thenThrow(new ServiceException("不是当前空间成员"));
        rejected(() -> service.settings(20L)); rejected(() -> service.status(20L,40L));
    }
    @Test public void readonlyConfirmationCannotApproveAnUnassociatedDatasource() {
        when(datasources.hasDatasourceSpace(50L,1L,"space01")).thenReturn(false);
        rejected(() -> service.approve(request(0))); assertEquals(0,jdbc.writes);
    }
    @Test public void executionPermissionDoesNotGrantControllerApprovalPermission() throws Exception {
        PreAuthorize approve=MetricSourceApprovalController.class.getMethod("approve",MetricSourceApprovalSaveReqVO.class).getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermi('ont:metric-source:approve')",approve.value());
        PreAuthorize revoke=MetricSourceApprovalController.class.getMethod("revoke",MetricSourceApprovalSaveReqVO.class).getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermi('ont:metric-source:revoke')",revoke.value());
    }
    private String identity() { return MetricSourceApprovalService.token(jdbc.source,1L,"reader"); }
    private Map<String,Object> approved(int revision) {
        Map<String,Object> record=new HashMap<>(); record.put("ontology_id",20L); record.put("space_id",1L);
        record.put("status","APPROVED"); record.put("source_token",identity()); record.put("revision",revision); return record;
    }
    private MetricSourceApprovalSaveReqVO request(int revision) {
        MetricSourceApprovalSaveReqVO request=new MetricSourceApprovalSaveReqVO(); request.setOntologyId(20L); request.setBindingId(40L);
        request.setRevision(revision); request.setSourceToken(identity()); request.setReason("Synthetic local test confirmation"); request.setReadonlyConfirmed(true); return request;
    }
    private void inject(String name,Object value) throws Exception { Field field=MetricSourceApprovalService.class.getDeclaredField(name); field.setAccessible(true); field.set(service,value); }
    private static class MemoryJdbc extends JdbcTemplate {
        final Map<String,Object> source=new HashMap<>(); Map<String,Object> approval; Long managerId=7L; boolean fail; int writes; int events;
        MemoryJdbc() {
            source.put("binding_id",40L); source.put("concept_id",30L); source.put("ontology_id",20L); source.put("datasource_id",50L);
            source.put("binding_auth_revision",0L); source.put("datasource_auth_revision",0L);
            source.put("database_name","ads"); source.put("table_name","ads_orders"); source.put("datasource_type","DORIS");
            source.put("ip","unused"); source.put("port",1);
            source.put("datasource_config","{\"username\":\"reader\",\"password\":\"synthetic-only\",\"dbname\":\"ads\"}");
        }
        @Override public List<Map<String,Object>> queryForList(String sql,Object...args) {
            if(fail) throw new ServiceException("应用库读取不可用");
            if(sql.contains("SELECT manager_id")) return List.of(Map.of("manager_id",managerId));
            if(sql.startsWith("SELECT * FROM ont_metric_source_approval")) return approval==null?List.of():List.of(approval);
            if(sql.contains("b.source_auth_revision")) return List.of(source);
            if(sql.startsWith("SELECT id,datasource_id FROM ont_concept_table")) return List.of(Map.of("id",40L,"datasource_id",50L));
            if(sql.startsWith("SELECT id FROM ast_datasource")) return List.of(Map.of("id",50L));
            if(sql.startsWith("SELECT b.id FROM ont_concept_table")) return List.of(Map.of("id",40L));
            throw new AssertionError("Unexpected application SQL: "+sql);
        }
        @Override public int update(String sql,Object...args) {
            writes++;
            if(sql.startsWith("INSERT INTO ont_metric_source_approval(")) {
                approval=new HashMap<>(); approval.put("ontology_id",args[1]); approval.put("space_id",args[2]);
                approval.put("datasource_id",args[4]); approval.put("username",args[7]); approval.put("database_name",args[8]);
                approval.put("schema_name",args[9]); approval.put("table_name",args[10]); approval.put("source_token",args[11]);
                approval.put("revision",args[12]); approval.put("status","APPROVED"); return 1;
            }
            if(sql.startsWith("UPDATE ont_metric_source_approval SET status='REVOKED'")) { approval.put("status","REVOKED"); approval.put("revision",args[0]); return 1; }
            if(sql.startsWith("INSERT INTO ont_metric_source_approval_event")) { events++; return 1; }
            throw new AssertionError("Unexpected write: "+sql);
        }
    }
}
