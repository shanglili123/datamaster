

package com.datamaster.api.ds.api.service.etl;

import com.datamaster.api.ds.api.base.DsStatusRespDTO;
import com.datamaster.api.ds.api.etl.DsStartTaskReqDTO;
import com.datamaster.api.ds.api.etl.DsTaskSaveReqDTO;
import com.datamaster.api.ds.api.etl.DsTaskSaveRespDTO;
import com.datamaster.api.ds.api.etl.ds.ProcessDefinition;

/**
 * <P>
 * 用途:ds数据集成任务相关接口
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 16:47
 **/
public interface IDsEtlTaskService {
    /**
     * 创建任务
     *
     * @param dsTaskSaveReqDTO
     * @param projectCode      项目编码
     * @return
     */
    DsTaskSaveRespDTO createTask(DsTaskSaveReqDTO dsTaskSaveReqDTO, Long projectCode);

    /**
     * 更新任务
     *
     * @param dsTaskSaveReqDTO
     * @param projectCode      项目编码
     * @param taskCode         任务编码
     * @return
     */
    DsTaskSaveRespDTO updateTask(DsTaskSaveReqDTO dsTaskSaveReqDTO, String projectCode, String taskCode);

    /**
     * 按名称查询流程定义。
     *
     * @param projectCode 项目编码
     * @param name        流程定义名称
     * @return 流程定义，不存在时返回 null
     */
    ProcessDefinition getTaskByName(String projectCode, String name);

    /**
     * 发布或下线任务
     *
     * @param releaseState releaseState 状态 ONLINE：上线 OFFLINE：下线
     * @param projectCode  项目编码
     * @param code         任务编码
     * @return 注：上线后需将调度也上线，下线时接口会处理调度同时进行下线
     */
    DsStatusRespDTO releaseTask(String releaseState, String projectCode, String code);


    /**
     * 删除任务
     *
     * @param projectCode 项目编码
     * @param code        任务编码
     * @return 注：只有下线的任务才能删除
     */
    DsStatusRespDTO deleteTask(String projectCode, String code);

    /**
     * 启动任务
     *
     * @param dsStartTaskReqDTO
     * @param projectCode      项目编码
     * @return
     */
    DsStatusRespDTO startTask(DsStartTaskReqDTO dsStartTaskReqDTO, String projectCode);


    /**
     *
     * @param code
     * @param projectCode
     * @return
     */
    DsTaskSaveRespDTO batchCopy(String code, String projectCode);

}
