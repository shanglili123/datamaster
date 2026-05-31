package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import javax.sql.DataSource;

/**
 * MongoDB is not a JDBC datasource; this dialect only supplies endpoint
 * formatting so FlinkX configuration can be generated consistently.
 */
public class MongoDBDialect extends UnknownDialect {

    @Override
    public Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty) {
        try (MongoClient client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(trainToJdbcUrl(dbQueryProperty)))
                .build())) {
            client.getDatabase(dbQueryProperty.getDbName()).runCommand(new Document("ping", 1));
            return true;
        }
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        String authDb = property.getSid() == null ? "admin" : property.getSid();
        String dbName = property.getDbName() == null ? "" : property.getDbName();
        String userInfo = "";
        if (property.getUsername() != null && property.getPassword() != null) {
            userInfo = property.getUsername() + ":" + property.getPassword() + "@";
        }
        return "mongodb://" + userInfo + property.getHost() + ":" + property.getPort() + "/" + dbName + "?authSource=" + authDb;
    }
}
