

package com.datamaster.module.taxonomy.api.client;

import com.datamaster.module.taxonomy.api.client.dto.TaxonomyClientRespDTO;

/**
 * 应用 API 接口
 *
 * @author Ming
 */
public interface ClientApi {

    /**
     * 获得应用信息
     *
     * @param id 应用编号
     * @return 应用信息
     */
    TaxonomyClientRespDTO getClient(Long id);
}
