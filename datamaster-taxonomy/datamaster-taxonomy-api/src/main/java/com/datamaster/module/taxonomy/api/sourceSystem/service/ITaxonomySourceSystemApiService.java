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
}