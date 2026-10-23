package com.datamaster.module.ai.service.agent.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.module.ai.dal.dataobject.agent.AiAgentOperationDO;
import com.datamaster.module.ai.dal.mapper.agent.AiAgentOperationMapper;
import com.datamaster.module.ai.service.agent.IAiAgentOperationService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 数据智能体运营流程持久化服务实现。 */
@Service
public class AiAgentOperationServiceImpl extends ServiceImpl<AiAgentOperationMapper, AiAgentOperationDO>
        implements IAiAgentOperationService {

    @Override
    public void create(AiAgentOperationDO operation) {
        save(operation);
    }

    @Override
    public void update(AiAgentOperationDO operation) {
        updateById(operation);
    }

    @Override
    public AiAgentOperationDO get(String id) {
        return getById(id);
    }

    @Override
    public List<AiAgentOperationDO> listLatest(Long spaceId, String spaceCode, Long creatorId, int limit) {
        return baseMapper.selectLatest(spaceId, spaceCode, creatorId, limit);
    }

    @Override
    public List<AiAgentOperationDO> listRunning() {
        return baseMapper.selectRunning();
    }

    @Override
    public AiAgentOperationDO findActiveByDatabaseName(String databaseName, Long creatorId,
                                                       Long spaceId, String spaceCode) {
        return baseMapper.selectActiveByDatabaseName(databaseName, creatorId, spaceId, spaceCode);
    }

    @Override
    public AiAgentOperationDO findByDatasourceAndSpace(Long datasourceId, Long creatorId,
                                                       Long spaceId, String spaceCode) {
        return baseMapper.selectByDatasourceAndSpace(datasourceId, creatorId, spaceId, spaceCode);
    }

    @Override
    public AiAgentOperationDO findByFlowKey(String flowKey, Long creatorId) {
        return baseMapper.selectByFlowKey(flowKey, creatorId);
    }
}
