package com.datamaster.module.ai.agent.tool;

/**
 * Tool 风险等级。风险等级只描述能力，不代替业务权限和审批校验。
 */
public enum AgentToolRisk {
    READ_ONLY,
    WRITE,
    DESTRUCTIVE
}
