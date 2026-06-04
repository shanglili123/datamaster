package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.exception.DataQueryException;

import javax.sql.DataSource;

public class ObjectStorageDialect extends UnknownDialect {

    @Override
    public Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty) {
        if (dbQueryProperty.getDatasourceConfig() == null
                || dbQueryProperty.getDatasourceConfig().get("keyId") == null
                || dbQueryProperty.getDatasourceConfig().get("keySecret") == null
                || dbQueryProperty.getDatasourceConfig().get("bucket") == null
                || dbQueryProperty.getDatasourceConfig().get("endpoint") == null) {
            throw new DataQueryException("对象存储配置不完整");
        }
        return true;
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        Object endpoint = property.getDatasourceConfig() == null ? null : property.getDatasourceConfig().get("endpoint");
        Object bucket = property.getDatasourceConfig() == null ? null : property.getDatasourceConfig().get("bucket");
        return "oss://" + bucket + "@" + endpoint;
    }
}
