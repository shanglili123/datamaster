package com.datamaster.module.taxonomy.api.sourceSystem.service;

import com.datamaster.module.taxonomy.api.sourceSystem.dto.TaxonomySourceSystemRespDTO;

import java.util.List;

/**
 * 来源系统 API 服务接口
 *
 * @author DATAMASTER
 * @date 2026-04-30
 */
public interface ITaxonomySourceSystemApiService {

    /**
     * 获取所有有效的来源系统列表
     *
     * @return 有效来源系统列表
     */
    List<TaxonomySourceSystemRespDTO> getValidSourceSystems();

    /**
     * 获取指定空间下的有效来源系统列表，包含未绑定空间的通用来源系统。
     *
     * @param spaceId 空间ID
     * @return 有效来源系统列表
     */
    List<TaxonomySourceSystemRespDTO> getValidSourceSystems(Long spaceId);
}
