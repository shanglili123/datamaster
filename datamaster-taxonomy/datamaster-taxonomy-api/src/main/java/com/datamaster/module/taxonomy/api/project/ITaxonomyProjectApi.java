

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
     * 获得项目分页列表
     *
     * @param pageReqVO 分页请求
     * @return 项目分页列表
     */
    PageResult<TaxonomyProjectRespDTO> getAttProjectPage(TaxonomyProjectReqDTO pageReqVO);
}
