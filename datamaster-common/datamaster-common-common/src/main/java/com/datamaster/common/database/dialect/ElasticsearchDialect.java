package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;

import javax.sql.DataSource;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Elasticsearch is configured as an HTTP endpoint for FlinkX.
 */
public class ElasticsearchDialect extends UnknownDialect {

    @Override
    public Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(trainToJdbcUrl(dbQueryProperty)).openConnection();
            conn.setRequestMethod("GET");
            if (dbQueryProperty.getUsername() != null && dbQueryProperty.getPassword() != null) {
                String token = dbQueryProperty.getUsername() + ":" + dbQueryProperty.getPassword();
                conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(token.getBytes("UTF-8")));
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() < 400;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        return "http://" + property.getHost() + ":" + property.getPort();
    }
}
