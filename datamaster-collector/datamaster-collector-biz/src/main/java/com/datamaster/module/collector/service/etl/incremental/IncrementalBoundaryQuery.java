package com.datamaster.module.collector.service.etl.incremental;

import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;

/**
 * Queries an incremental cursor boundary from an external data source.
 */
public interface IncrementalBoundaryQuery {

    /**
     * Returns the maximum non-null value of the requested column.
     */
    Object queryMaxValue(AssetsDatasourceRespDTO datasource, String tableName, String columnName);
}
