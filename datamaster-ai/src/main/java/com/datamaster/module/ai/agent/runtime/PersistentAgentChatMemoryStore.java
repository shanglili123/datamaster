package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.datamaster.common.database.utils.AesEncryptUtil;
import lombok.Data;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j ChatMemoryStore 的数据库实现。
 * 会话短期上下文存入平台数据库，服务重启后仍可继续 Agent 调度。
 */
@Component
public class PersistentAgentChatMemoryStore implements ChatMemoryStore {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private volatile boolean tableReady;

    @PostConstruct
    public void initialize() {
        try {
            ensureTable();
        } catch (Exception ignored) {
            // 数据库迁移尚未完成时不阻断应用启动，首次调用时再次尝试创建。
        }
    }

    @Override
    public synchronized List<ChatMessage> getMessages(Object memoryId) {
        if (memoryId == null) return new ArrayList<>();
        ensureTable();
        List<String> rows = jdbcTemplate.query(
                "SELECT messages_json FROM AI_AGENT_CHAT_MEMORY WHERE MEMORY_ID = ?",
                (rs, rowNum) -> rs.getString(1), String.valueOf(memoryId));
        if (rows.isEmpty() || rows.get(0) == null) return new ArrayList<>();
        try {
            String decrypted = AesEncryptUtil.desEncrypt(rows.get(0));
            if (decrypted == null) return new ArrayList<>();
            List<StoredMessage> stored = JSON.parseObject(decrypted, new TypeReference<List<StoredMessage>>() {});
            List<ChatMessage> result = new ArrayList<>();
            if (stored != null) {
                for (StoredMessage message : stored) {
                    ChatMessage restored = restore(message);
                    if (restored != null) result.add(restored);
                }
            }
            return result;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    @Override
    public synchronized void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if (memoryId == null) return;
        ensureTable();
        String id = String.valueOf(memoryId);
        final String json;
        try {
            json = AesEncryptUtil.encrypt(JSON.toJSONString(toStored(messages)));
        } catch (Exception ex) {
            throw new IllegalStateException("Agent 会话记忆加密失败", ex);
        }
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalStateException("Agent 会话记忆加密失败");
        }
        int updated = jdbcTemplate.update(
                "UPDATE AI_AGENT_CHAT_MEMORY SET MESSAGES_JSON = ?, UPDATE_TIME = CURRENT_TIMESTAMP WHERE MEMORY_ID = ?",
                json, id);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO AI_AGENT_CHAT_MEMORY (MEMORY_ID, MESSAGES_JSON, UPDATE_TIME) VALUES (?, ?, CURRENT_TIMESTAMP)",
                    id, json);
        }
    }

    @Override
    public synchronized void deleteMessages(Object memoryId) {
        if (memoryId == null) return;
        ensureTable();
        jdbcTemplate.update("DELETE FROM AI_AGENT_CHAT_MEMORY WHERE MEMORY_ID = ?", String.valueOf(memoryId));
    }

    private void ensureTable() {
        if (tableReady) return;
        synchronized (this) {
            if (tableReady) return;
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS AI_AGENT_CHAT_MEMORY ("
                    + "MEMORY_ID VARCHAR(128) PRIMARY KEY, MESSAGES_JSON TEXT NOT NULL, UPDATE_TIME TIMESTAMP)");
            tableReady = true;
        }
    }

    private List<StoredMessage> toStored(List<ChatMessage> messages) {
        List<StoredMessage> result = new ArrayList<>();
        if (messages == null) return result;
        for (ChatMessage message : messages) {
            if (message == null) continue;
            StoredMessage stored = new StoredMessage();
            stored.type = message.type().name();
            stored.text = message.text();
            if (message instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage tool = (ToolExecutionResultMessage) message;
                stored.id = tool.id();
                stored.name = tool.toolName();
            } else if (message instanceof AiMessage) {
                AiMessage ai = (AiMessage) message;
                if (ai.toolExecutionRequests() != null) {
                    stored.toolRequests = new ArrayList<>();
                    for (ToolExecutionRequest request : ai.toolExecutionRequests()) {
                        Map<String, String> item = new HashMap<>();
                        item.put("id", request.id());
                        item.put("name", request.name());
                        item.put("arguments", request.arguments());
                        stored.toolRequests.add(item);
                    }
                }
            }
            result.add(stored);
        }
        return result;
    }

    private ChatMessage restore(StoredMessage stored) {
        if (stored == null || stored.type == null) return null;
        if ("SYSTEM".equals(stored.type)) return SystemMessage.from(defaultText(stored.text));
        if ("USER".equals(stored.type)) return UserMessage.from(defaultText(stored.text));
        if ("TOOL_EXECUTION_RESULT".equals(stored.type)) {
            return ToolExecutionResultMessage.from(
                    defaultText(stored.id), defaultText(stored.name), defaultText(stored.text));
        }
        if ("AI".equals(stored.type)) {
            List<ToolExecutionRequest> requests = new ArrayList<>();
            if (stored.toolRequests != null) {
                for (Map<String, String> item : stored.toolRequests) {
                    if (item == null) continue;
                    requests.add(ToolExecutionRequest.builder()
                            .id(defaultText(item.get("id")))
                            .name(defaultText(item.get("name")))
                            .arguments(defaultText(item.get("arguments")))
                            .build());
                }
            }
            return requests.isEmpty() ? AiMessage.from(defaultText(stored.text))
                    : AiMessage.from(defaultText(stored.text), requests);
        }
        return null;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    @Data
    public static class StoredMessage {
        private String type;
        private String text;
        private String id;
        private String name;
        private List<Map<String, String>> toolRequests;
    }
}
