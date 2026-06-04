package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.exception.DataQueryException;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;

public class EndpointDialect extends UnknownDialect {

    @Override
    public Boolean validConnection(DataSource dataSource, DbQueryProperty dbQueryProperty) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(dbQueryProperty.getHost(), dbQueryProperty.getPort()), 5000);
            return true;
        } catch (Exception e) {
            throw new DataQueryException("数据源连接失败,稍后重试");
        }
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        return property.getHost() + ":" + property.getPort();
    }
}
