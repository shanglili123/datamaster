package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Captured DS output replay: publish-time valid JSON is replaced by an untyped response variable. */
public class StagePublishResponseContractTest {
    @Test public void directJobPublishRetainsValidChunjunRoot() {
        String job = "{\"job\":{\"content\":[]}}";
        JSONObject task = JSON.parseArray(TaskConverter.buildEtlTaskDefinitionJsonFlinkx(null,
                "test", "100", 0, job, "{}", "default")).getJSONObject(0);
        JSONObject params = task.getJSONObject("taskParams");
        assertEquals("CHUNJUN", task.getString("taskType"));
        assertNotNull(JSON.parseObject(params.getString("json")).getJSONObject("job"));
    }
    @Test public void currentStageReferenceDoesNotExtractHttpBody() {
        String placeholder = "${stage.response}";
        JSONObject task = JSON.parseArray(TaskConverter.buildEtlTaskDefinitionJsonFlinkx(null,
                "test", "100", 0, placeholder, "{}", "default")).getJSONObject(0);
        String emitted = task.getJSONObject("taskParams").getString("json");
        assertEquals(placeholder, emitted);
        String oldOutput = "{\"job\":{\"content\":[]}}";
        String actualOutput = JSON.toJSONString(java.util.Map.of("statusCode", 200, "body", oldOutput));
        assertNotNull(JSON.parseObject(emitted.replace(placeholder, oldOutput)).getJSONObject("job"));
        JSONObject replaced = JSON.parseObject(emitted.replace(placeholder, actualOutput));
        assertNull("Full response cannot serve as CHUNJUN job root", replaced.getJSONObject("job"));
        assertNotNull(JSON.parseObject(replaced.getString("body")).getJSONObject("job"));
    }
}
