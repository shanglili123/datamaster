package com.datamaster.module.ai.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Tool 调用审计。只保存脱敏后的参数摘要和结果摘要，不保存密码等凭据。 */
@Service
public class AgentToolAuditService {

    private static final int MAX_JSON_LENGTH = 20000;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private volatile boolean tableReady;

    @PostConstruct
    public void initialize() {
        try {
            ensureTable();
        } catch (Exception ignored) {
            // 审计表可在数据库迁移后由第一次 Tool 调用再次初始化。
        }
    }

    public void record(String toolName, AgentToolContext context, Map<String, Object> arguments,
                       AgentToolResult result, long durationMs) {
        try {
            ensureTable();
            Map<String, Object> safeArguments = sanitize(arguments);
            String operationId = context == null ? null : context.getOperationId();
            Long userId = context == null ? null : context.getUserId();
            Long spaceId = context == null ? null : context.getSpaceId();
            String spaceCode = context == null ? null : context.getSpaceCode();
            jdbcTemplate.update("INSERT INTO AI_AGENT_TOOL_CALL "
                            + "(ID, OPERATION_ID, TOOL_NAME, USER_ID, SPACE_ID, SPACE_CODE, ARGUMENTS_JSON, STATUS, RESULT_JSON, DURATION_MS, CREATE_TIME) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    UUID.randomUUID().toString(), operationId, toolName, userId, spaceId, spaceCode,
                    limitJson(JSON.toJSONString(safeArguments)), result == null ? AgentToolResult.FAILED : result.getStatus(),
                    limitJson(AgentPayloadSanitizer.sanitizeJson(result)), durationMs);
        } catch (Exception ignored) {
            // 审计表尚未迁移或数据库暂时不可用时，不能覆盖 Tool 的业务结果。
        }
    }

    private void ensureTable() {
        if (tableReady) return;
        synchronized (this) {
            if (tableReady) return;
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS AI_AGENT_TOOL_CALL ("
                    + "ID VARCHAR(64) PRIMARY KEY, OPERATION_ID VARCHAR(64), TOOL_NAME VARCHAR(128) NOT NULL,"
                    + "USER_ID BIGINT, SPACE_ID BIGINT, SPACE_CODE VARCHAR(128), ARGUMENTS_JSON TEXT, STATUS VARCHAR(32), RESULT_JSON TEXT,"
                    + "DURATION_MS BIGINT, CREATE_TIME TIMESTAMP)");
            jdbcTemplate.execute("ALTER TABLE AI_AGENT_TOOL_CALL ADD COLUMN IF NOT EXISTS SPACE_ID BIGINT");
            jdbcTemplate.execute("ALTER TABLE AI_AGENT_TOOL_CALL ADD COLUMN IF NOT EXISTS SPACE_CODE VARCHAR(128)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_AI_AGENT_TOOL_OPERATION "
                    + "ON AI_AGENT_TOOL_CALL (OPERATION_ID, CREATE_TIME)");
            tableReady = true;
        }
    }

    private Map<String, Object> sanitize(Map<String, Object> arguments) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (arguments == null) return result;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            result.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private String limitJson(String value) {
        if (value == null || value.length() <= MAX_JSON_LENGTH) return value;
        return value.substring(0, MAX_JSON_LENGTH) + "...[truncated]";
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeValue(String key, Object value) {
        return AgentPayloadSanitizer.sanitizeValue(key, value);
    }
}
