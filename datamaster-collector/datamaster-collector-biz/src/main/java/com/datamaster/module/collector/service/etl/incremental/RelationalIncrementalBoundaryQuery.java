package com.datamaster.module.collector.service.etl.incremental;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Relational implementation. Other data source families can provide their own
 * {@link IncrementalBoundaryQuery} implementation later.
 */
@Slf4j
@Service
public class RelationalIncrementalBoundaryQuery implements IncrementalBoundaryQuery {

    private static final int MAX_RETRY_TIMES = 3;
    private static final Pattern SQL_IDENTIFIER = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_$#]*(\\.[A-Za-z_][A-Za-z0-9_$#]*)*");
    private static final Set<DbType> SUPPORTED_TYPES = EnumSet.of(
            DbType.MYSQL, DbType.MARIADB, DbType.ORACLE, DbType.ORACLE_12C,
            DbType.POSTGRE_SQL, DbType.SQL_SERVER2008, DbType.SQL_SERVER,
            DbType.DM8, DbType.KINGBASE8, DbType.PHOENIX, DbType.DORIS,
            DbType.CLICK_HOUSE, DbType.OSCAR, DbType.DB2);

    @Resource
    private DataSourceFactory dataSourceFactory;

    @Override
    public Object queryMaxValue(AssetsDatasourceRespDTO datasource, String tableName, String columnName) {
        validateDatasource(datasource);
        validateIdentifier("表名", tableName);
        validateIdentifier("字段名", columnName);

        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_TIMES; attempt++) {
            DbQuery dbQuery = null;
            try {
                DbQueryProperty property = new DbQueryProperty(
                        datasource.getDatasourceType(), datasource.getIp(),
                        datasource.getPort(), datasource.getDatasourceConfig());
                dbQuery = dataSourceFactory.createDbQuery(property);
                List<Map<String, Object>> rows = dbQuery.queryList(
                        "SELECT MAX(" + columnName + ") AS MAX_VALUE FROM " + tableName);
                return firstValue(rows);
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("查询增量最大值失败，第 {}/{} 次，datasourceId={}，table={}，column={}，原因={}",
                        attempt, MAX_RETRY_TIMES, datasource.getId(), tableName, columnName, e.getMessage());
            } finally {
                if (dbQuery != null) {
                    try {
                        dbQuery.close();
                    } catch (Exception closeException) {
                        log.warn("关闭增量查询数据源失败，datasourceId={}", datasource.getId(), closeException);
                    }
                }
            }
        }
        throw new ServiceException("查询增量最大值失败，已重试3次: "
                + (lastException == null ? "未知错误" : lastException.getMessage()));
    }

    private Object firstValue(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).isEmpty()) {
            return null;
        }
        return rows.get(0).values().iterator().next();
    }

    private void validateDatasource(AssetsDatasourceRespDTO datasource) {
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        DbType dbType;
        try {
            dbType = DbType.getDbType(datasource.getDatasourceType());
        } catch (RuntimeException e) {
            throw new ServiceException("当前增量同步暂不支持数据源类型: " + datasource.getDatasourceType());
        }
        if (!SUPPORTED_TYPES.contains(dbType)) {
            throw new ServiceException("当前增量同步暂不支持数据源类型: " + datasource.getDatasourceType());
        }
    }

    private void validateIdentifier(String label, String identifier) {
        if (identifier == null || !SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new ServiceException(label + "不合法: " + identifier);
        }
    }
}
