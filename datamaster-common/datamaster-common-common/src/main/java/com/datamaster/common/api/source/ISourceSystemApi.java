package com.datamaster.common.api.source;

import com.datamaster.common.api.source.dto.SourceSystemRespDTO;
import java.util.List;

public interface ISourceSystemApi {

    List<SourceSystemRespDTO> getValidSourceSystems();

    List<SourceSystemRespDTO> getValidSourceSystems(Long spaceId);
}
