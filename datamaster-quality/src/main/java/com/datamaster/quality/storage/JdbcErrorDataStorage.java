package com.datamaster.quality.storage;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.quality.controller.quality.vo.CheckErrorDataReqDTO;
import com.datamaster.quality.dal.dataobject.quality.CheckErrorData;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class JdbcErrorDataStorage implements ErrorDataStorage {

    private static final Logger log = LoggerFactory.getLogger(JdbcErrorDataStorage.class);
    private static final String DEFAULT_TABLE_NAME = "quality_error_data";

    private final DataSourceFactory dataSourceFactory;
    private final DbQueryProperty dbQueryProperty;
    private final String tableName;

    private volatile boolean tableReady = false;

    public JdbcErrorDataStorage(DataSourceFactory dataSourceFactory, DbQueryProperty dbQueryProperty) {
        this(dataSourceFactory, dbQueryProperty, DEFAULT_TABLE_NAME);
    }

    public JdbcErrorDataStorage(DataSourceFactory dataSourceFactory, DbQueryProperty dbQueryProperty, String tableName) {
        this.dataSourceFactory = dataSourceFactory;
        this.dbQueryProperty = dbQueryProperty;
        this.tableName = tableName != null ? tableName : DEFAULT_TABLE_NAME;
    }

    public void initTable() {
        if (tableReady) return;
        synchronized (this) {
            if (tableReady) return;
            DbQuery dbQuery = null;
            try {
                dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
                if (!dbQuery.valid()) {
                    throw new RuntimeException("无法连接到错误明细存储数据源：" + dbQueryProperty.getDbType());
                }
                int exists = dbQuery.generateCheckTableExistsSQL(dbQueryProperty, tableName);
                if (exists > 0) {
                    log.info("错误明细表 {} 已存在，跳过建表", tableName);
                    tableReady = true;
                    return;
                }
                List<DbColumn> columns = buildErrorTableColumns();
                List<String> createSqlList = dbQuery.generateCreateTableSQL(dbQueryProperty, tableName,
                        "质量检测错误明细表", columns);
                for (String sql : createSqlList) {
                    log.info("执行建表SQL: {}", sql);
                    dbQuery.execute(sql);
                }
                log.info("错误明细表 {} 创建成功", tableName);
                tableReady = true;
            } catch (Exception e) {
                log.error("初始化错误明细表失败: {}", e.getMessage(), e);
                throw new RuntimeException("初始化错误明细表失败", e);
            } finally {
                if (dbQuery != null) {
                    try { dbQuery.close(); } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public void saveErrorData(String reportId, Integer totalCount, Integer errorCount, List<JSONObject> errorList) {
        if (!tableReady) {
            log.warn("错误明细表未初始化，跳过写入");
            return;
        }
        DbQuery dbQuery = null;
        Connection conn = null;
        try {
            dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
            conn = dbQuery.getConnection();
            String fullTable = buildFullTableName(dbQuery);

            String sql = "INSERT INTO " + fullTable + " (id, report_id, count, error_count, data_json, time, json_data, json_data_old, data_json_old, repair, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (JSONObject obj : errorList) {
                    ps.setString(1, IdUtil.simpleUUID());
                    ps.setString(2, reportId);
                    setInt(ps, 3, totalCount);
                    setInt(ps, 4, errorCount);
                    ps.setString(5, obj.toJSONString());
                    ps.setTimestamp(6, new java.sql.Timestamp(new Date().getTime()));
                    ps.setString(7, obj.toJSONString());
                    ps.setString(8, obj.toJSONString());
                    ps.setString(9, obj.toJSONString());
                    ps.setInt(10, 0);
                    ps.setString(11, "");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception e) {
            log.error("批量写入错误明细失败: {}", e.getMessage(), e);
        } finally {
            if (conn != null) { try { conn.close(); } catch (Exception ignored) {} }
            if (dbQuery != null) { try { dbQuery.close(); } catch (Exception ignored) {} }
        }
    }

    @Override
    public Page<CheckErrorData> pageErrorData(PageRequest pageRequest, CheckErrorDataReqDTO dto) {
        if (!tableReady) return Page.empty();

        DbQuery dbQuery = null;
        try {
            dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
            String fullTable = buildFullTableName(dbQuery);

            Map<String, Object> params = new HashMap<>();
            params.put("reportId", dto.getReportId());

            String where = " WHERE report_id = :reportId ";
            String countSql = "SELECT COUNT(*) FROM " + fullTable + where;

            int total = dbQuery.count(countSql, params);
            if (total == 0) {
                return new PageImpl<>(Collections.emptyList(), pageRequest, 0);
            }

            String querySql = "SELECT * FROM " + fullTable + where + " ORDER BY time DESC";
            var pageResult = dbQuery.queryByPage(querySql, params,
                    pageRequest.getOffset(), pageRequest.getPageSize(), 0);

            List<CheckErrorData> content = pageResult.getRows().stream()
                    .map(this::mapRowToCheckErrorData)
                    .collect(Collectors.toList());

            return new PageImpl<>(content, pageRequest, total);
        } catch (Exception e) {
            log.error("分页查询错误明细失败: {}", e.getMessage(), e);
            return new PageImpl<>(Collections.emptyList(), pageRequest, 0);
        } finally {
            if (dbQuery != null) { try { dbQuery.close(); } catch (Exception ignored) {} }
        }
    }

    @Override
    public boolean updateErrorData(CheckErrorDataReqDTO dto) {
        if (!tableReady || dto.getReportId() == null) return false;
        String updateType = dto.getUpdateType();
        if (updateType == null) return false;

        DbQuery dbQuery = null;
        try {
            dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
            String fullTable = buildFullTableName(dbQuery);

            switch (updateType) {
                case "1": return updateAfterRepair(dbQuery, fullTable, dto);
                case "2": return updateRemark(dbQuery, fullTable, dto);
                case "3": return updateRepairStatus(dbQuery, fullTable, dto);
                default: return false;
            }
        } catch (Exception e) {
            log.error("更新错误明细失败: {}", e.getMessage(), e);
            return false;
        } finally {
            if (dbQuery != null) { try { dbQuery.close(); } catch (Exception ignored) {} }
        }
    }

    @Override
    public boolean isAvailable() {
        return tableReady;
    }

    private boolean updateAfterRepair(DbQuery dbQuery, String fullTable, CheckErrorDataReqDTO dto) {
        if (dto.getUpdatedData() == null || dto.getUpdatedData().isEmpty()) return false;
        Map<String, Object> oldData = dto.getOldData();
        if (oldData == null) return false;
        String id = MapUtils.getString(oldData, "id");
        if (id == null) return false;
        String dataJsonStr = MapUtils.getString(oldData, "dataJsonStr");

        String newDataJson = JSONObject.toJSONString(dto.getUpdatedData());
        String sql = "UPDATE " + fullTable + " SET data_json = '" + escapeSql(newDataJson)
                + "', json_data = '" + escapeSql(newDataJson)
                + "', data_json_old = '" + escapeSql(dataJsonStr)
                + "', json_data_old = '" + escapeSql(dataJsonStr)
                + "', repair = 1 WHERE id = '" + escapeSql(id) + "'";
        return dbQuery.update(sql) > 0;
    }

    private boolean updateRemark(DbQuery dbQuery, String fullTable, CheckErrorDataReqDTO dto) {
        if (dto.getId() == null) return false;
        String remark = dto.getRemark() == null ? "" : dto.getRemark();
        String sql = "UPDATE " + fullTable + " SET remark = '" + escapeSql(remark) + "' WHERE id = '" + escapeSql(dto.getId()) + "'";
        return dbQuery.update(sql) > 0;
    }

    private boolean updateRepairStatus(DbQuery dbQuery, String fullTable, CheckErrorDataReqDTO dto) {
        if (CollectionUtils.isEmpty(dto.getErrorDataId())) return false;
        String ids = dto.getErrorDataId().stream()
                .map(id -> "'" + escapeSql(id) + "'")
                .collect(Collectors.joining(", "));
        String sql = "UPDATE " + fullTable + " SET repair = 2 WHERE id IN (" + ids + ")";
        return dbQuery.update(sql) > 0;
    }

    private CheckErrorData mapRowToCheckErrorData(Map<String, Object> row) {
        CheckErrorData data = new CheckErrorData();
        data.setId(toString(row.get("id")));
        data.setReportId(toString(row.get("report_id")));
        data.setCount(toInteger(row.get("count")));
        data.setErrorCount(toInteger(row.get("error_count")));
        data.setDataJsonStr(toString(row.get("data_json")));
        Object timeObj = row.get("time");
        if (timeObj instanceof Date) {
            data.setTime((Date) timeObj);
        } else if (timeObj instanceof Timestamp) {
            data.setTime(new Date(((Timestamp) timeObj).getTime()));
        }
        String jsonDataStr = toString(row.get("json_data"));
        if (jsonDataStr != null) {
            data.setJsonData(JSONObject.parseObject(jsonDataStr));
        }
        String jsonDataOldStr = toString(row.get("json_data_old"));
        if (jsonDataOldStr != null) {
            data.setJsonDataOld(JSONObject.parseObject(jsonDataOldStr));
        }
        data.setDataJsonStrOLd(toString(row.get("data_json_old")));
        data.setRepair(toInteger(row.get("repair")));
        data.setRemark(toString(row.get("remark")));
        return data;
    }

    private String toString(Object val) {
        return val != null ? val.toString() : null;
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private void setInt(PreparedStatement ps, int index, Integer val) throws SQLException {
        if (val != null) ps.setInt(index, val);
        else ps.setNull(index, Types.INTEGER);
    }

    private static String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }

    private String getOq(String dbType) {
        if (isSqlServer(dbType)) return "[";
        if (isMySqlFamily(dbType)) return "`";
        return "\"";
    }

    private String getCq(String dbType) {
        if (isSqlServer(dbType)) return "]";
        if (isMySqlFamily(dbType)) return "`";
        return "\"";
    }

    private boolean isSqlServer(String dbType) {
        return "SQL_Server".equalsIgnoreCase(dbType) || "SQL_Server2008".equalsIgnoreCase(dbType);
    }

    private boolean isMySqlFamily(String dbType) {
        return "MySql".equalsIgnoreCase(dbType) || "Doris".equalsIgnoreCase(dbType) || "ClickHouse".equalsIgnoreCase(dbType);
    }

    private String buildFullTableName(DbQuery dbQuery) {
        String dbType = dbQueryProperty.getDbType();
        String dbName = dbQueryProperty.getDbName();
        String sid = dbQueryProperty.getSid();

        if (isSqlServer(dbType)) {
            if (org.apache.commons.lang3.StringUtils.isNotBlank(dbName) && org.apache.commons.lang3.StringUtils.isNotBlank(sid)) {
                return dbName + "." + sid + "." + tableName;
            } else if (org.apache.commons.lang3.StringUtils.isNotBlank(dbName)) {
                return dbName + ".." + tableName;
            }
            return tableName;
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(dbName)) {
            if (isMySqlFamily(dbType)) {
                return "`" + dbName + "`.`" + tableName + "`";
            }
            return "\"" + dbName + "\".\"" + tableName + "\"";
        }
        if (isMySqlFamily(dbType)) {
            return "`" + tableName + "`";
        }
        return "\"" + tableName + "\"";
    }

    private List<DbColumn> buildErrorTableColumns() {
        List<DbColumn> columns = new ArrayList<>();
        columns.add(DbColumn.builder().colName("id").dataType("VARCHAR").dataLength("64").colKey(true).nullable(false).colComment("主键").build());
        columns.add(DbColumn.builder().colName("report_id").dataType("VARCHAR").dataLength("64").nullable(false).colComment("报告ID").build());
        columns.add(DbColumn.builder().colName("count").dataType("INTEGER").colComment("总数").build());
        columns.add(DbColumn.builder().colName("error_count").dataType("INTEGER").colComment("错误数").build());
        columns.add(DbColumn.builder().colName("data_json").dataType("TEXT").colComment("错误数据JSON").build());
        columns.add(DbColumn.builder().colName("time").dataType("TIMESTAMP").colComment("核查时间").build());
        columns.add(DbColumn.builder().colName("json_data").dataType("TEXT").colComment("错误数据JSON对象").build());
        columns.add(DbColumn.builder().colName("json_data_old").dataType("TEXT").colComment("旧数据JSON").build());
        columns.add(DbColumn.builder().colName("data_json_old").dataType("TEXT").colComment("旧数据JSON字符串").build());
        columns.add(DbColumn.builder().colName("repair").dataType("SMALLINT").dataDefault("0").colComment("修复状态 0:否 1:是 2:忽略").build());
        columns.add(DbColumn.builder().colName("remark").dataType("TEXT").colComment("备注").build());
        return columns;
    }
}
