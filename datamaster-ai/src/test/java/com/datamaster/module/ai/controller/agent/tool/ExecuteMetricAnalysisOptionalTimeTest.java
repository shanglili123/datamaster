package com.datamaster.module.ai.controller.agent.tool;

import com.datamaster.module.ai.agent.runtime.AgentToolContextHolder;
import com.datamaster.module.ai.agent.tool.*;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.IMetricAnalysisService;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ExecuteMetricAnalysisOptionalTimeTest {
    @Test public void emptyModelDatesMeanAbsentWhileHalfRangeNeverExecutes() throws Exception {
        var tool = new ExecuteMetricAnalysisTool();
        var service = mock(IMetricAnalysisService.class);
        var support = mock(MetricAgentToolSupport.class);
        inject(tool,"metricAnalysisService",service); inject(tool,"support",support);
        var context = new AgentToolContext("op",1L,"test",1L,"space",Collections.emptyMap());
        when(support.positiveId("10")).thenReturn(10L);
        when(support.positiveVersion("1")).thenReturn(1);
        when(support.wasOffered(10L,1,context)).thenReturn(true);
        var metric = new MetricRespVO(); metric.setOntologyId(1L); metric.setConceptId(2L);
        when(support.published(10L,1,context)).thenReturn(metric);
        when(support.result(anyString(),any(),anyString(),eq(context))).thenAnswer(call ->
                AgentToolResult.of(call.getArgument(0),call.getArgument(1),call.getArgument(2),"op"));
        when(service.query(any())).thenAnswer(call -> {
            MetricQueryReqVO query = call.getArgument(0);
            assertNull(query.getStartTime()); assertNull(query.getEndTime());
            assertEquals(Integer.valueOf(1), query.getLimit());
            var response = new MetricQueryRespVO(); response.setRows(Collections.emptyList()); return response;
        });
        try (var ignored=AgentToolContextHolder.scope(context)) {
            var input = new AgentToolRequest.ExecuteMetricAnalysis();input.setMetricId("10");input.setVersion("1");
            input.setStartTime("");input.setEndTime("  ");
            for (Integer limit : java.util.Arrays.asList(null, 0, 101)) {
                input.setLimit(limit);
                assertEquals(AgentToolResult.SUCCEEDED, tool.execute(input).getStatus());
            }
            verify(service,times(3)).query(any());
            input.setStartTime("2026-11-01T00:00:00");
            var incomplete = tool.execute(input);
            assertEquals(AgentToolResult.NEED_INPUT, incomplete.getStatus());
            assertTrue(incomplete.mayRetryInput());
            input.setEndTime("2026-12-01T00:00:00");
            var unsupported = tool.execute(input);
            assertEquals(AgentToolResult.NEED_INPUT, unsupported.getStatus());
            assertTrue(unsupported.mayRetryInput());
            metric.setTimePropertyId(4L);
            when(support.semanticProperty(metric, 4L)).thenReturn(java.util.Map.of("dataType", "date"));
            var wrongFormat = tool.execute(input);
            assertTrue(wrongFormat.mayRetryInput());
            assertTrue(wrongFormat.getMessage().contains("YYYY-MM-DD"));
            input.setStartTime("2026-12-01");
            input.setEndTime("2026-11-01");
            assertTrue(tool.execute(input).mayRetryInput());

            input.setStartTime(null);
            input.setEndTime(null);
            input.setDimensionPropertyIds(java.util.List.of("3"));
            when(support.positiveId("3")).thenReturn(3L);
            var invalidGroupLimit = tool.execute(input);
            assertTrue(invalidGroupLimit.mayRetryInput());
            verify(service,times(3)).query(any());
        }
    }
    @Test public void inventedDayIsRejectedButUnrestrictedCountExecutes() throws Exception {
        var tool = new ExecuteMetricAnalysisTool();
        var service = mock(IMetricAnalysisService.class);
        var support = mock(MetricAgentToolSupport.class);
        inject(tool, "metricAnalysisService", service);
        inject(tool, "support", support);
        var context = new AgentToolContext("op", 1L, "test", 1L, "space",
                java.util.Map.of(AskTimeScope.GOAL, "统计下销售记录条数"));
        when(support.positiveId("10")).thenReturn(10L);
        when(support.positiveVersion("1")).thenReturn(1);
        when(support.wasOffered(eq(10L), eq(1), any(AgentToolContext.class))).thenReturn(true);
        var metric = new MetricRespVO();
        metric.setTimePropertyId(4L);
        when(support.published(eq(10L), eq(1), any(AgentToolContext.class))).thenReturn(metric);
        when(support.semanticProperty(metric, 4L)).thenReturn(java.util.Map.of("dataType", "date"));
        when(support.filters(isNull())).thenReturn(Collections.emptyList());
        when(support.result(anyString(), any(), anyString(), any(AgentToolContext.class))).thenAnswer(call ->
                AgentToolResult.of(call.getArgument(0), call.getArgument(1), call.getArgument(2), "op"));
        when(service.query(any())).thenReturn(new MetricQueryRespVO());
        try (var ignored = AgentToolContextHolder.scope(context)) {
            var input = new AgentToolRequest.ExecuteMetricAnalysis();
            input.setMetricId("10");
            input.setVersion("1");
            input.setStartTime("2026-11-19");
            input.setEndTime("2026-11-20");
            input.setTimeScopeQuote("今天");
            assertTrue(tool.execute(input).mayRetryInput());
            verifyNoInteractions(service);

            input.setStartTime(null);
            input.setEndTime(null);
            input.setTimeScopeQuote(null);
            assertEquals(AgentToolResult.SUCCEEDED, tool.execute(input).getStatus());
            verify(service).query(argThat(query -> query.getStartTime() == null && query.getEndTime() == null));
        }
        var datedContext = new AgentToolContext("dated", 1L, "test", 1L, "space",
                java.util.Map.of(AskTimeScope.GOAL, "统计 2026 年 11 月的销售记录条数"));
        try (var ignored = AgentToolContextHolder.scope(datedContext)) {
            var input = new AgentToolRequest.ExecuteMetricAnalysis();
            input.setMetricId("10");
            input.setVersion("1");
            input.setStartTime("2026-11-01");
            input.setEndTime("2026-12-01");
            input.setTimeScopeQuote("2026 年 11 月");
            assertEquals(AgentToolResult.SUCCEEDED, tool.execute(input).getStatus());
            verify(service).query(argThat(query -> query != null && "2026-11-01".equals(query.getStartTime())
                    && "2026-12-01".equals(query.getEndTime())));
        }
    }

    private static void inject(Object bean,String name,Object value)throws Exception {
        Field field=bean.getClass().getDeclaredField(name);field.setAccessible(true);field.set(bean,value);
    }
}
