

package com.datamaster.module.taxonomy.api.service.cat.tagRel;

import com.datamaster.module.taxonomy.api.Rel.dto.TaxonomyTagAssetRelReqDTO;
import com.datamaster.module.taxonomy.api.Rel.dto.TaxonomyTagAssetRelRespDTO;

import java.util.List;

public interface ITaxonomyTagAssetRelApiService {

    /**
     * 获得全部类目关联资产信息
     *
     * @return 数据类目管理列表
     */
    List<TaxonomyTagAssetRelRespDTO> getApiList(TaxonomyTagAssetRelReqDTO TaxonomyApiCatReqDTO);
    void deleteRelByUpdateTag(Long assetId);
}
