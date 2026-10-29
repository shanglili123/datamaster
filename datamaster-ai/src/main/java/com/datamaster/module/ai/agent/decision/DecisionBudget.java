package com.datamaster.module.ai.agent.decision;

import com.datamaster.module.ai.agent.runtime.AgentToolFlowHaltException;
import com.datamaster.module.ai.agent.tool.AgentToolResult;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** One budget shared by the coordinator, specialist models and their tools. */
public final class DecisionBudget {
    private final AtomicInteger remaining = new AtomicInteger(24);
    private final long deadline = System.currentTimeMillis() + 240000L;
    private final AtomicBoolean cancelled;
    private final Consumer<String> progress;

    public DecisionBudget(AtomicBoolean cancelled, Consumer<String> progress) {
        this.cancelled = cancelled;
        this.progress = progress;
    }

    public void check() {
        if (cancelled.get() || System.currentTimeMillis() > deadline)
            throw new AgentToolFlowHaltException(AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "决策请求已停止或超过时间限制；已提交的业务执行不会因此撤销", null));
    }

    public void use(String activity) {
        check();
        if (remaining.decrementAndGet() < 0)
            throw new AgentToolFlowHaltException(AgentToolResult.of(AgentToolResult.NEED_INPUT, null,
                    "决策调用预算已用完，请缩小问题范围", null));
        progress.accept(activity);
    }
}
