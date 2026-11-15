package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Selection rules only: no external query, datasource or graph writes. */
public class ObjectSourceSelectionTest {
    @Test public void multipleBindingsRequireExplicitSelectionForBothObjectAndRelationSources() throws Exception {
        Fixture f = new Fixture(); ConceptTableDO first = table(40L), second = table(41L);
        when(f.tables.selectByConceptId(30L)).thenReturn(List.of(first, second));
        reject(() -> f.binding.invoke(f.service, 30L, null));
        ObjectInstanceQueryReqVO request = new ObjectInstanceQueryReqVO();
        reject(() -> f.objectBinding.invoke(f.service, request, 30L));
        request.setTableBindingId(41L);
        assertSame(second, f.objectBinding.invoke(f.service, request, 30L));
        assertSame(first, f.binding.invoke(f.service, 30L, 40L));
    }
    @Test public void soleBindingRemainsCompatibleButWrongOrIncompleteBindingIsRejected() throws Exception {
        Fixture f = new Fixture(); ConceptTableDO binding = table(40L);
        when(f.tables.selectByConceptId(30L)).thenReturn(List.of(binding));
        assertSame(binding, f.binding.invoke(f.service, 30L, null));
        reject(() -> f.binding.invoke(f.service, 30L, 99L));
        binding.setDatasourceId(null); reject(() -> f.binding.invoke(f.service, 30L, 40L));
        binding.setDatasourceId(9L); binding.setConceptId(99L); reject(() -> f.binding.invoke(f.service, 30L, 40L));
    }
    @Test public void duplicateRelationshipMappingsCannotSilentlyChooseFirstPath() throws Exception {
        Fixture f = new Fixture(); RelationColumnDO first = mapping(40L), second = mapping(40L);
        assertSame(first, f.relation.invoke(f.service, 40L, List.of(first)));
        reject(() -> f.relation.invoke(f.service, 40L, List.of(first, second)));
        assertNull(f.relation.invoke(f.service, 41L, List.of(first)));
        reject(() -> f.relation.invoke(f.service, 40L, Arrays.asList(first, null)));
    }
    private static class Fixture {
        final ObjectInstanceQueryServiceImpl service = new ObjectInstanceQueryServiceImpl();
        final ConceptTableMapper tables = mock(ConceptTableMapper.class);
        final Method binding, objectBinding, relation;
        Fixture() throws Exception {
            Field field = service.getClass().getDeclaredField("conceptTableMapper"); field.setAccessible(true); field.set(service, tables);
            binding = method("resolveBinding", Long.class, Long.class);
            objectBinding = method("resolveTableBinding", ObjectInstanceQueryReqVO.class, Long.class);
            relation = method("resolveRelationColumn", Long.class, List.class);
        }
        Method method(String name, Class<?>... types) throws Exception {
            Method method = service.getClass().getDeclaredMethod(name, types); method.setAccessible(true); return method;
        }
    }
    private static ConceptTableDO table(Long id) {
        ConceptTableDO table = new ConceptTableDO(); table.setId(id); table.setConceptId(30L); table.setDatasourceId(9L); table.setTableName("orders"); return table;
    }
    private static RelationColumnDO mapping(Long source) {
        RelationColumnDO mapping = new RelationColumnDO(); mapping.setSourceConceptTableId(source); mapping.setTargetConceptTableId(42L);
        mapping.setSourceColumn("id"); mapping.setTargetColumn("order_id"); return mapping;
    }
    private interface Call { void run() throws Exception; }
    private static void reject(Call call) throws Exception {
        try { call.run(); fail("Ambiguous source selection must be rejected"); }
        catch (InvocationTargetException failure) { assertTrue(failure.getCause() instanceof ServiceException); }
    }
}
