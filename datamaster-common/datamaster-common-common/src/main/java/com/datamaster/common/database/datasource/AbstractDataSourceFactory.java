

package com.datamaster.common.database.datasource;


import cn.hutool.db.ds.simple.SimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbDialect;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.DialectFactory;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.database.query.AbstractDbQueryFactory;
import com.datamaster.common.database.query.CacheDbQueryFactoryBean;

import javax.sql.DataSource;

public abstract class AbstractDataSourceFactory implements DataSourceFactory {

    @Override
    public DbQuery createDbQuery(DbQueryProperty property) {
        property.viald();
        DbType dbType = DbType.getDbType(property.getDbType());
        DataSource dataSource = null;
        // Non-JDBC sources are validated by their dialects and FlinkX connectors.
        if (isJdbcDataSource(dbType)) {
            dataSource = createDataSource(property);
        }
        DbQuery dbQuery = createDbQuery(property, dataSource, dbType);
        return dbQuery;
    }

    public DbQuery createDbQuery(DbQueryProperty dbQueryProperty, DataSource dataSource, DbType dbType) {
        DbDialect dbDialect = DialectFactory.getDialect(dbType);
        if (dbDialect == null) {
            throw new DataQueryException("该数据库类型正在开发中");
        }
        AbstractDbQueryFactory dbQuery = new CacheDbQueryFactoryBean();
        dbQuery.setDbQueryProperty(dbQueryProperty);
        dbQuery.setDataSource(dataSource);
        dbQuery.setDbDialect(dbDialect);
        if (isJdbcDataSource(dbType)) {
            dbQuery.setJdbcTemplate(new JdbcTemplate(dataSource));
        }
        return dbQuery;
    }

    private boolean isJdbcDataSource(DbType dbType) {
        return !dbType.getDb().equals(DbType.KAFKA.getDb())
                && !dbType.getDb().equals(DbType.MONGODB.getDb())
                && !dbType.getDb().equals(DbType.ELASTICSEARCH.getDb());
    }

    public DataSource createDataSource(DbQueryProperty property) {
        SimpleDataSource dataSource = null;
        if (DbType.SQL_SERVER2008.getDb().equals(property.getDbType()) && com.datamaster.common.utils.StringUtils.startsWith(property.trainToJdbcUrl(), "jdbc:jtds:sqlserver")) {
            dataSource = new SimpleDataSource(property.trainToJdbcUrl(), property.getUsername(), property.getPassword(), "net.sourceforge.jtds.jdbc.Driver");
        } else {
            dataSource = new SimpleDataSource(property.trainToJdbcUrl(), property.getUsername(), property.getPassword());
        }
        return dataSource;
    }

    protected String trainToJdbcUrl(DbQueryProperty property) {
        String url = DbType.getDbType(property.getDbType()).getUrl();
        if (StringUtils.isEmpty(url)) {
            throw new DataQueryException("无效数据库类型!");
        }
        url = url.replace("${host}", property.getHost());
        url = url.replace("${port}", String.valueOf(property.getPort()));
        if (DbType.ORACLE.getDb().equals(property.getDbType()) || DbType.ORACLE_12C.getDb().equals(property.getDbType())) {
            url = url.replace("${sid}", property.getSid());
        } else if (!StringUtils.isEmpty(property.getDbName())) {
            url = url.replace("${dbName}", property.getDbName());
        }
        return url;
    }
}
