

package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceLogDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlNodeInstanceLogMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeInstanceLogService;

import javax.annotation.Resource;

/**
 * 数据集成节点实例-日志Service业务层处理
 *
 * @author lili.shang
 * @date 2025-08-05
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CollectorEtlNodeInstanceLogServiceImpl extends ServiceImpl<CollectorEtlNodeInstanceLogMapper, CollectorEtlNodeInstanceLogDO> implements ICollectorEtlNodeInstanceLogService {
    @Resource
    private CollectorEtlNodeInstanceLogMapper CollectorEtlNodeInstanceLogMapper;

    @Override
    public String getLog(Long nodeInstanceId) {
        CollectorEtlNodeInstanceLogDO collectorEtlNodeInstanceLogDO = CollectorEtlNodeInstanceLogMapper.selectOne(Wrappers.lambdaQuery(CollectorEtlNodeInstanceLogDO.class)
                .eq(CollectorEtlNodeInstanceLogDO::getNodeInstanceId, nodeInstanceId));
        if (collectorEtlNodeInstanceLogDO != null) {
            return collectorEtlNodeInstanceLogDO.getLogContent();
        }
        return null;
    }
}
