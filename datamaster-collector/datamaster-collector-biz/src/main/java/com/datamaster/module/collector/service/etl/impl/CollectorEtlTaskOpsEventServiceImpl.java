package com.datamaster.module.collector.service.etl.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsEventDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskOpsEventMapper;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsEventService;
import org.springframework.stereotype.Service;

@Service
public class CollectorEtlTaskOpsEventServiceImpl extends ServiceImpl<CollectorEtlTaskOpsEventMapper, CollectorEtlTaskOpsEventDO>
        implements ICollectorEtlTaskOpsEventService {

    @Override
    public PageResult<CollectorEtlTaskOpsEventDO> pageByTask(Long taskId, Integer pageNum, Integer pageSize) {
        Page<CollectorEtlTaskOpsEventDO> page = new Page<CollectorEtlTaskOpsEventDO>(
                pageNum == null ? 1 : pageNum,
                pageSize == null ? 10 : pageSize);
        IPage<CollectorEtlTaskOpsEventDO> result = page(page, Wrappers.lambdaQuery(CollectorEtlTaskOpsEventDO.class)
                .eq(taskId != null, CollectorEtlTaskOpsEventDO::getTaskId, taskId)
                .orderByDesc(CollectorEtlTaskOpsEventDO::getCreateTime));
        return new PageResult<CollectorEtlTaskOpsEventDO>(result.getRecords(), result.getTotal());
    }
}
