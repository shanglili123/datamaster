package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlCallbackTokenService;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlStageRuntimeService;
import com.datamaster.common.exception.ServiceException;
import org.junit.Test;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Replays the response shape captured from real DS instance 1172, without running a job. */
public class CollectorEtlStageRuntimeControllerTest {
    @Test public void schedulerEnvelopeResolvesExecutionIdAfterTokenCheck() throws Exception {
        var controller = new CollectorEtlStageRuntimeController();
        var runs = mock(CollectorEtlRunMapper.class);
        var tokens = mock(CollectorEtlCallbackTokenService.class);
        var stages = mock(CollectorEtlStageRuntimeService.class);
        inject(controller, "runMapper", runs); inject(controller, "callbackTokens", tokens); inject(controller, "stageService", stages);
        when(runs.lockTask(10L)).thenReturn(10L);
        when(stages.prepareExecution(10L, "stage-a", 1794639552921002L)).thenReturn("{\"job\":{}}");
        assertEquals("{\"job\":{}}", controller.prepare("test-token", 10L, "stage-a",
                "{\"statusCode\":200,\"body\":\"1794639552921002\"}", null, mock(HttpServletResponse.class)));
        var order = inOrder(runs, tokens, stages);
        order.verify(runs).lockTask(10L);
        order.verify(tokens).requireToken(10L, "test-token");
        order.verify(stages).prepareExecution(10L, "stage-a", 1794639552921002L);
    }
    @Test public void failedSchedulerEnvelopeNeverPreparesWindow() throws Exception {
        var controller = new CollectorEtlStageRuntimeController();
        var runs = mock(CollectorEtlRunMapper.class); var tokens = mock(CollectorEtlCallbackTokenService.class);
        var stages = mock(CollectorEtlStageRuntimeService.class);
        inject(controller, "runMapper", runs); inject(controller, "callbackTokens", tokens); inject(controller, "stageService", stages);
        when(runs.lockTask(10L)).thenReturn(10L);
        try {
            controller.prepare("test-token", 10L, "stage-a", "{\"statusCode\":409,\"body\":\"1\"}", null, mock(HttpServletResponse.class));
            fail("failed response must not create a window");
        } catch (ServiceException expected) { verifyNoInteractions(stages); }
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
