package com.datamaster.module.collector.service.etl.impl;

import com.datamaster.api.ds.api.etl.ds.TaskInstance;
import com.datamaster.common.enums.TaskExecutionStatus;
import com.datamaster.module.collector.dal.dataobject.etl.*;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskExtService;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Independent stage completion: no DS, source connection, Spring startup or model calls. */
public class IndependentStageProgressTest {
    @Test public void successfulSiblingCommitsWhileOtherIsRunningOrFailed() throws Exception {
        var service = new CollectorEtlStageRuntimeService();
        var extensions = mock(ICollectorEtlTaskExtService.class);
        var runs = mock(CollectorEtlRunMapper.class);
        var windows = mock(CollectorEtlIncrementalWindowService.class);
        inject(service,"extService",extensions); inject(service,"runMapper",runs); inject(service,"windowService",windows);
        var ext = new CollectorEtlTaskExtDO();
        ext.setFlinkxJobTemplateJson("{\"kind\":\"STAGE_RUNTIME_V1\",\"workflowCode\":\"100\",\"stages\":{\"a\":{\"dsNodeCode\":\"11\",\"configHash\":\"A\"},\"b\":{\"dsNodeCode\":\"12\",\"configHash\":\"B\"}}}");
        when(extensions.getByTaskId(7L)).thenReturn(ext);
        var run = new CollectorEtlRunDO(); run.setId(99L); when(runs.getByWorkflowInstance(7L,88L)).thenReturn(run);
        when(windows.getWindow(7L,"a",99L)).thenReturn(new CollectorEtlIncrementalWindowDO());
        when(windows.getWindow(7L,"b",99L)).thenReturn(new CollectorEtlIncrementalWindowDO());
        var a = node(1L,"11",TaskExecutionStatus.SUCCESS);
        var b = node(2L,"12",TaskExecutionStatus.RUNNING_EXECUTION);
        service.progress(7L,88L,100L,List.of(a,b));
        verify(windows).finish(7L,"a",99L,"A",true);
        verify(windows,never()).finish(7L,"b",99L,"B",false);
        clearInvocations(windows);
        b.setState(TaskExecutionStatus.FAILURE);
        service.finish(7L,88L,100L,false,List.of(a,b));
        verify(windows).finish(7L,"a",99L,"A",true);
        verify(windows).finish(7L,"b",99L,"B",false);
    }
    @Test public void forcedSuccessCannotCommitAndSkippedStageDoesNotInventWindow() throws Exception {
        var service=new CollectorEtlStageRuntimeService();var extensions=mock(ICollectorEtlTaskExtService.class);
        var runs=mock(CollectorEtlRunMapper.class);var windows=mock(CollectorEtlIncrementalWindowService.class);
        inject(service,"extService",extensions);inject(service,"runMapper",runs);inject(service,"windowService",windows);
        var ext=new CollectorEtlTaskExtDO();ext.setFlinkxJobTemplateJson("{\"kind\":\"STAGE_RUNTIME_V1\",\"workflowCode\":\"100\",\"stages\":{\"a\":{\"dsNodeCode\":\"11\",\"configHash\":\"A\"}}}");
        when(extensions.getByTaskId(7L)).thenReturn(ext);var run=new CollectorEtlRunDO();run.setId(99L);when(runs.getByWorkflowInstance(7L,88L)).thenReturn(run);
        service.finish(7L,88L,100L,true,List.of());
        verify(windows,never()).finish(anyLong(),anyString(),anyLong(),anyString(),anyBoolean());
        when(windows.getWindow(7L,"a",99L)).thenReturn(new CollectorEtlIncrementalWindowDO());
        var forced=node(1L,"11",TaskExecutionStatus.FORCED_SUCCESS);
        service.progress(7L,88L,100L,List.of(forced));verify(windows).finish(7L,"a",99L,"A",false);
        try {service.finish(7L,88L,100L,true,List.of(forced));fail("forced success must not advance watermark");}
        catch(com.datamaster.common.exception.ServiceException expected){assertNotNull(expected.getMessage());}
    }

    private TaskInstance node(Long id,String code,TaskExecutionStatus state) {
        var node = new TaskInstance(); node.setId(id); node.setProcessInstanceId(88L); node.setTaskCode(code); node.setState(state); return node;
    }
    private static void inject(Object target,String name,Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
