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
    public List<AiAgentOperationDO> listLatest(Long spaceId, String spaceCode, int limit) {
        return baseMapper.selectLatest(spaceId, spaceCode, limit);
    }

    @Override
    public List<AiAgentOperationDO> listRunning() {
        return baseMapper.selectRunning();
    }

    @Override
    public AiAgentOperationDO findActiveByDatabaseName(String databaseName) {
        return baseMapper.selectActiveByDatabaseName(databaseName);
    }
}
