package com.datamaster.module.ai.agent.tool;

/** A model-proposed time restriction needs a quote from this turn, not the system clock. */
public final class AskTimeScope {
    public static final String GOAL = "askGoal";

    private AskTimeScope() { }

    public static AgentToolResult validate(AgentToolContext context, boolean restricted, String quote) {
        if (!restricted || context == null || !context.getAttributes().containsKey(GOAL)) return null;
        Object goal = context.getAttributes().get(GOAL);
        // A quote establishes provenance of the expression, not semantic equivalence to its parsed bounds.
        if (goal instanceof String text && quote != null && !quote.isBlank() && text.contains(quote.trim())) return null;
        return AgentToolResult.recoverableInput(
                "新增时间条件须引用原问题中的时间要求。未要求时间限制时，不要加时间条件；若这个指标因此不适用，可用其他受控能力。",
                context.getOperationId());
    }
}
