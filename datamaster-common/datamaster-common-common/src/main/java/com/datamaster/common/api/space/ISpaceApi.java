package com.datamaster.common.api.space;

import com.datamaster.common.api.space.dto.SpaceReqDTO;
import com.datamaster.common.api.space.dto.SpaceRespDTO;
import com.datamaster.common.core.page.PageResult;

public interface ISpaceApi {

    Long getSpaceIdBySpaceCode(String spaceCode);

    String getSpaceCodeBySpaceId(Long spaceId);

    String getWorkerGroupBySpaceCode(String spaceCode);

    PageResult<SpaceRespDTO> getSpacePage(SpaceReqDTO pageReqVO);
}
