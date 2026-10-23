package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;

/** 基于 LangChain4j 的自主 Tool 调度服务。 */
public interface IAutonomousAgentService {

    AgentRunResponse run(AgentRunRequest request, AgentToolContext context);

    AgentRunResponse start(AgentRunRequest request, AgentToolContext context);

    AgentRunResponse get(String operationId, AgentToolContext context);
}
