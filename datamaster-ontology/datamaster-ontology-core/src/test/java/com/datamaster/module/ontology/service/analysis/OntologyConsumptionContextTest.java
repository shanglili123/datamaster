package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static com.datamaster.module.ontology.service.analysis.MetricQueryBuilderTest.rejected;

public class OntologyConsumptionContextTest {
    @Test public void omittedBrowserSpaceUsesStoredOntologyAndMismatchCannotSelectLightPolicy() throws Exception {
        MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class);
        OntologyConsumptionContext context = new OntologyConsumptionContext(); Field field = context.getClass().getDeclaredField("access"); field.setAccessible(true); field.set(context, access);
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L);
        OntologyDO ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(1L); ontology.setSpaceCode("space01"); when(access.ontology(20L)).thenReturn(ontology);
        assertSame(ontology, context.resolve(concept, null, null, null));
        rejected(() -> context.resolve(concept, 99L, null, null));
        rejected(() -> context.resolve(concept, null, 2L, "space02"));
        rejected(() -> context.resolve(concept, null, 1L, "space02"));
        rejected(() -> context.resolve(null, null, null, null));
    }
    @Test public void membershipFailurePropagatesBeforeConsumersCanQuery() throws Exception {
        MetricAnalysisAccessGuard access = mock(MetricAnalysisAccessGuard.class); when(access.ontology(20L)).thenThrow(new com.datamaster.common.exception.ServiceException("非成员"));
        OntologyConsumptionContext context = new OntologyConsumptionContext(); Field field = context.getClass().getDeclaredField("access"); field.setAccessible(true); field.set(context, access);
        ConceptDO concept = new ConceptDO(); concept.setOntologyId(20L);
        rejected(() -> context.resolve(concept, 20L, 1L, "space01"));
    }
}
