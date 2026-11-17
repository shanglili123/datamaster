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
    private final MetricSourceApprovalService approvals;
    public MetricQueryExecutor(DataSourceFactory factory, MetricQueryPolicy policy, MetricSourceApprovalService approvals) {
        this.factory = factory; this.policy = policy; this.approvals = approvals;
    }
    public MetricQueryRespVO execute(MetricRespVO snapshot, MetricQueryBuilder.Plan plan, MetricSourceApprovalService.ExecutionContext execution) {
        policy.assertEnabled();
        require(execution != null, "缺少指标来源准入上下文");
        DbQueryProperty property = execution.getProperty();
        require(MetricSqlDialect.forSource(snapshot).sameType(property.getDbType()), "指标数据源类型不匹配");
        require(plan.getDatabaseName().equals(property.getDbName()), "指标数据库不匹配");
        require(policy.tryAcquire(), "指标查询繁忙，请稍后重试");
        long started = System.nanoTime();
        java.util.Date executedAt = new java.util.Date();
        try {
            // Current committed admission must still match immediately before any external connection.
            approvals.recheck(execution, snapshot);
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
                            if (Boolean.TRUE.equals(plan.getRequest().getRequireComplete()) && truncated) throw new CompleteResultLimitException();
                            if (truncated) rows.remove(rows.size() - 1);
                            MetricQueryRespVO result = new MetricQueryRespVO();
                            result.setMetricId(snapshot.getId()); result.setVersion(plan.getRequest().getVersion());
                            result.setMetricName(snapshot.getName()); result.setUnit(snapshot.getUnit());
                            result.setPageNum(plan.getRequest().getPageNum()); result.setHasMore(truncated);
                            result.setColumns(plan.getColumns()); result.setRows(rows); result.setTruncated(truncated || plan.getRequest().getPageNum() > 1);
                            result.setLimit(limit); result.setExecutedAt(executedAt);
                            result.setElapsedMs((System.nanoTime() - started) / 1000000L);
                            result.setSourceTimeOffset(snapshot.getSourceTimeOffset()); result.setBusinessTimeOffset(snapshot.getBusinessTimeOffset());
                            result.setTimeBucket(plan.getRequest().getTimeBucket());
                            result.setSortBy(plan.getRequest().getSortBy()); result.setSortDirection(plan.getRequest().getSortDirection());
                            result.setNotice("仅执行明确发布版本；固定口径与用户条件AND。" + MetricTimeRules.description(snapshot) + "；起止时间按业务口径输入，范围为[start,end)，配置偏移时转换为来源时间；固定/附加时间字段过滤按原物理字段口径。分桶先按已发布偏移转换再在来源数据库完整聚合排序，未满桶仅统计所选区间、不补零。"
                                    + (truncated || plan.getRequest().getPageNum() > 1 ? "当前仅为有界分页/前N组，不能代表完整分组；页间查询不冻结源数据，不返回估算总数。" : "当前分组未截断。")
                                    + "访问隔离仍依赖已批准的只读来源和账号，并非全量行权限引擎。");
                            MetricTrendRules.completeCalendar(plan.getRequest(), result);
                            return result;
                        }
                    }
                }
            } finally {
                if (query != null) query.close();
            }
        } catch (CompleteResultLimitException ex) {
            throw new ServiceException("完整结果超出上限，请缩小区间或使用更粗时间粒度");
        } catch (Exception ex) {
            // No SQL, filter values, connection configuration, credentials, or raw driver errors in response/logs.
            throw new ServiceException("受控指标查询失败，请联系管理员核对安全数据源及治理配置");
        } finally { policy.release(); }
    }
    private static final class CompleteResultLimitException extends RuntimeException { }
    private static Object read(ResultSet result, int index, String type) throws SQLException {
        if (MetricRules.numeric(type)) {
            java.math.BigDecimal value = result.getBigDecimal(index);
            require(value == null || value.precision() <= 1000 && Math.abs((long) value.scale()) <= 1000, "来源数值超出安全返回范围");
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
