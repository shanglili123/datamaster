package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StageRuntimeFingerprintTest {
    @Test
    public void ignoresObjectKeyOrderButPreservesDataSemantics() {
        Map<String, Object> cursor = JSON.parseObject("{\"sources\":[{\"nodeId\":\"a\",\"cursorColumn\":\"updated_at\",\"cursorType\":\"TIME\"}]}");
        String a = StageRuntimeFingerprint.of("{\"job\":{\"reader\":1,\"writer\":2}}", cursor);
        String b = StageRuntimeFingerprint.of("{\"job\":{\"writer\":2,\"reader\":1}}", cursor);
        assertEquals(a, b);
        assertFalse(a.equals(StageRuntimeFingerprint.of("{\"job\":{\"reader\":1,\"writer\":3}}", cursor)));
        Map<String, Object> changed = JSON.parseObject("{\"sources\":[{\"nodeId\":\"a\",\"cursorColumn\":\"id\",\"cursorType\":\"ID\"}]}");
        assertFalse(a.equals(StageRuntimeFingerprint.of("{\"job\":{\"reader\":1,\"writer\":2}}", changed)));
    }

    @Test
    public void preservesArrayOrderAndDoesNotMutateInput() {
        Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put("sources", Collections.singletonList(Collections.singletonMap("nodeId", "a")));
        String before = JSON.toJSONString(cursor);
        String a = StageRuntimeFingerprint.of("{\"columns\":[\"a\",\"b\"]}", cursor);
        String b = StageRuntimeFingerprint.of("{\"columns\":[\"b\",\"a\"]}", cursor);
        assertFalse(a.equals(b));
        assertEquals(before, JSON.toJSONString(cursor));
    }
}
