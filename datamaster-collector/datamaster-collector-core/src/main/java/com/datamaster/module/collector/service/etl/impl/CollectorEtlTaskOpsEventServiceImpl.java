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
        if (taskId == null) throw new com.datamaster.common.exception.ServiceException("运维事件查询必须指定任务");
        Page<CollectorEtlTaskOpsEventDO> page = new Page<CollectorEtlTaskOpsEventDO>(
                pageNum == null ? 1 : Math.max(1, pageNum),
                pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize)));
        IPage<CollectorEtlTaskOpsEventDO> result = page(page, Wrappers.lambdaQuery(CollectorEtlTaskOpsEventDO.class)
                .eq(taskId != null, CollectorEtlTaskOpsEventDO::getTaskId, taskId)
                .orderByDesc(CollectorEtlTaskOpsEventDO::getCreateTime));
        for (CollectorEtlTaskOpsEventDO event : result.getRecords()) {
            if (CollectorComponentRecoveryService.ACTION.equals(event.getAction())) {
                com.alibaba.fastjson2.JSONObject meta = CollectorComponentRecoveryService.recovery(event);
                event.setRecoveryAttempt(meta.getInteger("attempt"));
                if (meta.getLongValue("nextRetryAt") > 0 && "WAITING".equals(event.getActionStatus())) event.setNextRetryAt(new java.util.Date(meta.getLongValue("nextRetryAt")));
                event.setRecoveryExecutionId(meta.getString("executionId"));
                event.setLogExcerpt(null);
            }
        }
        return new PageResult<CollectorEtlTaskOpsEventDO>(result.getRecords(), result.getTotal());
    }
}
