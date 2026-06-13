

package com.datamaster.api.ds.api.service.etl;

import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DSExecuteDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessInstance;

import java.util.List;

/**
 * <P>
 * 用途:执行相关相关接口
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-27 14:29
 **/
public interface IDsEtlExecutorService {
    /**
     * 执行命令
     *
     * @param dsExecuteDTO
     * @param projectCode
     * @return
     */
    DsStatusRespDTO execute(DSExecuteDTO dsExecuteDTO, String projectCode);

    /**
     * 查询指定流程定义下的流程实例。
     *
     * @param projectCode           项目编码
     * @param processDefinitionCode 流程定义编码
     * @return 流程实例列表
     */
    List<ProcessInstance> listProcessInstances(String projectCode, String processDefinitionCode);
}
