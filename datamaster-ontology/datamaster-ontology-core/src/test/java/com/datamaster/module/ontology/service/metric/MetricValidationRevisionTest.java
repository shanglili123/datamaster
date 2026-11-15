package com.datamaster.module.ontology.service.metric;

import com.datamaster.module.ontology.controller.admin.metric.MetricController;
import com.datamaster.module.ontology.dal.dataobject.MetricDO;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IMetricService;
import com.datamaster.module.ontology.service.impl.MetricAccessGuard;
import com.datamaster.module.ontology.service.impl.MetricServiceImpl;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MetricValidationRevisionTest {
    @Test public void controllerForwardsTheDisplayedRevision() throws Exception {
        MetricController controller = new MetricController(); IMetricService service = mock(IMetricService.class);
        inject(controller, "metricService", service); controller.validate(10L, 3);
        verify(service).validate(10L, 3); verify(service, never()).validate(anyLong());
    }

    @Test public void staleRevisionIsRejectedAfterLockBeforeReadingDefinitionMetadata() throws Exception {
        MetricServiceImpl service = new MetricServiceImpl(); MetricMapper metrics = mock(MetricMapper.class);
        MetricMetadataLockMapper locks = mock(MetricMetadataLockMapper.class); MetricAccessGuard access = mock(MetricAccessGuard.class);
        ConceptMapper concepts = mock(ConceptMapper.class); ConceptTableMapper tables = mock(ConceptTableMapper.class);
        inject(service, "metricMapper", metrics); inject(service, "metadataLockMapper", locks); inject(service, "accessGuard", access);
        inject(service, "conceptMapper", concepts); inject(service, "conceptTableMapper", tables);
        MetricDO metric = new MetricDO(); metric.setId(10L); metric.setOntologyId(20L); metric.setRevision(4);
        when(metrics.selectById(10L)).thenReturn(metric); when(metrics.selectForUpdate(10L)).thenReturn(metric); when(locks.lockOntology(20L)).thenReturn(20L);
        try { service.validate(10L, 3); fail("Stale revision must not be silently ignored"); }
        catch (com.datamaster.common.exception.ServiceException expected) { assertTrue(expected.getMessage().contains("revision")); }
        org.mockito.InOrder order = inOrder(metrics, locks);
        order.verify(metrics).selectById(10L); order.verify(locks).lockOntology(20L); order.verify(metrics).selectForUpdate(10L);
        verifyNoInteractions(concepts, tables); verify(metrics, never()).updateById(any(MetricDO.class));
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
