package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.Assert.*;

public class WriterModeContractTest {
    @Test public void absentAndAppendModesRemainInsertWithoutUpdateKeys() throws Exception {
        for (Object mode : Arrays.asList(null, "2", 2)) {
            JSONObject writer = writer(mode);
            assertEquals("insert", writer.getString("mode"));
            assertEquals("insert", writer.getString("writeMode"));
            assertFalse(writer.containsKey("uniqueKey"));
        }
    }
    @Test public void fullWriteKeepsTruncateAndUsesInsert() throws Exception {
        JSONObject writer = writer("1");
        assertEquals("insert", writer.getString("mode"));
        assertEquals("TRUNCATE TABLE rt_test", writer.getJSONArray("preSql").getString(0));
    }
    @Test public void explicitUpdateEmitsSeparateModeAndExactKeys() throws Exception {
        JSONObject writer = writer("3");
        assertEquals("update", writer.getString("mode"));
        assertEquals(List.of("id"), writer.getJSONArray("uniqueKey").toJavaList(String.class));
    }
    private JSONObject writer(Object mode) throws Exception {
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("dbType", "PostgreSQL"); p.put("writeModeType", mode);
        p.put("column", List.of("id", "name")); p.put("target_column", List.of("id", "name"));
        p.put("selectedColumns", List.of("id"));
        p.put("connection", Map.of("jdbcUrl", "jdbc:postgresql://localhost/test", "table", "rt_test"));
        if ("1".equals(mode)) p.put("preSql", List.of("TRUNCATE TABLE rt_test"));
        Method method = FlinkxEtlTaskConverter.class.getDeclaredMethod("buildWriter", Map.class);
        method.setAccessible(true);
        return ((JSONObject) method.invoke(null, Map.of("parameter", p))).getJSONObject("parameter");
    }
}
