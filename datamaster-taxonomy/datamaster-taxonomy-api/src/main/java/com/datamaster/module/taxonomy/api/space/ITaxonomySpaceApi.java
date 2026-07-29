

package com.datamaster.module.taxonomy.api.space;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.api.space.dto.TaxonomySpaceReqDTO;
import com.datamaster.module.taxonomy.api.space.dto.TaxonomySpaceRespDTO;

/**
 * <P>
 * 用途:空间相关接口
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-25 14:31
 **/
public interface ITaxonomySpaceApi {

    /**
     * 根据空间编码获取空间id
     *
     * @param spaceCode
     * @return
     */
    Long getSpaceIdBySpaceCode(String spaceCode);

    /**
     * 根据空间 ID 获取空间编码
     *
     * @param spaceId 空间 ID
     * @return 空间编码
     */
    String getSpaceCodeBySpaceId(Long spaceId);

    /**
     * 根据空间编码获取专属工作组
     *
     * @param spaceCode 空间编码
     * @return 工作组名称
     */
    String getWorkerGroupBySpaceCode(String spaceCode);

    /**
     * 获得空间分页列表
     *
     * @param pageReqVO 分页请求
     * @return 空间分页列表
     */
    PageResult<TaxonomySpaceRespDTO> getSpacePage(TaxonomySpaceReqDTO pageReqVO);
}
