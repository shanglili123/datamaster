

package com.datamaster.module.taxonomy.api.service.cat;

import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyTaskCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyTaskCatRespDTO;

import java.util.List;

public interface ITaxonomyTaskCatApiService {
    List<TaxonomyTaskCatRespDTO> getAttTaskCatApiList(TaxonomyTaskCatReqDTO TaxonomyTaskCatReqDTO);
}
