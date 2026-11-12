package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.module.assets.api.governance.dto.*;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class ConceptTableConsumptionGuardTest {
    @Test public void previewUsesStoredOntologyAndRequiresDatasourceSpaceAndConnectionIdentity() throws Exception {
        Fixture f = new Fixture(); assertSame(f.ontology, f.guard.source(f.table));
        f.guard.connection(f.table, new DbQueryProperty("MySql", "unused", "reader", null, 1, "business", null));
        assertEquals("orders", f.guard.physicalTable(f.table, new DbQueryProperty("MySql", "unused", "reader", null, 1, "business", null)));
        f.table.setSchemaName("reporting");
        assertEquals("\"reporting\".\"orders\"", f.guard.physicalTable(f.table, new DbQueryProperty("PostgreSQL", "unused", "reader", null, 1, "business", "reporting")));
        rejected(() -> f.guard.connection(f.table, new DbQueryProperty("MySql", "unused", "reader", null, 1, "other", null)));
        when(f.datasources.hasDatasourceSpace(9L, 1L, "space01")).thenReturn(false); rejected(() -> f.guard.source(f.table));
    }
    @Test public void deniesUnknownColumnsPartialAuthorizationAndHiddenInputs() throws Exception {
        Fixture f = new Fixture(); rejected(() -> f.guard.admit(f.table, f.ontology, List.of()));
        f.response.setAllowedColumns(List.of("id")); rejected(() -> f.guard.admit(f.table, f.ontology, null));
        rejected(() -> f.guard.admit(f.table, f.ontology, List.of("id", "secret")));
        f.guard.admit(f.table, f.ontology, List.of("id"));
        rejected(() -> f.guard.admit(f.table, f.ontology, List.of("(SELECT secret FROM other)")));
        f.response.setAccessAllowed(false); rejected(() -> f.guard.admit(f.table, f.ontology, List.of("id")));
    }
    @Test public void deniedReturnCheckNeverReturnsPreviewRows() throws Exception {
        Fixture f = new Fixture(); f.response.setAccessAllowed(false);
        rejected(() -> f.guard.mask(f.table, f.ontology, List.of(Map.of("id", "1"))));
    }
    private static class Fixture {
        final ConceptTableConsumptionGuard guard = new ConceptTableConsumptionGuard(); final IDatasourceApiService datasources = mock(IDatasourceApiService.class);
        final IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class);
        final OntologyDO ontology = new OntologyDO(); final ConceptTableDO table = new ConceptTableDO(); final AssetsTableGovernanceRespDTO response = new AssetsTableGovernanceRespDTO();
        Fixture() throws Exception {
            ConceptMapper concepts = mock(ConceptMapper.class); OntologyConsumptionContext context = mock(OntologyConsumptionContext.class);
            ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
            ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(context.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(ontology);
            table.setId(40L); table.setConceptId(30L); table.setDatasourceId(9L); table.setDatabaseName("business"); table.setTableName("orders");
            when(datasources.hasDatasourceSpace(9L, 1L, "space01")).thenReturn(true);
            response.setAccessAllowed(true); when(governance.resolveTable(any())).thenReturn(response);
            inject(guard, "concepts", concepts); inject(guard, "context", context); inject(guard, "datasources", datasources); inject(guard, "governance", governance);
        }
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
