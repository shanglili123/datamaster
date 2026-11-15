package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.collector.service.etl.incremental.IncrementalBoundaryQuery;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StageTargetBoundaryTest {
    @Test public void renamedTargetCursorIsQueriedUsingPhysicalMapping() throws Exception {
        var service = new CollectorEtlStageRuntimeService(); var query = mock(IncrementalBoundaryQuery.class);
        Field field=CollectorEtlStageRuntimeService.class.getDeclaredField("boundaryQuery");field.setAccessible(true);field.set(service,query);
        var target=new DatasourceRespDTO();
        when(query.queryComponentMaxValue(target,"out_table","target_id")).thenReturn(50L);
        JSONObject config=JSON.parseObject("{\"output\":{\"target_table_name\":\"out_table\",\"columns\":[\"id\",\"name\"],\"target_columns\":[\"target_id\",\"target_name\"]}}");
        var cursors=List.of(JSON.parseObject("{\"nodeId\":\"src\",\"cursorColumn\":\"id\"}"));
        assertEquals("50",invoke(service,config,cursors,target).get("src"));
        verify(query).queryComponentMaxValue(target,"out_table","target_id");
    }
    @Test public void unknownOrMultipleSourceMappingIsNotGuessed() throws Exception {
        var service=new CollectorEtlStageRuntimeService();var query=mock(IncrementalBoundaryQuery.class);
        Field field=CollectorEtlStageRuntimeService.class.getDeclaredField("boundaryQuery");field.setAccessible(true);field.set(service,query);
        var config=JSON.parseObject("{\"output\":{\"table\":\"out_table\",\"columns\":[\"id\"],\"target_columns\":[\"id\"]}}");
        var cursors=List.of(JSON.parseObject("{\"nodeId\":\"a\",\"cursorColumn\":\"id\"}"),JSON.parseObject("{\"nodeId\":\"b\",\"cursorColumn\":\"id\"}"));
        var result=invoke(service,config,cursors,new DatasourceRespDTO());assertTrue(result.containsKey("a"));assertNull(result.get("a"));assertNull(result.get("b"));verifyNoInteractions(query);
    }
    @Test public void transformedAndDuplicateTargetCursorsUseRecordedFallback() throws Exception {
        var service=new CollectorEtlStageRuntimeService();var query=mock(IncrementalBoundaryQuery.class);
        Field field=CollectorEtlStageRuntimeService.class.getDeclaredField("boundaryQuery");field.setAccessible(true);field.set(service,query);
        var cursors=List.of(JSON.parseObject("{\"nodeId\":\"src\",\"cursorColumn\":\"id\"}"));
        var config=JSON.parseObject("{\"transforms\":[{}],\"output\":{\"table\":\"out_table\",\"columns\":[\"id\",\"name\"],\"target_columns\":[\"id\",\"name\"]}}");
        assertNull(invoke(service,config,cursors,new DatasourceRespDTO()).get("src"));
        config.remove("transforms");config.getJSONObject("output").put("target_columns",List.of("id","id"));
        assertNull(invoke(service,config,cursors,new DatasourceRespDTO()).get("src"));verifyNoInteractions(query);
    }

    @SuppressWarnings("unchecked")
    private Map<String,String> invoke(CollectorEtlStageRuntimeService service,JSONObject config,List<JSONObject> cursors,DatasourceRespDTO target) throws Exception {
        Method method=CollectorEtlStageRuntimeService.class.getDeclaredMethod("targetMaxima",JSONObject.class,List.class,DatasourceRespDTO.class);method.setAccessible(true);
        return (Map<String,String>)method.invoke(service,config,cursors,target);
    }
}
