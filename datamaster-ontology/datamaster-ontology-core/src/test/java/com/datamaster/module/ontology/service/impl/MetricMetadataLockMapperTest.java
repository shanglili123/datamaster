package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/** Annotation contract only; real transaction/concurrency acceptance remains a separate check. */
public class MetricMetadataLockMapperTest {
    @Test public void lockInvalidatesPreLockReadsAndCannotBeServedFromCache() throws Exception {
        Method lock = MetricMetadataLockMapper.class.getMethod("lockOntology", Long.class);
        Options options = lock.getAnnotation(Options.class);
        assertNotNull(options);
        assertEquals(Options.FlushCachePolicy.TRUE, options.flushCache());
        assertFalse(options.useCache());
        Select select = lock.getAnnotation(Select.class);
        assertNotNull(select);
        assertTrue(String.join(" ", select.value()).contains("FOR UPDATE"));
    }
}
