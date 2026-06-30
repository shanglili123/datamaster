package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsEventDO;

public interface ICollectorEtlTaskOpsEventService extends IService<CollectorEtlTaskOpsEventDO> {

    PageResult<CollectorEtlTaskOpsEventDO> pageByTask(Long taskId, Integer pageNum, Integer pageSize);
}
