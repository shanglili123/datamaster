package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.datamaster.metadata.api.lineage.dto.LineageSnapshotDTO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PublishedLineageAssemblerTest {
    @Test
    public void preservesActualColumnCaseAndDatabaseQualifier() {
        Map<String, Object> args = route("db.Source", "db.Target");
        LineageSnapshotDTO snapshot = PublishedLineageAssembler.snapshot(task(), args);
        assertEquals("COMPLETE", snapshot.getCaptureStatus());
        assertEquals("db", snapshot.getUnits().get(0).getSources().get(0).getCatalog());
        assertNull(snapshot.getUnits().get(0).getSources().get(0).getSchema());
        assertEquals("UserID", snapshot.getUnits().get(0).getMappings().get(0).getSourceColumn().getName());
        assertEquals(Boolean.TRUE, snapshot.getUnits().get(0).getMappings().get(0).getSourceColumn().getQuoted());
    }

    @Test
    public void keepsPipelineRoutesIsolated() {
        Map<String, Object> args = new HashMap<>();
        List<Map<String, Object>> units = new ArrayList<>();
        for (String key : Arrays.asList("a", "b")) {
            Map<String, Object> unit = new HashMap<>();
            unit.put("unitKey", key);
            unit.put("args", route(key + "_source", key + "_target"));
            units.add(unit);
        }
        args.put("lineageUnits", units);
        LineageSnapshotDTO snapshot = PublishedLineageAssembler.snapshot(task(), args);
        assertEquals(2, snapshot.getUnits().size());
        assertEquals("a_target", snapshot.getUnits().get(0).getTarget().getTable());
        assertEquals("b_source", snapshot.getUnits().get(1).getSources().get(0).getTable());
        assertNotEquals(snapshot.getUnits().get(0).getUnitKey(), snapshot.getUnits().get(1).getUnitKey());
    }

    @Test
    public void unsupportedExecutableDoesNotClearKnownLineageAsEmpty() {
        LineageSnapshotDTO snapshot = PublishedLineageAssembler.snapshot(task(), Collections.emptyMap());
        assertEquals("FAILED", snapshot.getCaptureStatus());
        assertEquals("NO_PROVABLE_PUBLISHED_ROUTE", snapshot.getErrorCode());
    }

    private CollectorEtlTaskDO task() {
        CollectorEtlTaskDO task = new CollectorEtlTaskDO();
        task.setId(1L); task.setSpaceId(2L); task.setSpaceCode("space"); task.setName("test");
        return task;
    }
    private Map<String, Object> route(String source, String target) {
        Map<String, Object> args = new HashMap<>();
        args.put("reader", component(source));
        args.put("writerList", Collections.singletonList(component(target)));
        args.put("publishedJobJson", "{\"job\":{\"content\":[{\"writer\":{\"parameter\":{\"column\":[\"OutputID\"]}}}]}}");
        return args;
    }
    private Map<String, Object> component(String table) {
        String json = "{\"parameter\":{\"datasourceId\":3,\"dbType\":\"MySql\",\"dbName\":\"db\",\"column\":[\"UserID\"],\"connection\":{\"table\":\"" + table + "\"}}}";
        return JSON.parseObject(json, Map.class);
    }
}
