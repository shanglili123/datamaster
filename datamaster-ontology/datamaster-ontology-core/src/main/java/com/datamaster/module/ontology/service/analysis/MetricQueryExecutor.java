package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.analysis.vo.*;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.service.metric.MetricRules;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Synchronous, bounded, request-local results. Does not reuse arbitrary-SQL execution helpers. */
@Component
public class MetricQueryExecutor {
    private final DataSourceFactory factory;
    private final MetricQueryPolicy policy;
    public MetricQueryExecutor(DataSourceFactory factory, MetricQueryPolicy policy) {
        this.factory = factory; this.policy = policy;
    }
    public MetricQueryRespVO execute(MetricRespVO snapshot, MetricQueryBuilder.Plan plan, DbQueryProperty property) {
        policy.assertBinding(snapshot.getConceptTableId());
        require("DORIS".equalsIgnoreCase(property.getDbType()), "仅支持Doris数据源");
        policy.assertReadonlyUsername(property.getUsername());
        require(plan.getDatabaseName().equals(property.getDbName()), "指标数据库不匹配");
        require(policy.tryAcquire(), "指标查询繁忙，请稍后重试");
        long started = System.nanoTime();
        java.util.Date executedAt = new java.util.Date();
        try {
            DbQuery query = null;
            try {
                query = factory.createDbQuery(property);
                require(query != null, "来源查询不可用");
                try (Connection connection = query.getConnection()) {
                    require(connection != null, "来源连接不可用");
                    // Fail closed when unsupported; account-side SELECT-only grants are still mandatory.
                    connection.setReadOnly(true);
                    require(connection.isReadOnly(), "来源驱动未启用只读模式");
                    try (PreparedStatement statement = connection.prepareStatement(plan.getSql())) {
                        statement.setQueryTimeout(policy.getTimeoutSeconds());
                        statement.setMaxRows(plan.getRequest().getLimit() + 1);
                        for (int i = 0; i < plan.getParameters().size(); i++) statement.setObject(i + 1, plan.getParameters().get(i));
                        try (ResultSet resultSet = statement.executeQuery()) {
                            ResultSetMetaData metadata = resultSet.getMetaData();
                            require(metadata.getColumnCount() == plan.getColumns().size(), "来源返回字段不一致");
                            for (int i = 0; i < plan.getColumns().size(); i++) {
                                require(plan.getColumns().get(i).getKey().equalsIgnoreCase(metadata.getColumnLabel(i + 1)), "来源返回字段不一致");
                            }
                            List<Map<String, Object>> rows = new ArrayList<>();
                            int limit = plan.getRequest().getLimit();
                            while (rows.size() <= limit && resultSet.next()) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                for (int i = 0; i < plan.getColumns().size(); i++) {
                                    MetricQueryColumnVO column = plan.getColumns().get(i);
                                    row.put(column.getKey(), read(resultSet, i + 1, column.getDataType()));
                                }
                                rows.add(row);
                            }
                            boolean truncated = rows.size() > limit;
                            if (truncated) rows.remove(rows.size() - 1);
                            MetricQueryRespVO result = new MetricQueryRespVO();
                            result.setMetricId(snapshot.getId()); result.setVersion(plan.getRequest().getVersion());
                            result.setMetricName(snapshot.getName()); result.setUnit(snapshot.getUnit());
                            result.setColumns(plan.getColumns()); result.setRows(rows); result.setTruncated(truncated);
                            result.setLimit(limit); result.setExecutedAt(executedAt);
                            result.setElapsedMs((System.nanoTime() - started) / 1000000L);
                            result.setNotice("仅执行明确发布版本；固定口径与用户条件AND；时间范围为[start,end)，不自动分桶或变换粒度。访问隔离依赖预先批准的只读ADS视图和账号，并非全量行权限引擎。");
                            return result;
                        }
                    }
                }
            } finally {
                if (query != null) query.close();
            }
        } catch (Exception ex) {
            // No SQL, filter values, connection configuration, credentials, or raw driver errors in response/logs.
            throw new ServiceException("受控指标查询失败，请联系管理员核对安全数据源及治理配置");
        } finally { policy.release(); }
    }
    private static Object read(ResultSet result, int index, String type) throws SQLException {
        if (MetricRules.numeric(type)) {
            java.math.BigDecimal value = result.getBigDecimal(index);
            return value == null ? null : value.toPlainString();
        }
        if ("boolean".equalsIgnoreCase(type)) {
            boolean value = result.getBoolean(index); return result.wasNull() ? null : value;
        }
        if ("date".equalsIgnoreCase(type)) {
            java.sql.Date value = result.getDate(index); return value == null ? null : value.toLocalDate().toString();
        }
        if (MetricRules.temporal(type)) {
            Timestamp value = result.getTimestamp(index); return value == null ? null : value.toLocalDateTime().toString();
        }
        return result.getString(index);
    }
}
