package com.datamaster.ingestion.doris;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.ingestion.config.IngestionProperties;
import com.datamaster.ingestion.model.IngestionResult;
import com.datamaster.module.ontology.api.IManualEditQueryService;
import com.datamaster.module.ontology.api.dto.ManualEditDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 数据接收核心处理：冲突检查 → 按策略写入数据仓库（Doris）
 *
 * <p>复用 datamaster-common-common 的 SQL 能力（DbQuery / DataSourceFactory / DbQueryProperty），
 * 由数据源元信息（IDatasourceApiService）解析 Doris 数据源连接属性。</p>
 *
 * <p>冲突策略（datamaster.ingestion.conflict-strategy）：</p>
 * <ul>
 *   <li>INSERT：不论是否存在同键记录，直接插入（追加）</li>
 *   <li>UPSERT：存在同键记录则按主键更新，否则插入（默认）</li>
 *   <li>SKIP：存在同键记录则跳过</li>
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
        if (keyCols.isEmpty()) {
            keyCols = new ArrayList<>(properties.getKeyColumns());
        }
        if (keyCols.isEmpty()) {
            return IngestionResult.fail(IngestionResult.Action.ERROR, "未配置冲突判定主键列（key-columns 或消息 __keys）");
        }

        String strategyName = properties.getConflictStrategy() == null
                ? "UPSERT" : properties.getConflictStrategy().trim().toUpperCase(Locale.ROOT);

        // 写入参数字段（去除内部保留字段，如 __table / __keys）
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getKey().startsWith("__")) {
                continue;
            }
            data.put(entry.getKey(), entry.getValue());
        }

        try {
            boolean conflict = existsConflict(dbQuery, targetTable, keyCols, record);

            switch (strategyName) {
                case "INSERT":
                    dbQuery.addTableData(targetTable, data);
                    return IngestionResult.ok(IngestionResult.Action.INSERT, "已插入");
                case "UPSERT":
                    if (conflict) {
                        // 人为编辑仲裁：命中登记则按编辑值回写/跳过/告警；未命中按源数据直接更新
                        IngestionResult arbitrated = arbitrateManualEdit(dbQuery, targetTable, keyCols, data);
                        if (arbitrated != null) {
                            return arbitrated;
                        }
                        List<String> setCols = new ArrayList<>(data.keySet());
                        setCols.removeAll(keyCols);
                        if (setCols.isEmpty()) {
                            return IngestionResult.ok(IngestionResult.Action.UPSERT, "同键记录已存在且无非键字段可更新");
                        }
                        dbQuery.updateTableData(data, setCols, keyCols, targetTable);
                        return IngestionResult.ok(IngestionResult.Action.UPSERT, "同键记录存在，已更新");
                    }
                    dbQuery.addTableData(targetTable, data);
                    return IngestionResult.ok(IngestionResult.Action.INSERT, "新记录已插入");
                case "SKIP":
                    if (conflict) {
                        return IngestionResult.ok(IngestionResult.Action.SKIP, "同键记录已存在，跳过");
                    }
                    dbQuery.addTableData(targetTable, data);
                    return IngestionResult.ok(IngestionResult.Action.INSERT, "新记录已插入");
                case "ERROR":
                    if (conflict) {
                        return IngestionResult.fail(IngestionResult.Action.ERROR, "冲突策略为 ERROR，存在同键记录，拒绝写入");
                    }
                    dbQuery.addTableData(targetTable, data);
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
                                                List<String> keyCols, Map<String, Object> data) {
        try {
            List<ManualEditDTO> edits = manualEditQueryService.listLatestManualEdits(targetTable, MAX_MANUAL_EDIT_FETCH);
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
                dbQuery.updateTableData(merged, setCols, keyCols, targetTable);
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
                Object val = record.get(col);
                if (val == null) {
                    // 主键值为空无法判定冲突，视为无冲突尝试写入
                    return false;
                }
                String paramName = "p" + i;
                where.add("`" + quoteId(col) + "` = :" + paramName);
                params.put(paramName, val);
            }
            String sql = "SELECT COUNT(1) FROM `" + quoteId(targetTable) + "` WHERE " + String.join(" AND ", where);
            return dbQuery.count(sql, params) > 0;
        } catch (Exception e) {
            log.warn("冲突检查失败（按冲突策略默认视为无冲突以尝试写入），table={} err={}", targetTable, e.getMessage());
            return false;
        }
    }

    private String quoteId(String name) {
        return name.replace("`", "");
    }

    private String resolveTargetTable(JSONObject record, String override) {
        if (override != null && !override.trim().isEmpty()) {
            return override;
        }
        String inMessage = record.getString("__table");
        if (inMessage != null && !inMessage.trim().isEmpty()) {
            return inMessage.trim();
        }
        return properties.getTargetTable();
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