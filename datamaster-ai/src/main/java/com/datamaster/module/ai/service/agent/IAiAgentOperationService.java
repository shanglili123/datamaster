package com.datamaster.module.ai.service.agent;

import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;

import java.util.List;

/** 数据智能体运营流程持久化服务。 */
public interface IAiAgentOperationService {

    void create(AiAgentOperationDO operation);

    void update(AiAgentOperationDO operation);

    AiAgentOperationDO get(String id);

    List<AiAgentOperationDO> listLatest(Long spaceId, String spaceCode, Long creatorId, int limit);

    List<AiAgentOperationDO> listRunning();

    AiAgentOperationDO findActiveByDatabaseName(String databaseName, Long creatorId,
                                                 Long spaceId, String spaceCode);

    AiAgentOperationDO findByDatasourceAndSpace(Long datasourceId, Long creatorId,
                                                 Long spaceId, String spaceCode);

    AiAgentOperationDO findByFlowKey(String flowKey, Long creatorId);
}
