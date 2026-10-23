package com.datamaster.module.ai.agent.runtime;

import lombok.Data;

/** 自主 Agent 调度请求。 */
@Data
public class AgentRunRequest {

    private String goal;
    private String operationId;
    private String conversationId;
    /** 统一 AI 会话表中的会话 ID；自主 Agent 与 AI 问数共用会话管理。 */
    private Long sessionId;
    private Long spaceId;
    private String spaceCode;
    private Integer maxTurns;

    /** 用户确认的唯一动作执行记录 ID，不能用一次确认授权其他动作。 */
    private String confirmedExecutionId;

    /** 用户确认的唯一数据源资源 ID，用于删除等高风险数据源 Tool。 */
    private String confirmedDatasourceId;

    /** 用户确认的唯一数据服务 ID，用于发布数据服务 Tool。 */
    private String confirmedServiceId;

    /** 用户明确点击确认后才允许继续执行无资源绑定的写操作 Tool。 */
    private Boolean confirmed;

    /** 内部续跑标记，不由模型生成。 */
    private Boolean continuation;
}
