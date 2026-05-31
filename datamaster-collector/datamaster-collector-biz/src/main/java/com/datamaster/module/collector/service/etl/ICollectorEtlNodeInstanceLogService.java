

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceLogDO;

/**
 * 数据集成节点实例-日志Service接口
 *
 * @author qdata
 * @date 2025-08-05
 */
public interface ICollectorEtlNodeInstanceLogService extends IService<CollectorEtlNodeInstanceLogDO> {


    String getLog(Long nodeInstanceId);
}
