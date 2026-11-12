package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ObjectLineageAdmissionTest {
    @Test public void failedAdmissionDoesNotReturnDecisionSnapshotsOrReadExternalLineage() throws Exception {
        ObjectLineageServiceImpl service = new ObjectLineageServiceImpl(); ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        ActionMapper actions = mock(ActionMapper.class); ActionExecutionMapper executions = mock(ActionExecutionMapper.class);
        IAssetsTableGovernanceApiService governance = mock(IAssetsTableGovernanceApiService.class); OntologyConsumptionContext context = mock(OntologyConsumptionContext.class);
        inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables); inject(service, "actionMapper", actions); inject(service, "actionExecutionMapper", executions); inject(service, "tableGovernanceApiService", governance); inject(service, "consumptionContext", context);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L); when(concepts.selectById(30L)).thenReturn(concept);
        OntologyDO ontology = new OntologyDO(); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(context.resolve(eq(concept), isNull(), isNull(), isNull())).thenReturn(ontology);
        ConceptTableDO table = new ConceptTableDO(); table.setId(40L); table.setConceptId(30L); table.setDatasourceId(50L); table.setTableName("orders"); when(tables.selectByConceptId(30L)).thenReturn(List.of(table));
        when(governance.resolveTable(any())).thenThrow(new com.datamaster.common.exception.ServiceException("新流程未满足"));
        var response = service.objectLineage(30L, null, null);
        assertEquals(Boolean.FALSE, response.getPermission().getAccessible());
        verifyNoInteractions(actions, executions);
        com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO partial = new com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO(); partial.setAccessAllowed(true); partial.setAllowedColumns(List.of("id"));
        doReturn(partial).when(governance).resolveTable(any());
        assertEquals(Boolean.FALSE, service.objectLineage(30L, null, null).getPermission().getAccessible());
        verifyNoInteractions(actions, executions);
    }
    private static void inject(Object bean, String name, Object value) throws Exception { Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value); }
}
