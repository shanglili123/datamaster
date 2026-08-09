

package com.datamaster.module.governance.api.service.cat;

import com.datamaster.module.governance.api.cat.dto.TaxonomyApiCatReqDTO;
import com.datamaster.module.governance.api.cat.dto.TaxonomyApiCatRespDTO;

import java.util.List;

public interface ITaxonomyApiCatApiService {

    /**
     * 获得全部数据服务类目管理列表  服务资源模块使用
     *
     * @return 数据服务类目管理列表
     */
    List<TaxonomyApiCatRespDTO> getAttApiCatList(TaxonomyApiCatReqDTO TaxonomyApiCatReqDTO);
}
