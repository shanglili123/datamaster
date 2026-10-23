package com.datamaster.module.ai.controller.agent;

import lombok.Data;

/**
 * 数据智能体平台运营流程请求。
 * 连接信息只在平台中转发给受控数据源服务，不允许在流程中执行任意 SQL。
 */
@Data
public class PlatformAgentOperationReqVO {
    private String goal;
    /**
     * 可选的业务操作幂等键。同一空间、数据源下，不同操作应使用不同 key；
     * 未提供时由服务根据 conversationId 或 goal 生成稳定流程键。
     */
    private String operationKey;
    /** 可选会话标识，用于让同一数据源上的多个会话彼此隔离。 */
    private String conversationId;
    private Long datasourceId;
    private String datasourceName;
    private String databaseName;
    private String schemaName;
    private String datasourceType;
    private String datasourceConfig;
    private String ip;
    private Long port;
    private Long spaceId;
    private String spaceCode;
}
