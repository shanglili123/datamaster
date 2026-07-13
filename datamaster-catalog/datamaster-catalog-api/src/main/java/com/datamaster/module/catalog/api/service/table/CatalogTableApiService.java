package com.datamaster.module.catalog.api.service.table;

import com.datamaster.module.catalog.api.table.dto.CatalogTableRespDTO;

import java.util.List;

/**
 * 元数据信息 API Service 接口。
 */
public interface CatalogTableApiService {

    CatalogTableRespDTO getById(Long id);

    CatalogTableRespDTO getByDatasourceIdAndTableName(Long datasourceId, String tableName);

    List<CatalogTableRespDTO> listByDatasourceId(Long datasourceId);
}
