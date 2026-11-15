package com.datamaster.module.collector.utils;

import com.datamaster.common.exception.ServiceException;
import org.junit.Test;
import static org.junit.Assert.*;

public class StageRuntimeScriptTest {
    private static final String TOKEN = "a".repeat(43);
    @Test public void jobResponseIsAFileNotADsResponseVariableOrExecutableText() {
        String script = StageRuntimeScript.prepare("http://192.168.93.175:8989/prepare/10", TOKEN, "stage-1");
        assertTrue(script.contains("--output \"$work/job.json\""));
        assertTrue(script.contains("-job \"$work/job.json\""));
        assertTrue(script.contains("schedulerInstanceId=${system.workflow.instance.id}"));
        assertTrue(script.contains("--config \"$work/headers\""));
        assertFalse(script.contains(".response}")); assertFalse(script.contains("eval "));
        assertTrue(script.contains("set +x")); assertTrue(script.contains("umask 077"));
    }
    @Test public void guardConsumesRawExecutionBodyAndDoesNotPrintIt() {
        String script = StageRuntimeScript.guard("http://192.168.93.175:8989/guard/10", TOKEN);
        assertTrue(script.contains("--request POST")); assertTrue(script.contains("$work/execution"));
        assertFalse(script.contains(".response}"));
        assertTrue(script.contains("for attempt in 1 2 3 4 5"));
        assertTrue(script.contains("X-DataMaster-Admission-Retryable:"));
        assertTrue(script.contains("\"$status\" != 503"));
        assertFalse(script.contains("\"$status\" != 409"));
        assertTrue(script.contains("--dump-header \"$work/response-headers\""));
    }
    @Test public void invalidTokenUrlAndStageNeverBecomeShellCommands() {
        for (String stage : new String[]{"$(touch bad)", "a'b", "a\nb", ""}) {
            try { StageRuntimeScript.prepare("http://localhost/prepare", TOKEN, stage); fail(); }
            catch (ServiceException expected) { assertNotNull(expected.getMessage()); }
        }
        try { StageRuntimeScript.guard("file:///tmp/job", TOKEN); fail(); } catch (ServiceException expected) { }
        try { StageRuntimeScript.guard("http://localhost/job", "bad'key"); fail(); } catch (ServiceException expected) { }
    }
}
