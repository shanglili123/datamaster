

package com.datamaster.api.ds.api.service.project;

import com.datamaster.api.ds.api.project.DsProjectCreateReqDTO;
import com.datamaster.api.ds.api.project.DsProjectDeleteRespDTO;
import com.datamaster.api.ds.api.project.DsProjectRespDTO;
import com.datamaster.api.ds.api.project.DsProjectUpdateReqDTO;
import com.datamaster.api.ds.api.project.DsWorkerGroupRespDTO;
import com.datamaster.api.ds.api.base.DsStatusRespDTO;

/**
 * <P>
 * 用途:ds项目相关接口
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 14:26
 **/
public interface IDsProjectService {

    /**
     * 新增项目
     *
     * @param dsProjectCreateReqDTO
     * @return
     */
    DsProjectRespDTO saveProject(DsProjectCreateReqDTO dsProjectCreateReqDTO);

    /**
     * 修改项目
     *
     * @param dsProjectUpdateReqDTO
     * @return
     */
    DsProjectRespDTO updateProject(DsProjectUpdateReqDTO dsProjectUpdateReqDTO);

    /**
     * 删除项目
     *
     * @param projectCode 项目编码
     * @return
     */
    DsProjectDeleteRespDTO deleteProject(Long projectCode);

    /**
     * 新增工作组
     *
     * @param workerGroupName 工作组名称
     * @return 工作组信息
     */
    DsWorkerGroupRespDTO saveWorkerGroup(String workerGroupName);

    /**
     * 将工作组绑定到项目
     *
     * @param projectCode 项目编码
     * @param workerGroupName 工作组名称
     * @return 绑定结果
     */
    DsStatusRespDTO assignWorkerGroup(Long projectCode, String workerGroupName);

    /**
     * 删除工作组
     *
     * @param workerGroupId 工作组 ID
     * @return 删除结果
     */
    DsStatusRespDTO deleteWorkerGroup(Integer workerGroupId);
}
