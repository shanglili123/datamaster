package com.datamaster.module.ai.agent.tool;

import com.datamaster.module.ai.agent.runtime.AgentToolFlowHaltException;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.Method;

/** Keep platform stop signals out of model-visible tool results. */
public final class AgentToolExecutors {
    private AgentToolExecutors() { }

    public static ToolExecutor create(Object bean, Method method) {
        DefaultToolExecutor delegate = DefaultToolExecutor.builder()
                .object(bean).originalMethod(method).methodToInvoke(method)
                .propagateToolExecutionExceptions(true)
                .build();
        return (request, memoryId) -> {
            try {
                return delegate.execute(request, memoryId);
            } catch (ToolExecutionException ex) {
                // The SDK wraps reflection failures. Restore our typed control-flow signal
                // so callers retain NEED_INPUT/NEED_CONFIRMATION rather than generic failure.
                if (ex.getCause() instanceof AgentToolFlowHaltException) {
                    throw (AgentToolFlowHaltException) ex.getCause();
                }
                throw ex;
            }
        };
    }
}
