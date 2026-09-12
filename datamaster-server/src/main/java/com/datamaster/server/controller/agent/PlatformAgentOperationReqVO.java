package com.datamaster.server.controller.agent;

import lombok.Data;

/**
 * 数据智能体平台运营流程请求。
 * 连接信息只在平台中转发给受控数据源服务，不允许在流程中执行任意 SQL。
 */
@Data
public class PlatformAgentOperationReqVO {
    private String goal;
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
