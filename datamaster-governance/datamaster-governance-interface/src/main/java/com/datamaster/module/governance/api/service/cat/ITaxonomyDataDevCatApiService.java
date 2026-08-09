

package com.datamaster.module.governance.api.service.cat;

import com.datamaster.module.governance.api.cat.dto.TaxonomyDataDevCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyDataDevCatRespDTO;

import java.util.List;

public interface ITaxonomyDataDevCatApiService {

    /**
     * 获得全部数据开发类目管理列表
     *
     * @return 数据开发类目管理列表
     */
    List<TaxonomyDataDevCatRespDTO> getAttDataDevCatApiList(TaxonomyDataDevCatReqDTO TaxonomyDataDevCatReqDTO);
}
