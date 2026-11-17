package com.datamaster.module.ai.agent.runtime;

import org.junit.Test;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.junit.Assert.*;

public class AgentToolFailureDiagnosticsTest {
    @Test public void businessReasonsAreExactWhitelistedAndNotArbitraryMessages() {
        var known = AgentToolFailureDiagnostics.capture(new com.datamaster.common.exception.ServiceException("请缩小分析范围"));
        assertEquals("ANALYSIS_INPUT_OR_LIMIT", known.getReason());
        var unknown = AgentToolFailureDiagnostics.capture(new com.datamaster.common.exception.ServiceException("credential=never-log"));
        assertEquals("BUSINESS_REJECTED", unknown.getReason());
        assertFalse(unknown.getStack().getMessage().contains("never-log"));
    }

    @Test public void preservesCauseTypesAndFramesButNeverRawMessages() {
        Throwable original = new IllegalStateException("apiKey=secret model arguments",
                new IllegalArgumentException("password=hidden SQL SELECT"));
        var diagnostic = AgentToolFailureDiagnostics.capture(original);
        assertEquals("TOOL_ARGUMENT_INVALID", diagnostic.getCategory());
        assertEquals("java.lang.IllegalArgumentException", diagnostic.getExceptionType());
        assertTrue(diagnostic.getId().matches("[0-9a-f-]{36}"));
        StringWriter text = new StringWriter(); diagnostic.getStack().printStackTrace(new PrintWriter(text));
        assertTrue(text.toString().contains("java.lang.IllegalStateException"));
        assertTrue(text.toString().contains("AgentToolFailureDiagnosticsTest"));
        assertFalse(text.toString().contains("secret")); assertFalse(text.toString().contains("password"));
        assertFalse(text.toString().contains("SQL SELECT"));
    }
}
