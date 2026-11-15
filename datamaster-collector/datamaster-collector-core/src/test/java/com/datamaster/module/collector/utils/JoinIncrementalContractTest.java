package com.datamaster.module.collector.utils;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Special flows reuse the same typed window predicate; no source queries or job execution. */
public class JoinIncrementalContractTest {
    @Test public void joinKeepsBothSourcesAndOrsIndependentTimeAndNumericWindows() {
        var a=Map.<String,Object>of("refNodeId","a","alias","a","table_name","source_a","columns",List.of("id","changed"));
        var b=Map.<String,Object>of("refNodeId","b","alias","b","table_name","source_b","columns",List.of("id","changed"));
        Map<String,Object> config=new LinkedHashMap<>();
        config.put("sources",List.of(Map.of("nodeId","a","alias","a"),Map.of("nodeId","b","alias","b")));
        config.put("joins",List.of(Map.of("rightNodeId","b","type","LEFT","relation","AND","conditions",List.of(Map.of("leftNodeId","a","leftColumn","id","rightColumn","id","operator","=")))));
        config.put("fields",List.of(Map.of("nodeId","a","column","id","outputName","id")));
        config.put("sourceFilters",List.of(
                Map.of("nodeId","a","column","changed","cursorType","TIME","start","2026-11-01","end","2026-11-02"),
                Map.of("nodeId","b","column","id","cursorType","ID","start","10","end","20")));
        String sql=JoinQueryBuilder.build(List.of(a,b),config,"PostgreSQL",Map.of("sourceTableColumn","source_table"));
        assertTrue(sql.contains("LEFT JOIN"));assertTrue(sql.contains(" OR "));
        assertTrue(sql.contains("\"a\".\"changed\" >= TIMESTAMP '2026-11-01 00:00:00'"));
        assertTrue(sql.contains("\"b\".\"id\" > 10"));
        assertFalse(sql.contains("FROM (SELECT"));
    }
}
