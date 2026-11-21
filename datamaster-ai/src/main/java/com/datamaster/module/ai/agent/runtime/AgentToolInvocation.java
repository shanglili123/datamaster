package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolContext;
import com.datamaster.module.ai.agent.tool.AgentToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

/** Spring proxy boundary for tools whose implementation is bound to a single turn. */
@Component
public class AgentToolInvocation {
    public AgentToolResult execute(String toolName, AgentToolContext context, String[] permissions,
                                   Map<String, Object> arguments, Supplier<AgentToolResult> body) {
        return body.get();
    }
}
