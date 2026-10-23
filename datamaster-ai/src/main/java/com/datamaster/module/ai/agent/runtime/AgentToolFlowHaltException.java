package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;

/** 让 AiServices 在 Tool 进入待确认、待补充或失败状态时结束当前回合。 */
public class AgentToolFlowHaltException extends RuntimeException {

    private final AgentToolResult result;

    public AgentToolFlowHaltException(AgentToolResult result) {
        super(result == null ? "Agent Tool 流程已停止" : result.getMessage());
        this.result = result;
    }

    public AgentToolResult getResult() {
        return result;
    }
}
