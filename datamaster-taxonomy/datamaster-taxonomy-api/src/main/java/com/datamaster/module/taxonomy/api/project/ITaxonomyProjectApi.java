

package com.datamaster.module.taxonomy.api.project;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectReqDTO;
import com.datamaster.module.taxonomy.api.project.dto.TaxonomyProjectRespDTO;

/**
 * <P>
 * 用途:项目相关接口
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-25 14:31
 **/
public interface ITaxonomyProjectApi {

    /**
     * 根据项目编码获取项目id
     *
     * @param projectCode
     * @return
     */
    Long getProjectIdByProjectCode(String projectCode);

    /**
     * 根据项目 ID 获取项目编码
     *
     * @param projectId 项目 ID
     * @return 项目编码
     */
    String getProjectCodeByProjectId(Long projectId);

    /**
     * 根据项目编码获取专属工作组
     *
     * @param projectCode 项目编码
     * @return 工作组名称
     */
    String getWorkerGroupByProjectCode(String projectCode);

    /**
     * 获得项目分页列表
     *
     * @param pageReqVO 分页请求
     * @return 项目分页列表
     */
    PageResult<TaxonomyProjectRespDTO> getAttProjectPage(TaxonomyProjectReqDTO pageReqVO);
}
