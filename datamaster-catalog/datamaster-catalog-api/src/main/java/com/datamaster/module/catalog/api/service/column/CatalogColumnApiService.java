package com.datamaster.module.catalog.api.service.column;


import com.datamaster.module.catalog.api.column.dto.CatalogColumnRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * 元数据字段信息Service接口
 *
 * @author DATAMASTER
 * @date 2025-12-18
 */
public interface CatalogColumnApiService {

    boolean existsByDataElemIds(Collection<Long> dataElemIds);

    /**
     * 根据表id查询字段信息
     * @param tableId
     * @return
     */
    List<CatalogColumnRespDTO> listByTableId(Long tableId);

}
