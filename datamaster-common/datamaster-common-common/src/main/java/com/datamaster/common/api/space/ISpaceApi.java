package com.datamaster.common.api.space;

import com.datamaster.common.api.space.dto.SpaceReqDTO;
import com.datamaster.common.api.space.dto.SpaceRespDTO;
import com.datamaster.common.core.page.PageResult;

public interface ISpaceApi {

    Long getSpaceIdBySpaceCode(String spaceCode);

    String getSpaceCodeBySpaceId(Long spaceId);

    String getWorkerGroupBySpaceCode(String spaceCode);

    PageResult<SpaceRespDTO> getSpacePage(SpaceReqDTO pageReqVO);

    /**
     * 判断用户是否属于指定空间。空间上下文来自请求或模型参数时，调用方必须
     * 通过该接口完成成员校验，不能只比较 spaceId/spaceCode 本身。
     */
    boolean hasUserSpace(Long userId, Long spaceId, String spaceCode);
}
