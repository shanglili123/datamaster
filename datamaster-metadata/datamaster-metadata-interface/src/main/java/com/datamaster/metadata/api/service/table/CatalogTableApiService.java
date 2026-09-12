package com.datamaster.metadata.api.service.table;

import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;

import java.util.List;

/**
 * 元数据信息 API Service 接口。
 */
public interface CatalogTableApiService {

    CatalogTableRespDTO getById(Long id);

    CatalogTableRespDTO getByDatasourceIdAndTableName(Long datasourceId, String tableName);

    List<CatalogTableRespDTO> listByDatasourceId(Long datasourceId);

    List<CatalogTableRespDTO> listByDatasourceId(Long datasourceId, Long spaceId, String spaceCode);

    List<CatalogTableRespDTO> listByDatasourceAndDatabase(Long datasourceId, String databaseName, String schemaName);

    List<CatalogTableRespDTO> listByDatasourceAndDatabase(Long datasourceId, String databaseName, String schemaName,
                                                           Long spaceId, String spaceCode);
}
