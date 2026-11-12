package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class MetricLightSourceGuardTest {
    @Test public void unconfiguredLightSpaceRequiresApprovedBindingReadonlyAccountAndRegisteredSafeMetadata() throws Exception {
        MetricRespVO snapshot = MetricQueryBuilderTest.snapshot(); snapshot.getSourceSnapshot().put("sourceMode", "SPACE_FLOW");
        snapshot.getSourceSnapshot().put("datasourceType", "MySql"); snapshot.getSourceSnapshot().put("databaseName", "business"); snapshot.getSourceSnapshot().put("tableName", "orders");
        MetricQueryBuilder.Plan plan = MetricQueryBuilder.compile(snapshot, MetricQueryBuilderTest.request());
        ConceptTableMapper bindings = mock(ConceptTableMapper.class); ConceptTableDO binding = new ConceptTableDO();
        binding.setConceptId(30L); binding.setDatasourceId(50L); binding.setDatabaseName("business"); binding.setTableName("orders");
        when(bindings.selectById(40L)).thenReturn(binding);
        IDatasourceApiService datasources = mock(IDatasourceApiService.class); DatasourceRespDTO source = new DatasourceRespDTO();
        source.setId(50L); source.setDatasourceType("MySql"); source.setIp("127.0.0.1"); source.setPort(3306L);
        // Synthetic local-only fixture; no connection is created.
        source.setDatasourceConfig("{\"dbname\":\"business\",\"username\":\"metric_reader\",\"password\":\"unit-test-only\"}");
        when(datasources.getDatasourceById(50L)).thenReturn(source);
        when(datasources.hasDatasourceSpace(50L, 1L, "space01")).thenReturn(true);
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        AssetsTableGovernanceRespDTO allowed = new AssetsTableGovernanceRespDTO(); allowed.setEnabled(false); allowed.setAccessAllowed(true); allowed.setMode("off");
        when(governance.resolveTable(any())).thenReturn(allowed);
        ISpaceFlowApi flows = mock(ISpaceFlowApi.class); when(flows.policy(1L, "space01")).thenReturn(new ISpaceFlowApi.Policy());
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Map<String,Object> table = new HashMap<>(); table.put("id", 60L); table.put("safety_level_id", null);
        when(jdbc.queryForList(contains("FROM cat_table"), eq(50L), eq("orders"), eq("business"), isNull(), eq(1L))).thenReturn(List.of(table));
        List<Map<String,Object>> columns = new ArrayList<>();
        for (String name : plan.getUsedColumns()) { Map<String,Object> column = new HashMap<>(); column.put("column_name", name); column.put("safety_level_id", null); columns.add(column); }
        when(jdbc.queryForList(contains("FROM cat_column"), eq(60L))).thenReturn(columns);
        MetricQuerySourceGuard guard = new MetricQuerySourceGuard();
        inject(guard, "policy", new MetricQueryPolicy(true, "40", "metric_reader", 15, 1)); inject(guard, "conceptTableMapper", bindings);
        inject(guard, "datasourceApiService", datasources); inject(guard, "governance", governance); inject(guard, "spaceFlows", flows); inject(guard, "jdbc", jdbc);
        OntologyDO ontology = new OntologyDO(); ontology.setSpaceId(1L); ontology.setSpaceCode("space01");
        DbQueryProperty property = guard.authorize(snapshot, plan, ontology); assertEquals("business", property.getDbName());
        allowed.setEnabled(true); allowed.setMode("strict"); allowed.setSource("CATALOG");
        rejected(() -> guard.authorize(snapshot, plan, ontology)); // Light mode cannot erase an enabled global strict asset policy.
        allowed.setEnabled(false); allowed.setMode("off"); allowed.setSource(null);
        when(datasources.hasDatasourceSpace(50L, 1L, "space01")).thenReturn(false); rejected(() -> guard.authorize(snapshot, plan, ontology));
        when(datasources.hasDatasourceSpace(50L, 1L, "space01")).thenReturn(true);
        columns.get(0).put("safety_level_id", 1L); rejected(() -> guard.authorize(snapshot, plan, ontology));
        columns.get(0).put("safety_level_id", null); table.put("safety_level_id", 1L); rejected(() -> guard.authorize(snapshot, plan, ontology));
        table.put("safety_level_id", null); snapshot.getSourceSnapshot().remove("sourceMode");
        rejected(() -> guard.authorize(snapshot, plan, ontology)); // Old publications never silently inherit light access.
    }
    @Test public void dialectRejectsArbitraryTypesLegacyNonAdsAndImplicitPostgresSchema() {
        rejected(() -> MetricSqlDialect.of("Oracle"));
        rejected(() -> MetricSqlDialect.of("MySql").validateSource("business", null, "orders", true));
        rejected(() -> MetricSqlDialect.of("PostgreSQL").validateSource("business", null, "orders", false));
        assertEquals("\"reporting\".\"orders\"", MetricSqlDialect.of("PostgreSQL").table("business", "reporting", "orders"));
        assertEquals("`business`.`orders`", MetricSqlDialect.of("MySql").table("business", null, "orders"));
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
