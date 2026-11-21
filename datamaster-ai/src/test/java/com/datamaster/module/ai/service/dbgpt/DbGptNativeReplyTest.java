package com.datamaster.module.ai.service.dbgpt;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.service.dbgpt.impl.DbGptClientServiceImpl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DbGptNativeReplyTest {
    @Test public void sqlBlockDoesNotEndReplyBeforeAnswerArrives() throws Exception {
        var method = DbGptClientServiceImpl.class.getDeclaredMethod("readResponseBody", HttpURLConnection.class);
        method.setAccessible(true);
        var connection = mock(HttpURLConnection.class);
        String events = "data: {\"content\":\"```sql\\nSELECT 1\\n```\"}\n\n"
                + "data: {\"content\":\"最终答复\"}\n\ndata: [DONE]\n\n";
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(events.getBytes(StandardCharsets.UTF_8)));
        Object body = method.invoke(new DbGptClientServiceImpl(), connection);
        assertTrue(body.toString().contains("最终答复"));
    }

    @Test public void structuredErrorIsNotAReply() throws Exception {
        var method = DbGptClientServiceImpl.class.getDeclaredMethod("parseSseLine", String.class);
        method.setAccessible(true);
        try {
            method.invoke(new DbGptClientServiceImpl(), "data: {\"error_code\":1,\"text\":\"not an answer\"}");
            fail("Expected protocol error");
        } catch (InvocationTargetException error) {
            assertTrue(error.getCause() instanceof ServiceException);
        }
    }
}
