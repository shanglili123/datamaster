

package com.datamaster.module.taxonomy.api.service.cat;

import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyDocCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyDocCatRespDTO;

import java.util.List;

public interface ITaxonomyDocCatApiService {

    /**
     * 获得全部数据资产文档类目管理列表         服务资源模块使用
     *
     * @return 数据资产文档类目管理列表
     */
    List<TaxonomyDocCatRespDTO> getAttDocCatList(TaxonomyDocCatReqDTO reqDTO);
}
