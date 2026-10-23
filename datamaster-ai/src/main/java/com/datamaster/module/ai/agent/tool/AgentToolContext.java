package com.datamaster.module.ai.agent.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 调用 Tool 时的统一上下文。Tool 不应从自然语言中自行猜测操作者和空间。
 */
public class AgentToolContext {

    private final String operationId;
    private final Long userId;
    private final String userName;
    private final Long spaceId;
    private final String spaceCode;
    private final Map<String, Object> attributes;

    public AgentToolContext(String operationId, Long userId, String userName,
                            Long spaceId, String spaceCode, Map<String, Object> attributes) {
        this.operationId = operationId;
        this.userId = userId;
        this.userName = userName;
        this.spaceId = spaceId;
        this.spaceCode = spaceCode;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String getOperationId() {
        return operationId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public String getSpaceCode() {
        return spaceCode;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** 为异步续跑补齐会话标识，同时保留原用户、空间和确认上下文。 */
    public AgentToolContext withConversationId(String conversationId) {
        Map<String, Object> copy = new LinkedHashMap<>(attributes);
        copy.put("conversationId", conversationId);
        return new AgentToolContext(operationId, userId, userName, spaceId, spaceCode, copy);
    }
}
