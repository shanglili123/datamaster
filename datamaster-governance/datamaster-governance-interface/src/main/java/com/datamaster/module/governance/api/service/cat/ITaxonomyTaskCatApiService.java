

package com.datamaster.module.governance.api.service.cat;

import com.datamaster.module.governance.api.cat.dto.TaxonomyTaskCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyTaskCatRespDTO;

import java.util.List;

public interface ITaxonomyTaskCatApiService {
    List<TaxonomyTaskCatRespDTO> getAttTaskCatApiList(TaxonomyTaskCatReqDTO TaxonomyTaskCatReqDTO);
}
