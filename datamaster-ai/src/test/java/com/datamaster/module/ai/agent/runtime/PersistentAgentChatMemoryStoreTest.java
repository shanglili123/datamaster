package com.datamaster.module.ai.agent.runtime;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.Assert.*;

/** Real SDK messages and encryption, in-memory JDBC only: no database or model requests. */
public class PersistentAgentChatMemoryStoreTest {
    @Test
    public void encryptedMessagesAndToolChainSurviveStoreRecreation() throws Exception {
        MemoryJdbc jdbc = new MemoryJdbc();
        PersistentAgentChatMemoryStore store = store(jdbc);
        ToolExecutionRequest request = ToolExecutionRequest.builder().id("call-1").name("lookup")
                .arguments("{\"id\":\"9007199254740993\"}").build();
        List<ChatMessage> messages = Arrays.asList(SystemMessage.from("系统规则"),
                UserMessage.from("查询精确编号"), AiMessage.from(request),
                ToolExecutionResultMessage.from(request, "{\"amount\":\"123456789.123456789\"}"),
                AiMessage.from("已有结果"));
        store.updateMessages("user:7:space:10", messages);
        String ciphertext = jdbc.rows.get("user:7:space:10");
        assertNotNull(ciphertext);
        assertFalse(ciphertext.contains("查询精确编号"));
        assertFalse(ciphertext.contains("123456789"));
        List<ChatMessage> restored = store(jdbc).getMessages("user:7:space:10");
        assertEquals(5, restored.size());
        assertEquals("系统规则", ((SystemMessage) restored.get(0)).text());
        assertEquals("查询精确编号", ((UserMessage) restored.get(1)).singleText());
        ToolExecutionRequest actual = ((AiMessage) restored.get(2)).toolExecutionRequests().get(0);
        assertEquals(request, actual);
        ToolExecutionResultMessage result = (ToolExecutionResultMessage) restored.get(3);
        assertEquals("call-1", result.id());
        assertEquals("lookup", result.toolName());
        assertEquals("{\"amount\":\"123456789.123456789\"}", result.text());
        assertEquals("已有结果", ((AiMessage) restored.get(4)).text());
    }

    @Test
    public void updateDeleteAndDifferentMemoryIdsStayIsolated() throws Exception {
        MemoryJdbc jdbc = new MemoryJdbc(); PersistentAgentChatMemoryStore store = store(jdbc);
        store.updateMessages("owner-a:space-a", List.of(UserMessage.from("A")));
        store.updateMessages("owner-b:space-b", List.of(UserMessage.from("B")));
        store.updateMessages("owner-a:space-a", List.of(UserMessage.from("A2")));
        assertEquals("A2", ((UserMessage) store.getMessages("owner-a:space-a").get(0)).singleText());
        store.deleteMessages("owner-a:space-a");
        assertTrue(store.getMessages("owner-a:space-a").isEmpty());
        assertEquals("B", ((UserMessage) store.getMessages("owner-b:space-b").get(0)).singleText());
    }

    @Test
    public void unsupportedUserContentsNeverOverwriteExistingMemory() throws Exception {
        MemoryJdbc jdbc = new MemoryJdbc(); PersistentAgentChatMemoryStore store = store(jdbc);
        store.updateMessages("id", List.of(UserMessage.from("original")));
        String original = jdbc.rows.get("id");
        try {
            store.updateMessages("id", List.of(UserMessage.from(TextContent.from("one"), TextContent.from("two"))));
            fail("must reject text-only memory losing content");
        } catch (IllegalStateException expected) {
            assertEquals(original, jdbc.rows.get("id"));
        }
    }

    @Test
    public void missingNullAndCorruptStoredMemoryDoNotReturnUntrustedText() throws Exception {
        MemoryJdbc jdbc = new MemoryJdbc(); PersistentAgentChatMemoryStore store = store(jdbc);
        assertTrue(store.getMessages(null).isEmpty());
        assertTrue(store.getMessages("absent").isEmpty());
        jdbc.rows.put("corrupt", "not-ciphertext");
        assertTrue(store.getMessages("corrupt").isEmpty());
        store.updateMessages(null, List.of(UserMessage.from("not written")));
        assertEquals(1, jdbc.rows.size());
    }

    private PersistentAgentChatMemoryStore store(MemoryJdbc jdbc) throws Exception {
        PersistentAgentChatMemoryStore store = new PersistentAgentChatMemoryStore();
        Field field = PersistentAgentChatMemoryStore.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true); field.set(store, jdbc); return store;
    }

    private static final class MemoryJdbc extends JdbcTemplate {
        private final Map<String, String> rows = new HashMap<>();
        @Override public void execute(String sql) { assertTrue(sql.startsWith("CREATE TABLE IF NOT EXISTS AI_AGENT_CHAT_MEMORY")); }
        @Override public int update(String sql, Object... args) {
            if (sql.startsWith("UPDATE")) {
                String id = String.valueOf(args[1]);
                if (!rows.containsKey(id)) return 0;
                rows.put(id, (String) args[0]); return 1;
            }
            if (sql.startsWith("INSERT")) { rows.put(String.valueOf(args[0]), (String) args[1]); return 1; }
            if (sql.startsWith("DELETE")) return rows.remove(String.valueOf(args[0])) == null ? 0 : 1;
            throw new AssertionError(sql);
        }
        @Override public <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
            String id = String.valueOf(args[0]);
            if (!rows.containsKey(id)) return Collections.emptyList();
            ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, parameters) -> {
                        if (method.getName().equals("getString")) return rows.get(id);
                        throw new AssertionError(method.getName());
                    });
            try { return Collections.singletonList(mapper.mapRow(rs, 0)); }
            catch (java.sql.SQLException e) { throw new AssertionError(e); }
        }
    }
}
