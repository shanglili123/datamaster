package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.api.ds.api.service.etl.IDsEtlTaskService;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import com.datamaster.module.collector.utils.RunAdmissionDiagnostics;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RunAdmissionDiagnosticsServiceTest {
    @Test public void missingRunningGuardKeepsSelectedInstanceAndNeverCreatesOwner() throws Exception {
        var service = new CollectorEtlRunGuardService();
        var runs = mock(CollectorEtlRunMapper.class);
        var tasks = mock(CollectorEtlTaskMapper.class);
        var extensions = mock(ICollectorEtlTaskExtService.class);
        var ds = mock(IDsEtlTaskService.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        inject(service, "transactionManager", transactions); inject(service, "enabled", true);
        inject(service, "runMapper", runs); inject(service, "taskMapper", tasks);
        inject(service, "extService", extensions); inject(service, "dsTaskService", ds);
        when(runs.lockTask(10L)).thenReturn(10L);
        var task = new CollectorEtlTaskDO(); task.setStatus("1"); task.setSpaceCode("100");
        when(tasks.selectById(10L)).thenReturn(task);
        var ext = new CollectorEtlTaskExtDO(); ext.setEtlTaskCode("200");
        ext.setFlinkxJobTemplateJson("{\"kind\":\"STAGE_RUNTIME_V1\",\"workflowCode\":200,\"workflowVersion\":2,\"guardNodeCode\":300,\"resourcesJson\":\"[]\"}");
        when(extensions.getByTaskId(10L)).thenReturn(ext);
        var remote = JSONObject.parseObject("{\"id\":1190,\"workflowDefinitionCode\":200,\"workflowDefinitionVersion\":2,\"state\":\"RUNNING_EXECUTION\"}");
        when(ds.getLatestWorkflowInstance("100", "200")).thenReturn(remote);
        when(ds.getWorkflowInstance("100", 1190L)).thenReturn(remote);
        when(ds.listTaskInstancesStrict("100", 1190L)).thenReturn(Collections.emptyList());
        try {
            service.prepareLatestComponent(10L, "[]"); fail("must reject missing running guard");
        } catch (RunAdmissionDiagnostics.Failure failure) {
            assertEquals("GUARD_NODE_RUNNING_CHECK", failure.getStep());
            assertEquals("GUARD_NODE_NOT_RUNNING", failure.getReason());
            assertEquals(Long.valueOf(1190), failure.getSchedulerInstanceId());
            assertTrue(failure.getObservation().contains("guardState=NOT_VISIBLE"));
            verify(runs, never()).insertIfAbsent(any());
        }
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
}
