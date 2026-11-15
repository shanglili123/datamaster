package com.datamaster.module.collector.utils;

import com.datamaster.common.exception.ServiceException;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchedulerHttpResponseTest {
    @Test public void acceptsPlainAndSuccessfulSchedulerEnvelopeWithoutPrecisionLoss() {
        assertEquals(Long.valueOf(1794639552921002L), SchedulerHttpResponse.executionId("1794639552921002"));
        assertEquals(Long.valueOf(1794639552921002L), SchedulerHttpResponse.executionId("{\"statusCode\":200,\"body\":\"1794639552921002\"}"));
        assertEquals(Long.valueOf(Long.MAX_VALUE), SchedulerHttpResponse.executionId("9223372036854775807"));
    }
    @Test public void rejectsFailedMalformedOversizedAndAmbiguousResponses() {
        for (String raw : new String[]{"", "0", "-1", "1.0", "9223372036854775808", "${guard.response}",
                "{\"statusCode\":409,\"body\":\"1\"}", "{\"statusCode\":200,\"body\":1}",
                "{\"statusCode\":\"200\",\"body\":\"1\"}", "{\"statusCode\":200,\"body\":\"1\",\"other\":true}",
                "{\"statusCode\":200,\"body\":\"{\\\"id\\\":1}\"}", "x".repeat(1025)}) {
            try { SchedulerHttpResponse.executionId(raw); fail("must reject"); }
            catch (ServiceException expected) { assertEquals("运行执行ID或调度HTTP响应无效", expected.getMessage()); }
        }
    }
}
