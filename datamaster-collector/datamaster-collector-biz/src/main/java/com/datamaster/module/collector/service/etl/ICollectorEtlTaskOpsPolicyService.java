package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskOpsPolicySaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsPolicyDO;

public interface ICollectorEtlTaskOpsPolicyService extends IService<CollectorEtlTaskOpsPolicyDO> {

    CollectorEtlTaskOpsPolicyDO getByTaskId(Long taskId);

    CollectorEtlTaskOpsPolicyDO saveOrUpdatePolicy(CollectorEtlTaskOpsPolicySaveReqVO reqVO);
}
