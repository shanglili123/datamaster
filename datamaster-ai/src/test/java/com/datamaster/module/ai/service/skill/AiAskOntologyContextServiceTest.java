package com.datamaster.module.ai.service.skill;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.datasource.mgmt.mapper.DatasourceSpaceRelMgmtMapper;
import com.datamaster.common.datasource.mgmt.service.IDatasourceMgmtService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskOntologyContextRespVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IMetricService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.Assert.*;

/** Isolated resolver contracts: no database or model invocations. */
public class AiAskOntologyContextServiceTest {
    private AiAskOntologyContextService service;
    private OntologyDO ontology;
    private final List<ConceptTableDO> bindings = new ArrayList<>();
    private final List<RelationTableDO> relationBindings = new ArrayList<>();
    private final Map<Long, DatasourceDO> sources = new HashMap<>();
    private boolean member;
    private boolean sourceVisible;
    private int metadataReads;

    @Before public void setUp() throws Exception {
        service = new AiAskOntologyContextService(); member = true; sourceVisible = true; metadataReads = 0;
        bindings.clear(); relationBindings.clear(); sources.clear();
        ontology = new OntologyDO(); ontology.setId(20L); ontology.setSpaceId(10L); ontology.setSpaceCode("space10"); ontology.setDelFlag(0);
        for (Class<?> type : List.of(ConceptDO.class, RelationDO.class, DatasourceSpaceRelDO.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), type);
        inject("ontologyMapper", proxy(OntologyMapper.class, (p, m, a) -> ontology));
        inject("spaceApi", proxy(ISpaceApi.class, (p, m, a) -> {
            if (m.getName().equals("getSpaceCodeBySpaceId")) return "space" + a[0];
            if (m.getName().equals("hasUserSpace")) return member;
            return null;
        }));
        ConceptDO concept = new ConceptDO(); concept.setId(30L); concept.setOntologyId(20L);
        RelationDO relation = new RelationDO(); relation.setId(40L); relation.setOntologyId(20L);
        inject("conceptMapper", proxy(ConceptMapper.class, (p, m, a) -> { metadataReads++; return List.of(concept); }));
        inject("relationMapper", proxy(RelationMapper.class, (p, m, a) -> List.of(relation)));
        inject("conceptTableMapper", proxy(ConceptTableMapper.class, (p, m, a) -> bindings));
        inject("relationTableMapper", proxy(RelationTableMapper.class, (p, m, a) -> relationBindings));
        inject("datasourceService", proxy(IDatasourceMgmtService.class, (p, m, a) -> sources.get(a[0])));
        inject("datasourceSpaces", proxy(DatasourceSpaceRelMgmtMapper.class, (p, m, a) -> sourceVisible ? 1L : 0L));
        MetricRespVO metric = new MetricRespVO(); metric.setId(50L); metric.setName("Published snapshot"); metric.setPublishedVersion(2);
        inject("metricService", proxy(IMetricService.class, (p, m, a) -> new PageResult<>(List.of(metric), 1L)));
    }

    @Test public void deduplicatesConceptAndRelationBindingsAndDefaultsToStableFirst() {
        bind(8L); bind(3L);
        RelationTableDO relation = new RelationTableDO(); relation.setRelationId(40L); relation.setDatasourceId(8L); relationBindings.add(relation);
        AiAskOntologyContextRespVO context = service.context(20L, 10L, "space10", 7L);
        assertEquals(List.of(3L, 8L), context.getDatasources().stream().map(AiAskOntologyContextRespVO.Datasource::getId).toList());
        assertEquals(Long.valueOf(3L), resolve(null, true));
        assertEquals(Long.valueOf(8L), resolve(8L, true));
        assertEquals("Published snapshot", context.getMetrics().get(0).getName());
        assertEquals(Integer.valueOf(2), context.getMetrics().get(0).getPublishedVersion());
    }

    @Test public void relationOnlyBindingsAreIncluded() {
        bind(8L); bindings.clear();
        RelationTableDO relation = new RelationTableDO(); relation.setRelationId(40L); relation.setDatasourceId(8L); relationBindings.add(relation);
        assertEquals(Long.valueOf(8L), resolve(null, true));
        relation.setDelFlag(1);
        rejected(() -> resolve(null, true));
    }

    @Test public void emptyOntologyAllowsConfigurationAndFormalAnalysisButNotExploration() {
        assertTrue(service.context(20L, 10L, "space10", 7L).getDatasources().isEmpty());
        assertNull(resolve(null, false));
        rejected(() -> resolve(null, true));
        rejected(() -> resolve(99L, false));
    }

    @Test public void rejectsUnboundDisabledDeletedAndInaccessibleSelections() {
        bind(8L);
        rejected(() -> resolve(99L, true));
        sources.get(8L).setValidFlag(false); rejected(() -> resolve(8L, true));
        sources.get(8L).setValidFlag(true); sources.get(8L).setDelFlag(true); rejected(() -> resolve(8L, true));
        sources.get(8L).setDelFlag(false); sourceVisible = false; rejected(() -> resolve(8L, true));
        sourceVisible = true; bindings.get(0).setDelFlag(1); rejected(() -> resolve(8L, true));
    }

    @Test public void authorizesBeforeReadingBindingMetadata() {
        member = false; rejected(() -> resolve(null, false)); assertEquals(0, metadataReads);
        member = true;
        rejected(() -> service.context(20L, 11L, "space11", 7L)); assertEquals(0, metadataReads);
        ontology.setSpaceCode("wrong"); rejected(() -> resolve(null, false)); assertEquals(0, metadataReads);
        ontology.setSpaceCode("space10"); ontology.setDelFlag(1); rejected(() -> resolve(null, false)); assertEquals(0, metadataReads);
    }

    @Test public void legacyCallsWithoutOntologyRetainExplicitDatasource() {
        assertEquals(Long.valueOf(8L), service.resolveDatasource(null, 8L, 10L, "space10", 7L, true));
        assertEquals(0, metadataReads);
    }

    private Long resolve(Long selected, boolean required) { return service.resolveDatasource(20L, selected, 10L, "space10", 7L, required); }
    private void bind(Long id) {
        ConceptTableDO binding = new ConceptTableDO(); binding.setConceptId(30L); binding.setDatasourceId(id); bindings.add(binding);
        DatasourceDO source = new DatasourceDO(); source.setId(id); source.setDatasourceName("Source " + id); source.setValidFlag(true); source.setDelFlag(false); sources.put(id, source);
    }
    private void inject(String name, Object value) throws Exception { Field field = service.getClass().getDeclaredField(name); field.setAccessible(true); field.set(service, value); }
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
    private static void rejected(Runnable call) {
        try { call.run(); fail("Expected rejection"); } catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
    }
}
