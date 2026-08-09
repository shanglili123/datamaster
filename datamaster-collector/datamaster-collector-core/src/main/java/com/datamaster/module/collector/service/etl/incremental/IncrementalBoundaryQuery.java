package com.datamaster.module.collector.service.etl.incremental;

import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;

/**
 * Queries an incremental cursor boundary from an external data source.
 */
public interface IncrementalBoundaryQuery {

    /**
     * Returns the maximum non-null value of the requested column.
     */
    Object queryMaxValue(DatasourceRespDTO datasource, String tableName, String columnName);
}
