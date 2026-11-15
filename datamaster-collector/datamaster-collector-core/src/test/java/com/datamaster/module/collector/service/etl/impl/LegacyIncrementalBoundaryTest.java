package com.datamaster.module.collector.service.etl.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskExtDO;
import com.datamaster.module.collector.utils.model.FlinkxIncrementalConfig;
import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Legacy job builder only; no source database, service startup or model requests. */
public class LegacyIncrementalBoundaryTest {
    @Test public void timeIncludesStartAndDoesNotAlterConfiguredWriter() throws Exception {
        var ext = template(FlinkxIncrementalConfig.TYPE_TIME);
        JSONObject job = build(ext,"2026-11-01 00:00:00","2026-11-02 00:00:00");
        var content=job.getJSONObject("job").getJSONArray("content").getJSONObject(0);
        assertEquals("(enabled = 1) AND (cursor_value >= '2026-11-01 00:00:00' AND cursor_value <= '2026-11-02 00:00:00')",
                content.getJSONObject("reader").getJSONObject("parameter").getString("where"));
        assertEquals("insert",content.getJSONObject("writer").getJSONObject("parameter").getString("writeMode"));
    }
    @Test public void numericExcludesStartAndEmptySourceClosesRead() throws Exception {
        var ext=template(FlinkxIncrementalConfig.TYPE_ID);
        assertTrue(where(build(ext,"10","20")).contains("cursor_value > 10 AND cursor_value <= 20"));
        assertEquals("(enabled = 1) AND (1 = 0)",where(build(ext,"10",null)));
    }
    private CollectorEtlTaskExtDO template(String type) {
        var ext=new CollectorEtlTaskExtDO();ext.setIncrementalType(type);ext.setSourceIncrementColumn("cursor_value");
        ext.setFlinkxJobTemplateJson("{\"job\":{\"content\":[{\"reader\":{\"parameter\":{\"where\":\"enabled = 1\"}},\"writer\":{\"parameter\":{\"writeMode\":\"insert\"}}}]}}");return ext;
    }
    private JSONObject build(CollectorEtlTaskExtDO ext,String start,String end) throws Exception {
        Method method=CollectorEtlIncrementalServiceImpl.class.getDeclaredMethod("buildRuntimeJobJson",CollectorEtlTaskExtDO.class,String.class,String.class);
        method.setAccessible(true);return JSON.parseObject((String)method.invoke(new CollectorEtlIncrementalServiceImpl(),ext,start,end));
    }
    private String where(JSONObject root) {return root.getJSONObject("job").getJSONArray("content").getJSONObject(0).getJSONObject("reader").getJSONObject("parameter").getString("where");}
}
