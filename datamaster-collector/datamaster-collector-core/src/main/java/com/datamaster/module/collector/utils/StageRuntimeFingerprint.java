package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.database.utils.MD5Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Fingerprints the generated data job and cursor contract, not its editable canvas metadata. */
public final class StageRuntimeFingerprint {
    private StageRuntimeFingerprint() {
    }

    public static String of(String jobJson, Map<String, Object> incremental) {
        Map<String, Object> semantics = new TreeMap<>();
        semantics.put("job", JSON.parseObject(jobJson));
        semantics.put("incremental", incremental);
        return "JOB_V1:" + MD5Util.encrypt(JSON.toJSONString(canonical(semantics)));
    }

    private static Object canonical(Object value) {
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), canonical(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List) {
            List<Object> ordered = new ArrayList<>();
            for (Object item : (List<?>) value) ordered.add(canonical(item));
            return ordered;
        }
        return value;
    }
}
