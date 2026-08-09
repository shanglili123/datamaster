

package com.datamaster.module.collector.service.etl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceLogDO;

/**
 * 数据集成任务实例-日志Service接口
 *
 * @author lili.shang
 * @date 2025-08-05
 */
public interface ICollectorEtlTaskInstanceLogService extends IService<CollectorEtlTaskInstanceLogDO> {


    String getLog(Long taskInstanceId);
}
