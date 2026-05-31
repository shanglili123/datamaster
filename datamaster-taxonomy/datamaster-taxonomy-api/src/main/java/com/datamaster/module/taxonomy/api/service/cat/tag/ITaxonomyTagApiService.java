

package com.datamaster.module.taxonomy.api.service.cat.tag;

import com.datamaster.module.taxonomy.api.Tag.dto.TaxonomyTagRespDTO;

import java.util.List;

public interface ITaxonomyTagApiService {

    /**
     * 获得全部标签信息
     *
     * @return 数据类目管理列表
     */
    List<TaxonomyTagRespDTO> getApiList();
}
