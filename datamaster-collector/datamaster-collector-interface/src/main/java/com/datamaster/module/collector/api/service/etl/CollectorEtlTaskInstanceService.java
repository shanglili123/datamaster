

package com.datamaster.module.collector.api.service.etl;

import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceRespDTO;

import java.util.List;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-29 10:01
 **/
public interface CollectorEtlTaskInstanceService {
    /**
     * 获取最新的任务实例
     * @param taskIdList
     * @return
     */
    List<CollectorEtlTaskInstanceRespDTO> getLastTaskInstance(List<Long> taskIdList);
}
