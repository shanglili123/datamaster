package com.datamaster.module.collector.service.etl;

import com.datamaster.api.ds.api.etl.ds.ProcessInstance;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;

public interface ICollectorEtlTaskOpsService {

    void handleProcessInstanceFinished(ProcessInstance processInstance, CollectorEtlTaskInstanceDO instance);
}
