package com.datamaster.ingestion.doris;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.ingestion.config.IngestionProperties;
import com.datamaster.ingestion.model.IngestionResult;
import com.datamaster.ingestion.service.IngestionSchemaValidator;
import com.datamaster.module.ontology.api.IManualEditQueryService;
import com.datamaster.module.ontology.api.dto.ManualEditDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 数据接收核心处理：冲突检查 → 按策略写入数据仓库（Doris）
 *
 * <p>复用 datamaster-common-common 的连接能力（DbQuery / DataSourceFactory / DbQueryProperty），
 * 由数据源元信息（IDatasourceApiService）解析 Doris 数据源连接属性；实际写入使用本服务内的参数化 JDBC，
 * 避免公共 DbQuery 写入方法在部分方言实现中的空操作。</p>
 *
 * <p>冲突策略（datamaster.ingestion.conflict-strategy）：</p>
 * <ul>
 *   <li>INSERT：不论是否存在同键记录，直接插入（追加）</li>
 *   <li>UPSERT：存在同键记录则按主键更新，否则插入（默认）</li>
 *   <li>SKIP：存在同键记录则跳过</li>
 *   <li>DELETE：按消息主键删除目标行</li>
 *   <li>ERROR：存在同键记录则报错（不写入）</li>
 * </ul>
 */
@Slf4j
@Service
public class DorisIngestionService {

    @Resource
    private IDatasourceApiService datasourceApiService;

    @Resource
    private DataSourceFactory dataSourceFactory;

    @Resource
    private IngestionProperties properties;

    /** 人为编辑登记查询（冲突仲裁：按表/主键定位人工编辑） */
    @Resource
    private IManualEditQueryService manualEditQueryService;

    @Resource
    private IngestionSchemaValidator schemaValidator;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    /** 单次仲裁最多拉取的人为编辑条数（防一次拉回整表编辑史） */
    private static final int MAX_MANUAL_EDIT_FETCH = 50;

    /**
     * 处理一条收到的数据。
     *
     * @param topic    来源 Kafka topic
     * @param partition 分区
     * @param offset    偏移量
     * @param record    消息体（JSON）
     * @param targetTableOverride 目标表覆盖（可为空，取配置或消息内 __table）
     * @return 处理结果
     */
    public IngestionResult process(String topic, Integer partition, Long offset, JSONObject record, String targetTableOverride) {
        try {
            Long dsId = properties.getDorisDatasourceId();
            if (dsId == null) {
                return IngestionResult.fail(IngestionResult.Action.ERROR, "未配置 doris-datasource-id");
            }
            DatasourceRespDTO datasource = datasourceApiService.getDatasourceById(dsId);
            if (datasource == null) {
                return IngestionResult.fail(IngestionResult.Action.ERROR, "数据源不存在: " + dsId);
            }
            DbQueryProperty property = getDbQuery(datasource);
            if (property == null) {
                return IngestionResult.fail(IngestionResult.Action.ERROR, "Doris 数据源配置不完整: " + dsId);
            }

            String targetTable = resolveTargetTable(record, targetTableOverride);
            if (targetTable == null || targetTable.trim().isEmpty()) {
                return IngestionResult.fail(IngestionResult.Action.ERROR, "无法解析目标表名");
            }
            String schemaError = schemaValidator.validate(record, targetTable);
            if (schemaError != null) {
                return IngestionResult.fail(IngestionResult.Action.ERROR, schemaError);
            }

            DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
            return doProcess(dbQuery, targetTable, record);
        } catch (Exception e) {
            log.error("数据接收处理失败 topic={} offset={}", topic, offset, e);
            return IngestionResult.fail(IngestionResult.Action.ERROR, "处理异常: " + e.getMessage());
        }
    }

    private IngestionResult doProcess(DbQuery dbQuery, String targetTable, JSONObject record) {
        // 主键列：消息内 __keys 优先，其次配置 key-columns
        List<String> keyCols = resolveKeyColumns(record);
        if (keyCols.isEmpty() && properties.getKeyColumns() != null) {
            keyCols = new ArrayList<>(properties.getKeyColumns());
        }
        if (keyCols.isEmpty()) {
            dbQuery.close();
            return IngestionResult.fail(IngestionResult.Action.ERROR, "未配置冲突判定主键列（key-columns 或消息 __keys）");
        }

        for (String keyCol : keyCols) {
            if (valueForColumn(record, keyCol) == null) {
                dbQuery.close();
                return IngestionResult.fail(IngestionResult.Action.ERROR, "消息缺少主键值: " + keyCol);
            }
        }

        String operation = resolveOperation(record);
        Long spaceId = record.getLong("__spaceId");

        String strategyName = properties.getConflictStrategy() == null
                ? "UPSERT" : properties.getConflictStrategy().trim().toUpperCase(Locale.ROOT);

        // 写入参数字段（去除内部保留字段，如 __table / __keys）
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getKey().startsWith("__")) {
                continue;
            }
            data.put(canonicalColumnName(entry.getKey(), keyCols), entry.getValue());
        }

        try {
            if ("DELETE".equals(operation)) {
                int affected = deleteByKeys(dbQuery, targetTable, keyCols, record);
                return IngestionResult.ok(IngestionResult.Action.DELETE,
                        affected == 0 ? "目标行不存在，无需删除" : "已删除目标行");
            }
            boolean conflict = existsConflict(dbQuery, targetTable, keyCols, record);

            switch (strategyName) {
                case "INSERT":
                    insertRow(dbQuery, targetTable, data);
                    return IngestionResult.ok(IngestionResult.Action.INSERT, "已插入");
                case "UPSERT":
                    if (conflict) {
                        // 人为编辑仲裁：命中登记则按编辑值回写/跳过/告警；未命中按源数据直接更新
                        IngestionResult arbitrated = arbitrateManualEdit(dbQuery, targetTable, keyCols, data,
                                properties.getDorisDatasourceId(), spaceId);
                        if (arbitrated != null) {
                            return arbitrated;
                        }
                        List<String> setCols = new ArrayList<>(data.keySet());
                        setCols.removeAll(keyCols);
                        if (setCols.isEmpty()) {
                            return IngestionResult.ok(IngestionResult.Action.UPSERT, "同键记录已存在且无非键字段可更新");
                        }
                        upsertRow(dbQuery, targetTable, data, setCols, keyCols);
                        return IngestionResult.ok(IngestionResult.Action.UPSERT, "同键记录存在，已更新");
                    }
                    upsertRow(dbQuery, targetTable, data, null, keyCols);
                    return IngestionResult.ok(IngestionResult.Action.UPSERT, "已执行原子 UPSERT");
                case "SKIP":
                    if (conflict) {
                        return IngestionResult.ok(IngestionResult.Action.SKIP, "同键记录已存在，跳过");
                    }
                    insertRow(dbQuery, targetTable, data);
                    return IngestionResult.ok(IngestionResult.Action.INSERT, "新记录已插入");
                case "ERROR":
                    if (conflict) {
                        return IngestionResult.fail(IngestionResult.Action.ERROR, "冲突策略为 ERROR，存在同键记录，拒绝写入");
                    }
                    insertRow(dbQuery, targetTable, data);
                    return IngestionResult.ok(IngestionResult.Action.INSERT, "新记录已插入");
                default:
                    return IngestionResult.fail(IngestionResult.Action.ERROR, "未知冲突策略: " + strategyName);
            }
        } catch (Exception e) {
            log.error("写入目标数仓失败 table=" + targetTable + " strategy=" + strategyName, e);
            return IngestionResult.fail(IngestionResult.Action.ERROR, "写入失败: " + e.getMessage());
        } finally {
            dbQuery.close();
        }
    }

    /**
     * 人为编辑冲突仲裁（UPSERT 冲突且人工编辑登记命中该行时调用）。
     *
     * <p>裁定三态：</p>
     * <ul>
     *   <li>当前库值 == 人为编辑后值 → 编辑已生效，跳过（MANUAL_EDIT_SKIP）</li>
     *   <li>当前库值 == 人为编辑前值（或行缺失）→ 编辑未生效，按编辑值回放（MANUAL_EDIT_APPLIED）</li>
     *   <li>当前库值 != 编辑前、也不 = 编辑后 → 第三值，告警不覆盖（THIRD_VALUE_ALERT）</li>
     * </ul>
     *
     * <p>未命中人为编辑或仲裁异常时返回 null，由调用方按原 UPSERT 逻辑（源数据直接更新）兜底。</p>
     *
     * @return 已裁决的处理结果，或 null（放行给原逻辑）
     */
    private IngestionResult arbitrateManualEdit(DbQuery dbQuery, String targetTable,
                                                List<String> keyCols, Map<String, Object> data,
                                                Long datasourceId, Long spaceId) {
        try {
            // 仅使用内部元数据字段做域隔离，不把 __spaceId 写入业务表。
            // 调用方在消息未携带空间时保持兼容，但此时无法保证同名表跨空间完全隔离。
            List<ManualEditDTO> edits = manualEditQueryService.listLatestManualEdits(
                    targetTable, datasourceId, spaceId, MAX_MANUAL_EDIT_FETCH);
            if (edits == null || edits.isEmpty()) {
                return null;
            }
            // 编辑史中是否存在命中当前主键的记录（决策行=EXECUTED after / ROLLED_BACK rollbackAfter）
            Map<String, Object> decided = findDecisionRow(edits, keyCols, data);
            if (decided == null) {
                return null; // 无人编辑过此行 → 放行原逻辑
            }
            ManualEditDTO hit = lastMatchedEdit(edits, keyCols, data);
            // 编辑前值（EXECUTED before / ROLLED_BACK rollbackBefore）
            Map<String, Object> prior = hit == null ? null : pickRow(
                    "ROLLED_BACK".equals(hit.getStatus()) ? hit.getRollbackBeforeData() : hit.getBeforeData(),
                    keyCols, data);
            // 当前库中行
            Map<String, Object> current = fetchRowByPk(dbQuery, targetTable, keyCols, data);

            Long execId = hit == null ? null : hit.getExecutionId();
            if (current != null && rowMatchesTemplate(decided, current)) {
                return IngestionResult.ok(IngestionResult.Action.MANUAL_EDIT_SKIP,
                        "命中人为编辑(execId=" + execId + ")且编辑值已生效，跳过");
            }
            if (current == null || (prior != null && rowMatchesTemplate(prior, current))) {
                Map<String, Object> merged = mergeDecisionIntoSource(data, prior, decided, keyCols);
                List<String> setCols = new ArrayList<>(merged.keySet());
                setCols.removeAll(keyCols);
                if (setCols.isEmpty()) {
                    return IngestionResult.ok(IngestionResult.Action.MANUAL_EDIT_SKIP, "命中人为编辑后无非键字段变化");
                }
                updateRow(dbQuery, targetTable, merged, setCols, keyCols);
                return IngestionResult.ok(IngestionResult.Action.MANUAL_EDIT_APPLIED,
                        "命中人为编辑(execId=" + execId + ")，按编辑值回放");
            }
            log.warn("冲突仲裁：目标表 [{}] 主键={} 当前库值为第三值，与人为编辑(execId={})前后均不符，不覆盖。"
                            + "当前={}, 编辑前={}, 编辑后={}",
                    targetTable, displayKey(keyCols, data), execId, current, prior, decided);
            return IngestionResult.fail(IngestionResult.Action.THIRD_VALUE_ALERT,
                    "命中人为编辑但当前库值既非编辑前也非编辑后，视为第三值，告警不覆盖(execId=" + execId + ")");
        } catch (Exception e) {
            log.warn("人为编辑仲裁失败，按源数据直接更新: {}", e.getMessage());
            return null;
        }
    }

    /** 在编辑史中定位命中当前主键的决策行（EXECUTED after / ROLLED_BACK rollbackAfter），无则 null */
    private Map<String, Object> findDecisionRow(List<ManualEditDTO> edits, List<String> keyCols, Map<String, Object> data) {
        ManualEditDTO hit = lastMatchedEdit(edits, keyCols, data);
        if (hit == null) {
            return null;
        }
        return pickRow("ROLLED_BACK".equals(hit.getStatus()) ? hit.getRollbackAfterData() : hit.getAfterData(),
                keyCols, data);
    }

    /** 返回编辑史中（倒序）首个命中当前主键的编辑记录 */
    private ManualEditDTO lastMatchedEdit(List<ManualEditDTO> edits, List<String> keyCols, Map<String, Object> data) {
        for (ManualEditDTO edit : edits) {
            String decidedJson = "ROLLED_BACK".equals(edit.getStatus())
                    ? edit.getRollbackAfterData() : edit.getAfterData();
            if (pickRow(decidedJson, keyCols, data) != null) {
                return edit;
            }
        }
        return null;
    }

    /** 从快照 JSON（数组或单对象）中按主键定位命中行 */
    private Map<String, Object> pickRow(String snapshotJson, List<String> keyCols, Map<String, Object> data) {
        List<Map<String, Object>> rows = parseSnapshot(snapshotJson);
        if (rows.isEmpty() || keyCols == null) {
            return null;
        }
        for (Map<String, Object> row : rows) {
            boolean match = true;
            for (String col : keyCols) {
                if (!compareValue(row.get(col), data.get(col))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return row;
            }
        }
        return null;
    }

    /** 解析数据快照 JSON 为行列表（支持数组与单对象） */
    private List<Map<String, Object>> parseSnapshot(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Object o = objectMapper.readValue(json, new TypeReference<Object>() {});
            List<Map<String, Object>> rows = new ArrayList<>();
            if (o instanceof List) {
                for (Object item : (List<?>) o) {
                    if (item instanceof Map) {
                        rows.add((Map<String, Object>) item);
                    }
                }
            } else if (o instanceof Map) {
                rows.add((Map<String, Object>) o);
            }
            return rows;
        } catch (Exception e) {
            log.warn("数据快照解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 字段级合并：以源最新数据为基础，被人工编辑的列取编辑值，其余列保留源值；
     * 源缺失但编辑快照含有的非键列（人工设置的列）也补入，避免丢列。
     */
    private Map<String, Object> mergeDecisionIntoSource(Map<String, Object> data, Map<String, Object> prior,
                                                        Map<String, Object> decided, List<String> keyCols) {
        Map<String, Object> merged = new HashMap<>(data);
        for (Map.Entry<String, Object> e : decided.entrySet()) {
            String col = e.getKey();
            if (keyCols.contains(col)) {
                continue; // 主键只作定位，不入 SET
            }
            boolean edited;
            if (prior == null || !prior.containsKey(col)) {
                edited = true; // 无编辑前值（CREATE/缺快照/新增列）→ 视为编辑列
            } else {
                edited = !compareValue(prior.get(col), e.getValue());
            }
            if (edited) {
                merged.put(col, e.getValue());
            } else if (!data.containsKey(col)) {
                merged.put(col, e.getValue());
            }
        }
        return merged;
    }

    /** 读取目标表当前行（按主键，命名参数查询）；读取失败返回 null */
    private Map<String, Object> fetchRowByPk(DbQuery dbQuery, String targetTable,
                                             List<String> keyCols, Map<String, Object> data) {
        try {
            List<String> where = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < keyCols.size(); i++) {
                String col = keyCols.get(i);
                Object val = data.get(col);
                if (val == null) {
                    return null;
                }
                String p = "p" + i;
                where.add("`" + quoteId(col) + "` = :" + p);
                params.put(p, val);
            }
            String sql = "SELECT * FROM `" + quoteId(targetTable) + "` WHERE " + String.join(" AND ", where);
            List<Map<String, Object>> rows = dbQuery.queryList(sql, params, 0);
            return (rows == null || rows.isEmpty()) ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("读取当前行失败 table={} err={}", targetTable, e.getMessage());
            return null;
        }
    }

    /** template 的所有键需在 actual 中均存在且值相等（值大小宽松比较） */
    private boolean rowMatchesTemplate(Map<String, Object> template, Map<String, Object> actual) {
        if (template == null || actual == null) {
            return false;
        }
        for (Map.Entry<String, Object> e : template.entrySet()) {
            if (!compareValue(e.getValue(), actual.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** 值宽松比较：null 相等；非 null 统一转字符串（去空白）比较，兼容数值/字符串差异 */
    private boolean compareValue(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return String.valueOf(a).trim().equals(String.valueOf(b).trim());
    }

    private String displayKey(List<String> keyCols, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        for (String col : keyCols) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(col).append("=").append(data.get(col));
        }
        return sb.toString();
    }

    /**
     * 冲突检查：按主键列查询目标表，是否存在同键记录（NamedParameterJdbcTemplate 命名参数）。
     */
    private boolean existsConflict(DbQuery dbQuery, String targetTable, List<String> keyCols, JSONObject record) {
        try {
            List<String> where = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < keyCols.size(); i++) {
                String col = keyCols.get(i);
                Object val = valueForColumn(record, col);
                if (val == null) {
                    throw new IllegalArgumentException("消息缺少主键值: " + col);
                }
                String paramName = "p" + i;
                where.add("`" + quoteId(col) + "` = :" + paramName);
                params.put(paramName, val);
            }
            String sql = "SELECT COUNT(1) FROM `" + quoteId(targetTable) + "` WHERE " + String.join(" AND ", where);
            return dbQuery.count(sql, params) > 0;
        } catch (Exception e) {
            log.error("冲突检查失败，拒绝继续写入以避免重复或错误覆盖，table={}", targetTable, e);
            throw new IllegalStateException("冲突检查失败: " + e.getMessage(), e);
        }
    }

    private String quoteId(String name) {
        return name.replace("`", "");
    }

    private Object valueForColumn(JSONObject record, String column) {
        if (record == null || column == null) {
            return null;
        }
        Object direct = record.get(column);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String canonicalColumnName(String column, List<String> keyCols) {
        if (column == null || keyCols == null) {
            return column;
        }
        for (String keyCol : keyCols) {
            if (keyCol != null && keyCol.equalsIgnoreCase(column)) {
                return keyCol;
            }
        }
        return column;
    }

    private String resolveTargetTable(JSONObject record, String override) {
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }
        String inMessage = record.getString("__table");
        if (inMessage != null && !inMessage.trim().isEmpty()) {
            return inMessage.trim();
        }
        return properties.getTargetTable();
    }

    /** 支持常见 CDC 删除标记；业务字段不使用无前缀 op，避免误判。 */
    private String resolveOperation(JSONObject record) {
        String raw = record.getString("__op");
        if (raw == null || raw.trim().isEmpty()) raw = record.getString("__operation");
        if (raw == null || raw.trim().isEmpty()) raw = record.getString("__eventType");
        if (raw == null) return "UPSERT";
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if ("D".equals(value) || "DELETE".equals(value) || "REMOVE".equals(value)) return "DELETE";
        return "UPSERT";
    }

    private int deleteByKeys(DbQuery dbQuery, String targetTable, List<String> keyCols, JSONObject record) throws Exception {
        StringBuilder sql = new StringBuilder("DELETE FROM `")
                .append(quoteId(targetTable)).append("` WHERE ");
        for (int i = 0; i < keyCols.size(); i++) {
            if (i > 0) sql.append(" AND ");
            sql.append('`').append(quoteId(keyCols.get(i))).append("` = ?");
            if (valueForColumn(record, keyCols.get(i)) == null) {
                throw new IllegalArgumentException("删除事件缺少主键值: " + keyCols.get(i));
            }
        }
        Connection connection = dbQuery.getConnection();
        if (connection == null) throw new IllegalStateException("无法获取目标数据源连接");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < keyCols.size(); i++) {
                statement.setObject(i + 1, valueForColumn(record, keyCols.get(i)));
            }
            return statement.executeUpdate();
        }
    }

    /**
     * 直接使用 JDBC 写入。DbQuery 的通用 addTableData/updateTableData 在部分方言实现中是空操作，
     * ingestion 不能依赖它们，否则会出现“返回成功但目标表没有数据”的静默丢写。
     */
    private int insertRow(DbQuery dbQuery, String targetTable, Map<String, Object> data) throws Exception {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("写入数据不能为空");
        }
        List<String> columns = new ArrayList<>(data.keySet());
        StringBuilder sql = new StringBuilder("INSERT INTO `")
                .append(quoteId(targetTable)).append("` (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('`').append(quoteId(columns.get(i))).append('`');
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('?');
        }
        sql.append(')');
        Connection connection = dbQuery.getConnection();
        if (connection == null) throw new IllegalStateException("无法获取目标数据源连接");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < columns.size(); i++) {
                statement.setObject(i + 1, data.get(columns.get(i)));
            }
            return statement.executeUpdate();
        }
    }

    /** 按主键执行参数化 UPDATE，避免拼接业务值并确保接入实际落库。 */
    private int updateRow(DbQuery dbQuery, String targetTable, Map<String, Object> data,
                          List<String> setCols, List<String> keyCols) throws Exception {
        if (setCols == null || setCols.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("UPDATE `")
                .append(quoteId(targetTable)).append("` SET ");
        for (int i = 0; i < setCols.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('`').append(quoteId(setCols.get(i))).append("` = ?");
        }
        sql.append(" WHERE ");
        for (int i = 0; i < keyCols.size(); i++) {
            if (i > 0) sql.append(" AND ");
            String key = keyCols.get(i);
            if (data.get(key) == null) {
                throw new IllegalArgumentException("更新事件缺少主键值: " + key);
            }
            sql.append('`').append(quoteId(key)).append("` = ?");
        }
        Connection connection = dbQuery.getConnection();
        if (connection == null) throw new IllegalStateException("无法获取目标数据源连接");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (String column : setCols) {
                statement.setObject(index++, data.get(column));
            }
            for (String key : keyCols) {
                statement.setObject(index++, data.get(key));
            }
            return statement.executeUpdate();
        }
    }

    /**
     * Doris Unique/Duplicate Key 表的原子 UPSERT。
     * 该语句把“是否存在”和“写入”交给数据库完成，避免并发消费者在 SELECT 后重复 INSERT。
     */
    private int upsertRow(DbQuery dbQuery, String targetTable, Map<String, Object> data,
                          List<String> preferredSetCols, List<String> keyCols) throws Exception {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("写入数据不能为空");
        }
        List<String> columns = new ArrayList<>(data.keySet());
        List<String> setCols = preferredSetCols == null
                ? new ArrayList<>(columns) : new ArrayList<>(preferredSetCols);
        setCols.removeAll(keyCols);
        if (setCols.isEmpty()) {
            // 只有主键时仍执行 INSERT，确保不存在时可以创建记录。
            return insertRow(dbQuery, targetTable, data);
        }
        StringBuilder sql = new StringBuilder("INSERT INTO `")
                .append(quoteId(targetTable)).append("` (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('`').append(quoteId(columns.get(i))).append('`');
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('?');
        }
        sql.append(") ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < setCols.size(); i++) {
            if (i > 0) sql.append(", ");
            String column = setCols.get(i);
            sql.append('`').append(quoteId(column)).append("` = VALUES(`")
                    .append(quoteId(column)).append('`').append(')');
        }
        Connection connection = dbQuery.getConnection();
        if (connection == null) throw new IllegalStateException("无法获取目标数据源连接");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < columns.size(); i++) {
                statement.setObject(i + 1, data.get(columns.get(i)));
            }
            return statement.executeUpdate();
        }
    }

    private List<String> resolveKeyColumns(JSONObject record) {
        List<String> result = new ArrayList<>();
        Object keys = record.get("__keys");
        if (keys == null) {
            return result;
        }
        String raw = String.valueOf(keys).trim();
        if (raw.isEmpty()) {
            return result;
        }
        if (raw.startsWith("[")) {
            try {
                com.alibaba.fastjson2.JSONArray arr = com.alibaba.fastjson2.JSON.parseArray(raw);
                for (int i = 0; i < arr.size(); i++) {
                    String t = String.valueOf(arr.getString(i)).trim();
                    if (!t.isEmpty()) {
                        result.add(t);
                    }
                }
                return result;
            } catch (Exception ignore) {
                // fallthrough to comma split
            }
        }
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    private DbQueryProperty getDbQuery(DatasourceRespDTO datasource) {
        String ip = datasource.getIp();
        Long port = datasource.getPort();
        String config = datasource.getDatasourceConfig();
        if (ip == null || ip.trim().isEmpty() || port == null || config == null || config.trim().isEmpty()) {
            return null;
        }
        return new DbQueryProperty(datasource.getDatasourceType(), ip, port, config);
    }
}
