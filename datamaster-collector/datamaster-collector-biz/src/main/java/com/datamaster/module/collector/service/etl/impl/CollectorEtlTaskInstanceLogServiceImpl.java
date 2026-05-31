

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceLogDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskInstanceLogMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceLogService;

import javax.annotation.Resource;

/**
 * 数据集成任务实例-日志Service业务层处理
 *
 * @author qdata
 * @date 2025-08-05
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlTaskInstanceLogServiceImpl extends ServiceImpl<CollectorEtlTaskInstanceLogMapper, CollectorEtlTaskInstanceLogDO> implements ICollectorEtlTaskInstanceLogService {
    @Resource
    private CollectorEtlTaskInstanceLogMapper CollectorEtlTaskInstanceLogMapper;

    @Override
    public boolean saveOrUpdate(CollectorEtlTaskInstanceLogDO entity) {
        CollectorEtlTaskInstanceLogDO old = this.getOne(Wrappers.lambdaQuery(CollectorEtlTaskInstanceLogDO.class)
                .eq(CollectorEtlTaskInstanceLogDO::getTaskInstanceId, entity.getTaskInstanceId()));
        if (old != null) {
            old.setTm(entity.getTm());
            old.setLogContent(entity.getLogContent());
            return this.update(old, Wrappers.lambdaUpdate(CollectorEtlTaskInstanceLogDO.class)
                    .eq(CollectorEtlTaskInstanceLogDO::getTaskInstanceId, entity.getTaskInstanceId()));
        } else {
            return this.save(entity);
        }
    }

    @Override
    public String getLog(Long taskInstanceId) {
        CollectorEtlTaskInstanceLogDO collectorEtlTaskInstanceLogDO = this.getOne(Wrappers.lambdaQuery(CollectorEtlTaskInstanceLogDO.class)
                .eq(CollectorEtlTaskInstanceLogDO::getTaskInstanceId, taskInstanceId));
        if (collectorEtlTaskInstanceLogDO != null) {
            return collectorEtlTaskInstanceLogDO.getLogContent();
        }
        return null;
    }
}
