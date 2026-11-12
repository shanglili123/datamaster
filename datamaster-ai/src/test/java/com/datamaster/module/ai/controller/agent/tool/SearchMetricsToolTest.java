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
            AgentToolRequest.SearchMetrics request = new AgentToolRequest.SearchMetrics(); request.setName("支付金额"); request.setCode("paid_amount");
            AgentToolResult result = tool.execute(request); assertEquals(AgentToolResult.SUCCEEDED, result.getStatus());
            Map<?, ?> data = (Map<?, ?>) result.getData(); assertEquals(true, data.get("uniqueMatch"));
            assertEquals(true, data.get("searchComplete"));
            Map<?, ?> row = (Map<?, ?>) ((List<?>) data.get("rows")).get(0);
            assertEquals("支付金额", row.get("name")); assertEquals("CNY", row.get("unit"));
            assertFalse(row.containsKey("sourceSnapshot"));
        } finally { AgentToolContextHolder.clear(); }
    }

    private static void inject(Object bean, String name, Object value) throws Exception {
        Field field = bean.getClass().getDeclaredField(name); field.setAccessible(true); field.set(bean, value);
    }
}
