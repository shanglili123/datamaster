package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlCallbackTokenService;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlRunGuardService;
import com.datamaster.module.collector.utils.RunAdmissionDiagnostics;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CollectorEtlRunGuardControllerTest {
    @Test public void tokenRejectionHasTaskStepAndCorrelationWithoutCredentials() throws Exception {
        var controller = new CollectorEtlRunGuardController();
        var tasks = mock(CollectorEtlTaskMapper.class);
        var runs = mock(CollectorEtlRunMapper.class);
        var tokens = mock(CollectorEtlCallbackTokenService.class);
        var service = mock(CollectorEtlRunGuardService.class);
        inject(controller, "enabled", true); inject(controller, "taskMapper", tasks);
        inject(controller, "runMapper", runs); inject(controller, "callbackTokens", tokens);
        inject(controller, "runGuardService", service);
        when(tasks.selectById(10L)).thenReturn(new CollectorEtlTaskDO());
        when(runs.lockTask(10L)).thenReturn(10L);
        doThrow(new ServiceException("任务回调凭据无效")).when(tokens).requireToken(10L, "never-log-this-token");
        try {
            controller.prepare("never-log-this-token", 10L);
            fail("must reject");
        } catch (RunAdmissionDiagnostics.Failure failure) {
            assertEquals(Long.valueOf(10), failure.getTaskId());
            assertEquals("TOKEN_CHECK", failure.getStep());
            assertEquals("CALLBACK_CREDENTIAL_REJECTED", failure.getReason());
            var response = controller.denied(failure);
            assertEquals(409, response.getStatusCodeValue());
            assertEquals(failure.getDiagnosticId(), response.getHeaders().getFirst("X-DataMaster-Diagnostic-Id"));
            assertFalse(response.getBody().contains("never-log-this-token"));
            verifyNoInteractions(service);
        }
    }

    @Test public void internalFailureIs500AndKeepsSanitizedCauseStack() {
        var diagnostics = new RunAdmissionDiagnostics(10L);
        diagnostics.schedulerInstance(1190L); diagnostics.step("DS_GUARD_TASK_LOOKUP");
        try {
            diagnostics.execute(() -> { throw new IllegalStateException("password=raw-secret",
                    new IllegalArgumentException("token=raw-token")); });
            fail("must fail");
        } catch (RunAdmissionDiagnostics.Failure failure) {
            assertEquals(500, new CollectorEtlRunGuardController().denied(failure).getStatusCodeValue());
            assertEquals(Long.valueOf(1190), failure.getSchedulerInstanceId());
            StringWriter text = new StringWriter(); failure.printStackTrace(new PrintWriter(text));
            assertFalse(text.toString().contains("raw-secret"));
            assertFalse(text.toString().contains("raw-token"));
            assertTrue(text.toString().contains("java.lang.IllegalStateException"));
            assertTrue(text.toString().contains("java.lang.IllegalArgumentException"));
            assertTrue(failure.getCause().getStackTrace().length > 0);
        }
    }

    @Test public void nestedContextPreservesOriginalFailureAndFiltersUnknownMessages() {
        var diagnostics = new RunAdmissionDiagnostics(10L);
        diagnostics.step("GUARD_NODE_RUNNING_CHECK");
        diagnostics.workflow(100L, 2, 100L, 2, "RUNNING_EXECUTION");
        diagnostics.guard(200L, 1, "DISPATCH");
        try {
            diagnostics.execute(() -> diagnostics.execute(() -> { throw new ServiceException("upstream password=secret"); }));
            fail("must fail");
        } catch (RunAdmissionDiagnostics.Failure failure) {
            assertTrue(failure.isBusinessRejection());
            assertEquals("BUSINESS_REJECTED", failure.getReason());
            assertEquals("GUARD_NODE_RUNNING_CHECK", failure.getStep());
            assertTrue(failure.getObservation().contains("guardState=DISPATCH"));
            assertFalse(failure.getMessage().contains("secret"));
            assertFalse(failure.getCause().getMessage().contains("secret"));
        }
    }

    @Test public void upstreamServiceExceptionIsNotReportedAsBusinessConflict() {
        var diagnostics = new RunAdmissionDiagnostics(10L);
        diagnostics.step("DS_LATEST_LOOKUP");
        try {
            diagnostics.execute(() -> { throw new ServiceException("DolphinScheduler接口请求失败，状态码：503，地址：https://secret"); });
            fail("must fail");
        } catch (RunAdmissionDiagnostics.Failure failure) {
            assertFalse(failure.isBusinessRejection());
            assertEquals("DS_HTTP_STATUS_ERROR", failure.getReason());
            assertEquals(500, new CollectorEtlRunGuardController().denied(failure).getStatusCodeValue());
            assertFalse(failure.getCause().getMessage().contains("secret"));
        }
    }

    @Test public void onlyPendingGuardVisibilityHasRetryable503() {
        for (String state : new String[]{"DISPATCH", "SUBMITTED_SUCCESS", "NOT_VISIBLE", "FAILURE", "STOP", "UNKNOWN"}) {
            var diagnostics = new RunAdmissionDiagnostics(10L);
            diagnostics.step("GUARD_NODE_RUNNING_CHECK"); diagnostics.guard(300L, 1, state);
            try {
                diagnostics.execute(() -> { throw new ServiceException("工作流运行准入节点未在执行"); });
                fail("must reject");
            } catch (RunAdmissionDiagnostics.Failure failure) {
                boolean pending = state.equals("DISPATCH") || state.equals("SUBMITTED_SUCCESS") || state.equals("NOT_VISIBLE");
                var response = new CollectorEtlRunGuardController().denied(failure);
                assertEquals(pending ? 503 : 409, response.getStatusCodeValue());
                assertEquals(Boolean.toString(pending), response.getHeaders().getFirst("X-DataMaster-Admission-Retryable"));
            }
        }
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
}
