package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.service.IMetricService;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.Assert.*;

public class SearchMetricsToolTest {
    @Test public void searchesPublicationWithExactCodeAndDoesNotReadDraftOrReloadVersion() throws Exception {
        SearchMetricsTool tool = new SearchMetricsTool();
        MetricRespVO published = new MetricRespVO(); published.setId(10L); published.setOntologyId(1L);
        published.setName("支付金额"); published.setCode("paid_amount"); published.setStatus("PUBLISHED"); published.setPublishedVersion(1);
        published.setUnit("CNY"); published.setDimensionPropertyIds(Collections.emptyList());
        inject(tool, "metricService", Proxy.newProxyInstance(IMetricService.class.getClassLoader(), new Class<?>[]{IMetricService.class}, (proxy, method, args) -> {
            assertEquals("getPublishedPage", method.getName());
            MetricPageReqVO req = (MetricPageReqVO) args[0]; assertEquals("支付金额", req.getName());
            assertEquals("paid_amount", args[1]); assertEquals(Long.valueOf(1), req.getOntologyId());
            assertEquals("PUBLISHED", req.getStatus());
            return new PageResult<>(Collections.singletonList(published), 1L);
        }));
        inject(tool, "support", new MetricAgentToolSupport() {
            @Override public AgentToolResult guard(AgentToolContext context, String... permissions) { return null; }
            @Override public Long currentOntology(AgentToolContext context) { return 1L; }
            @Override public void assertOntology(Long id, AgentToolContext context) { assertEquals(Long.valueOf(1), id); }
        });
        AgentToolContext context = new AgentToolContext("op", 7L, "owner", 8L, "space", Collections.emptyMap());
        AgentToolContextHolder.set(context);
        try {
            // A stale client or model may still send a status; it cannot change publication scope or stop discovery.
            AgentToolRequest.SearchMetrics request = com.alibaba.fastjson2.JSON.parseObject(
                    "{\"name\":\"支付金额\",\"code\":\"paid_amount\",\"status\":\"DRAFT\"}", AgentToolRequest.SearchMetrics.class);
            AgentToolResult result = tool.execute(request); assertEquals(AgentToolResult.SUCCEEDED, result.getStatus());
            Map<?, ?> data = (Map<?, ?>) result.getData(); assertEquals(true, data.get("uniqueMatch"));
            assertEquals(true, data.get("searchComplete"));
            Map<?, ?> row = (Map<?, ?>) ((List<?>) data.get("rows")).get(0);
            assertEquals("支付金额", row.get("name")); assertEquals("CNY", row.get("unit"));
            assertFalse(row.containsKey("sourceSnapshot"));
        } finally { AgentToolContextHolder.clear(); }
    }

    @Test public void repeatedHumanNameIsNotPassedAsInvalidCode() throws Exception {
        SearchMetricsTool tool = new SearchMetricsTool();
        inject(tool, "metricService", Proxy.newProxyInstance(IMetricService.class.getClassLoader(), new Class<?>[]{IMetricService.class}, (proxy, method, args) -> {
            assertEquals("getPublishedPage", method.getName());
            String actualName = ((MetricPageReqVO) args[0]).getName();
            assertTrue(actualName == null || "真实验收_count".equals(actualName));
            assertNull(args[1]);
            return new PageResult<>(Collections.emptyList(), 0L);
        }));
        inject(tool, "support", new MetricAgentToolSupport() {
            @Override public AgentToolResult guard(AgentToolContext context, String... permissions) { return null; }
            @Override public Long currentOntology(AgentToolContext context) { return 1L; }
            @Override public void assertOntology(Long id, AgentToolContext context) { }
        });
        try (AgentToolContextHolder.Scope ignored = AgentToolContextHolder.scope(new AgentToolContext("op",7L,"owner",8L,"space",Collections.emptyMap()))) {
            AgentToolRequest.SearchMetrics input = new AgentToolRequest.SearchMetrics();
            input.setName("真实验收_count"); input.setCode("真实验收_count");
            assertEquals(AgentToolResult.SUCCEEDED, tool.execute(input).getStatus());
            input.setCode("其他中文编码");
            assertEquals(AgentToolResult.NEED_INPUT, tool.execute(input).getStatus());
        }
    }

    @Test public void unmatchedBusinessPhraseReturnsSameOntologyCatalogueForSemanticSelection() throws Exception {
        SearchMetricsTool tool = new SearchMetricsTool();
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        MetricRespVO metric = new MetricRespVO(); metric.setId(10L); metric.setName("真实验收_count");
        metric.setStatus("PUBLISHED"); metric.setPublishedVersion(1); metric.setAggregation("COUNT");
        metric.setDimensionPropertyIds(Collections.emptyList());
        inject(tool,"metricService",Proxy.newProxyInstance(IMetricService.class.getClassLoader(),new Class<?>[]{IMetricService.class},(proxy,method,args)->{
            MetricPageReqVO page=(MetricPageReqVO)args[0]; assertEquals(Long.valueOf(1),page.getOntologyId());
            if(calls.incrementAndGet()==1){ assertEquals("销售记录条数",page.getName()); return new PageResult<>(Collections.emptyList(),0L); }
            assertNull(page.getName());assertNull(args[1]);return new PageResult<>(Collections.singletonList(metric),1L);
        }));
        inject(tool,"support",new MetricAgentToolSupport(){
            @Override public AgentToolResult guard(AgentToolContext context,String...permissions){return null;}
            @Override public Long currentOntology(AgentToolContext context){return 1L;}
            @Override public void assertOntology(Long id,AgentToolContext context){}
        });
        try(AgentToolContextHolder.Scope ignored=AgentToolContextHolder.scope(new AgentToolContext("op",7L,"owner",8L,"space",Collections.emptyMap()))){
            AgentToolRequest.SearchMetrics input=new AgentToolRequest.SearchMetrics();input.setName("销售记录条数");
            AgentToolResult result=tool.execute(input);assertEquals(AgentToolResult.SUCCEEDED,result.getStatus());
            Map<?,?> data=(Map<?,?>)result.getData();assertEquals(true,data.get("catalogSearch"));assertEquals(true,data.get("broadenedSearch"));
            assertEquals(false,data.get("uniqueMatch"));assertEquals(2,calls.get());
        }
    }

    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
