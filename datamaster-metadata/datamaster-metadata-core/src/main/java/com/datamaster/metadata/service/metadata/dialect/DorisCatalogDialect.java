package com.datamaster.metadata.service.metadata.dialect;

import com.datamaster.metadata.dal.dataobject.metadata.CatalogDbDO;

public class DorisCatalogDialect extends MySqlDialect {

    @Override
    public String getStorageEngine(CatalogDbDO CatalogDbDO) {
        return "Doris";
    }

    @Override
    public TableMetadata getTableMetadata(CatalogDbDO CatalogDbDO, String tableName) {
        TableMetadata metadata = super.getTableMetadata(CatalogDbDO, tableName);
        if (metadata.getStorageEngine() == null || metadata.getStorageEngine().isEmpty()) {
            metadata.setStorageEngine("Doris");
        }
        return metadata;
    }
}
