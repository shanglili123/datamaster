package com.datamaster.module.ontology.service.metric;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.impl.MetricAccessGuard;
import com.datamaster.module.ontology.service.impl.MetricServiceImpl;
import org.apache.ibatis.annotations.Select;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class MetricPublishedSearchTest {
    @Test
    public void returnsPublicationRatherThanRenamedDraftAndPreservesFilteredTotal() throws Exception {
        MetricDO draft = new MetricDO();
        draft.setId(10L); draft.setOntologyId(1L); draft.setConceptId(2L); draft.setCode("paid_amount");
        draft.setName("实收收入"); draft.setStatus("PUBLISHED"); draft.setPublishedVersion(1);
        MetricRespVO snapshot = new MetricRespVO();
        snapshot.setId(10L); snapshot.setOntologyId(1L); snapshot.setConceptId(2L); snapshot.setCode("paid_amount");
        snapshot.setName("支付金额"); snapshot.setStatus("PUBLISHED"); snapshot.setPublishedVersion(1);
        MetricVersionDO version = new MetricVersionDO(); version.setSnapshotJson(JSON.toJSONString(snapshot));
        MetricServiceImpl service = new MetricServiceImpl();
        inject(service, "accessGuard", new MetricAccessGuard() {
            @Override public void assertOntologyAccess(Long id) { assertEquals(Long.valueOf(1), id); }
        });
        inject(service, "metricMapper", Proxy.newProxyInstance(MetricMapper.class.getClassLoader(), new Class<?>[]{MetricMapper.class}, (proxy, method, args) -> {
            assertEquals("selectPublishedPage", method.getName());
            MetricPageReqVO req = (MetricPageReqVO) args[1];
            assertEquals("支付金额", req.getName()); assertEquals("paid_amount", args[2]);
            @SuppressWarnings("unchecked") IPage<MetricDO> page = (IPage<MetricDO>) args[0];
            page.setRecords(Collections.singletonList(draft)); page.setTotal(3); return page;
        }));
        inject(service, "metricVersionMapper", Proxy.newProxyInstance(MetricVersionMapper.class.getClassLoader(), new Class<?>[]{MetricVersionMapper.class},
                (proxy, method, args) -> { assertEquals("selectOne", method.getName()); return version; }));
        MetricPageReqVO req = new MetricPageReqVO(); req.setOntologyId(1L); req.setName("支付金额"); req.setPageNum(1); req.setPageSize(1);
        PageResult<MetricRespVO> result = service.getPublishedPage(req, "paid_amount");
        assertEquals(Long.valueOf(3), result.getTotal());
        assertEquals("支付金额", result.getRows().get(0).getName());
        assertEquals(Integer.valueOf(1), result.getRows().get(0).getPublishedVersion());
    }

    @Test
    public void sqlFiltersPublishedSnapshotAndExactCodeBeforePaging() throws Exception {
        String sql = String.join(" ", MetricMapper.class.getMethod("selectPublishedPage", IPage.class, MetricPageReqVO.class, String.class)
                .getAnnotation(Select.class).value());
        assertTrue(sql.contains("v.version = m.published_version"));
        assertTrue(sql.contains("m.status = 'PUBLISHED'"));
        assertTrue(sql.contains("CAST(v.snapshot_json AS jsonb) -&gt;&gt; 'name'"));
        assertTrue(sql.contains("LOWER(m.code) = LOWER(#{code})"));
        assertFalse(sql.contains("m.name"));
        MetricPageReqVO req = new MetricPageReqVO(); req.setOntologyId(1L); req.setName("支付金额");
        Map<String, Object> parameters = new HashMap<>(); parameters.put("req", req); parameters.put("code", "paid_amount");
        org.apache.ibatis.mapping.BoundSql bound = new org.apache.ibatis.scripting.xmltags.XMLLanguageDriver()
                .createSqlSource(new org.apache.ibatis.session.Configuration(), sql, Map.class).getBoundSql(parameters);
        assertTrue(bound.getSql().contains("STRPOS(LOWER(CAST(v.snapshot_json AS jsonb) ->> 'name'), LOWER(?)) > 0"));
        assertTrue(bound.getSql().contains("LOWER(m.code) = LOWER(?)"));
        assertEquals(3, bound.getParameterMappings().size());
    }

    @Test public void versionDetailsRejectMismatchedOrNullPublicationInsteadOfReturningIt() throws Exception {
        MetricDO current = new MetricDO(); current.setId(10L); current.setOntologyId(1L); current.setConceptId(2L); current.setCode("sales");
        MetricServiceImpl service = new MetricServiceImpl();
        inject(service, "accessGuard", new MetricAccessGuard() { @Override public void assertOntologyAccess(Long id) { assertEquals(Long.valueOf(1L), id); } });
        inject(service, "metricMapper", Proxy.newProxyInstance(MetricMapper.class.getClassLoader(), new Class<?>[]{MetricMapper.class}, (proxy, method, args) -> current));
        MetricVersionDO version = new MetricVersionDO();
        MetricRespVO publication = new MetricRespVO(); publication.setId(10L); publication.setOntologyId(99L); publication.setConceptId(2L); publication.setCode("sales"); publication.setPublishedVersion(1); publication.setStatus("PUBLISHED");
        version.setSnapshotJson(JSON.toJSONString(publication));
        inject(service, "metricVersionMapper", Proxy.newProxyInstance(MetricVersionMapper.class.getClassLoader(), new Class<?>[]{MetricVersionMapper.class}, (proxy, method, args) -> version));
        try { service.version(10L, 1); fail("Must reject cross-ontology snapshot"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
        version.setSnapshotJson("null");
        try { service.version(10L, 1); fail("Must reject null snapshot"); } catch (com.datamaster.common.exception.ServiceException expected) { assertNotNull(expected.getMessage()); }
    }

    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
