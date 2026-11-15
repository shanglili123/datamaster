package com.datamaster.module.governance.service.flow;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;

@Service
public class SpaceFlowService implements ISpaceFlowApi {
    @Resource private JdbcTemplate jdbc;
    @Resource private ISpaceApi spaces;
    @Resource private SpaceFlowReadinessService readiness;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider> impactProviders = Collections.emptyList();

    private String space(Long id, String code, boolean member) {
        if (id == null || id <= 0) throw new ServiceException("空间不能为空");
        String actual = spaces.getSpaceCodeBySpaceId(id);
        if (actual == null || (code != null && !code.isBlank() && !actual.equals(code))) throw new ServiceException("空间标识不一致");
        if (member) {
            Long user = SecurityUtils.getUserId();
            if (!spaces.hasUserSpace(user, id, actual)) throw new ServiceException("无权管理此空间流程");
            Long manager = jdbc.queryForObject("SELECT manager_id FROM tax_space WHERE id=? AND (del_flag IS NULL OR del_flag='0')", Long.class, id);
            if (!Objects.equals(user, manager) && !SecurityUtils.hasPermi("*:*:*")) throw new ServiceException("仅空间管理员或平台管理员可配置流程及确认质量");
        }
        return actual;
    }
    public Map<String,Object> get(Long id, String code) {
        space(id, code, true);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT revision,draft_json,published_version,published_json FROM gov_space_flow WHERE space_id=?", id);
        if (rows.isEmpty()) return new LinkedHashMap<>(Map.of("revision", 0, "publishedVersion", 0));
        Map<String,Object> row = rows.get(0), result = new LinkedHashMap<>();
        result.put("revision", row.get("revision")); result.put("publishedVersion", row.get("published_version"));
        result.put("draft", storedDefinition(row.get("draft_json")));
        result.put("published", publishedDefinition(row));
        return result;
    }
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> save(SaveRequest r) {
        space(r.getSpaceId(), r.getSpaceCode(), true); SpaceFlowRules.validate(r.getDefinition());
        if (r.getRevision() == null || r.getRevision() < 0) throw new ServiceException("缺少流程revision");
        nextCounter(r.getRevision());
        String json = JSON.toJSONString(r.getDefinition());
        int changed;
        if (r.getRevision() == 0) changed = jdbc.update("INSERT INTO gov_space_flow(space_id,revision,draft_json,published_version,updated_by) VALUES(?,1,?,0,?) ON CONFLICT(space_id) DO NOTHING", r.getSpaceId(), json, SecurityUtils.getUserId());
        else changed = jdbc.update("UPDATE gov_space_flow SET draft_json=?,revision=revision+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE space_id=? AND revision=?", json, SecurityUtils.getUserId(), r.getSpaceId(), r.getRevision());
        if (changed != 1) throw new ServiceException("流程已修改，请刷新");
        return get(r.getSpaceId(), r.getSpaceCode());
    }
    public void validateDraft(SaveRequest r) {
        space(r.getSpaceId(), r.getSpaceCode(), true);
        SpaceFlowRules.validate(r.getDefinition());
    }

    /** Preview the saved revision only; an unsaved client graph is never a publish candidate. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String,Object> preview(SaveRequest r) {
        space(r.getSpaceId(), r.getSpaceCode(), true);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT revision,draft_json,published_version,published_json FROM gov_space_flow WHERE space_id=?", r.getSpaceId());
        Map<String,Object> current = requireRevision(rows, r.getRevision());
        Definition draft = storedDefinition(current.get("draft_json"));
        Definition published = publishedDefinition(current);
        Map<String,Object> result = SpaceFlowRules.changes(published, draft);
        int version = ((Number) current.get("published_version")).intValue();
        result.put("revision", r.getRevision()); result.put("publishedVersion", version);
        result.put("nextVersion", nextCounter(version)); result.put("published", published); result.put("draft", draft);
        result.put("qualityEvidenceReset", draft.getNodes().contains("QUALITY"));
        // Configuration differences are exact; do not label unknown runtime consumer impact as zero.
        result.put("consumerImpactKnown", false);
        result.put("impactNotice", "下方按已保存草稿和下一版本，只读核对本空间API/正式指标声明来源的已知准入缺项；不执行来源查询或任务，也不授予权限。外部消费、未登记空间和脚本全部来源仍未知，实际使用仍须核验当前用户、字段、绑定与连接。");
        List<Map<String,Object>> inventories = new ArrayList<>();
        for (String type : List.of("API", "METRIC")) inventories.add(consumerInventory(r, type, 1, 10, draft, nextCounter(version)));
        result.put("inventories", inventories);
        return result;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String,Object> impacts(SaveRequest request, String type, int page, int pageSize) {
        space(request.getSpaceId(), request.getSpaceCode(), true);
        Map<String,Object> current = requireRevision(jdbc.queryForList("SELECT revision,draft_json,published_version FROM gov_space_flow WHERE space_id=?", request.getSpaceId()), request.getRevision());
        Definition draft = storedDefinition(current.get("draft_json"));
        return consumerInventory(request, type, page, pageSize, draft, nextCounter(((Number) current.get("published_version")).intValue()));
    }

    private Map<String,Object> consumerInventory(SaveRequest request, String type, int page, int pageSize, Definition draft, int nextVersion) {
        if (type == null || !Set.of("API", "METRIC").contains(type) || page < 1 || page > 1000000 || pageSize < 1 || pageSize > 100) throw new ServiceException("消费清单类型或分页参数不合法");
        Map<String,Object> result = new LinkedHashMap<>(); result.put("type", type);
        com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider provider = impactProviders.stream().filter(item -> type.equals(item.type())).findFirst().orElse(null);
        result.put("available", provider != null);
        result.put("coverage", "SPACE_REGISTERED_CONSUMERS_ONLY");
        if (provider == null) {
            result.put("total", null); result.put("rows", List.of());
            result.put("notice", "当前部署未提供此类消费登记查询，不能视为零影响");
        } else {
            String canonicalCode = spaces.getSpaceCodeBySpaceId(request.getSpaceId());
            com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Page inventory = provider.inventory(request.getSpaceId(), canonicalCode, page, pageSize);
            for (com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Consumer consumer : inventory.getRows()) readiness.assess(consumer, draft, request.getSpaceId(), canonicalCode, nextVersion);
            result.put("total", inventory.getTotal()); result.put("rows", inventory.getRows());
            result.put("notice", "按已保存草稿及下一版本核对登记来源的资产/空间授权/质量依据缺项；仍待消费时核验用户、字段、绑定及连接，不是放行许可");
        }
        return result;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String,Object> versions(Long id, String code, int page, int pageSize) {
        space(id, code, true);
        if (page < 1 || page > 1000000 || pageSize < 1 || pageSize > 100) throw new ServiceException("版本分页参数不合法");
        Long total = jdbc.queryForObject("SELECT count(*) FROM gov_space_flow_version WHERE space_id=?", Long.class, id);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT version,definition_json,published_by,published_at FROM gov_space_flow_version WHERE space_id=? ORDER BY version DESC LIMIT ? OFFSET ?", id, pageSize, (page - 1) * pageSize);
        List<Map<String,Object>> versions = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            Map<String,Object> item = new LinkedHashMap<>(row);
            item.put("definition", storedDefinition(item.remove("definition_json")));
            versions.add(item);
        }
        Map<String,Object> result = new LinkedHashMap<>(); result.put("rows", versions); result.put("total", total); return result;
    }

    private int nextCounter(int value) {
        if (value < 0 || value == Integer.MAX_VALUE) throw new ServiceException("流程版本或revision无效或已达上限，请联系管理员处理");
        return value + 1;
    }
    private Definition storedDefinition(Object json) {
        try {
            if (json == null) throw new IllegalArgumentException();
            return SpaceFlowRules.validate(JSON.parseObject(String.valueOf(json), Definition.class));
        } catch (RuntimeException invalid) {
            throw new ServiceException("存储流程定义损坏，请联系管理员核对；不能视为未配置或回退放行");
        }
    }
    private Definition publishedDefinition(Map<String,Object> row) {
        Object rawVersion = row.get("published_version");
        if (!(rawVersion instanceof Number) || ((Number) rawVersion).intValue() < 0) throw new ServiceException("流程发布版本无效");
        int version = ((Number) rawVersion).intValue();
        if (version == 0 && row.get("published_json") == null) return null;
        if (version == 0) throw new ServiceException("流程发布版本与定义不一致");
        return storedDefinition(row.get("published_json"));
    }
    private Map<String,Object> requireRevision(List<Map<String,Object>> rows, Integer revision) {
        if (revision == null || revision <= 0 || rows.isEmpty()
                || ((Number) rows.get(0).get("revision")).intValue() != revision) throw new ServiceException("流程版本已变化或未保存，请重新加载并保存草稿");
        return rows.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> publish(SaveRequest r) {
        space(r.getSpaceId(), r.getSpaceCode(), true);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM gov_space_flow WHERE space_id=? FOR UPDATE", r.getSpaceId());
        Map<String,Object> current = requireRevision(rows, r.getRevision());
        Definition d = storedDefinition(current.get("draft_json"));
        int version = nextCounter(((Number) current.get("published_version")).intValue());
        nextCounter(r.getRevision());
        String json = JSON.toJSONString(d);
        if (jdbc.update("INSERT INTO gov_space_flow_version(space_id,version,definition_json,published_by) VALUES(?,?,?,?)", r.getSpaceId(), version, json, SecurityUtils.getUserId()) != 1)
            throw new ServiceException("流程发布历史保存失败");
        if (jdbc.update("UPDATE gov_space_flow SET published_json=?,published_version=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP,updated_by=? WHERE space_id=?", json, version, SecurityUtils.getUserId(), r.getSpaceId()) != 1)
            throw new ServiceException("流程发布状态更新失败");
        return get(r.getSpaceId(), r.getSpaceCode());
    }
    @Override public Policy policy(Long id, String code) {
        Policy p = new Policy();
        if (id == null && code != null && !code.isBlank()) {
            id = spaces.getSpaceIdBySpaceCode(code);
            if (id == null) throw new ServiceException("空间编码不存在");
        }
        if (id == null) return p;
        space(id, code, false);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT published_version,published_json FROM gov_space_flow WHERE space_id=?", id);
        if (rows.isEmpty()) return p;
        Definition d = publishedDefinition(rows.get(0));
        if (d == null) return p;
        p.setConfigured(true); p.setVersion(((Number) rows.get(0).get("published_version")).intValue());
        p.setAssetRequired(d.getNodes().contains("ASSET")); p.setQualityRequired(d.getNodes().contains("QUALITY")); return p;
    }
    @Override public void assertQuality(Long id, String code, Long datasource, String table) {
        Policy p = policy(id, code); if (!p.isQualityRequired()) return;
        Long canonical = id == null ? spaces.getSpaceIdBySpaceCode(code) : id;
        Long count = jdbc.queryForObject("SELECT count(*) FROM gov_space_quality_admission WHERE space_id=? AND datasource_id=? AND table_name=? AND flow_version=? AND available=true", Long.class, canonical, datasource, table, p.getVersion());
        if (count == null || count == 0) throw new ServiceException("空间已配置质量准入：该表尚未检查达标或人工确认可用，禁止开放和分析");
    }
    /** Query only the selected space/table; publishing a new flow never hides old evidence. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String,Object> qualityHistory(Long id, String code, Long datasource, String table, int page, int pageSize) {
        space(id, code, true);
        validateQualityTable(datasource, table);
        if (page < 1 || page > 1000000 || pageSize < 1 || pageSize > 100) throw new ServiceException("历史分页参数不合法");
        Policy p = policy(id, code);
        List<Map<String,Object>> current = jdbc.queryForList("SELECT flow_version,available,basis,quality_log_id,reason,confirmed_by,confirmed_at FROM gov_space_quality_admission WHERE space_id=? AND datasource_id=? AND table_name=?", id, datasource, table);
        String status = qualityStatus(p, current.isEmpty() ? null : current.get(0));
        Long total = jdbc.queryForObject("SELECT count(*) FROM gov_space_quality_admission_event WHERE space_id=? AND datasource_id=? AND table_name=?", Long.class, id, datasource, table);
        List<Map<String,Object>> events = jdbc.queryForList("SELECT id,flow_version,basis,quality_log_id,reason,confirmed_by,confirmed_at FROM gov_space_quality_admission_event WHERE space_id=? AND datasource_id=? AND table_name=? ORDER BY confirmed_at DESC,id DESC LIMIT ? OFFSET ?", id, datasource, table, pageSize, (page - 1) * pageSize);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("status", status); result.put("publishedVersion", p.getVersion());
        result.put("current", current.isEmpty() ? null : current.get(0));
        result.put("rows", events); result.put("total", total); return result;
    }
    static String qualityStatus(Policy policy, Map<String,Object> current) {
        if (!policy.isQualityRequired()) return "NOT_REQUIRED";
        if (current == null) return "PENDING";
        if (!Boolean.TRUE.equals(current.get("available"))) return "REVOKED";
        if (((Number) current.get("flow_version")).intValue() != policy.getVersion()) return "VERSION_EXPIRED";
        return "AVAILABLE";
    }
    private void validateQualityTable(Long datasource, String table) {
        if (datasource == null || datasource <= 0 || table == null || table.isBlank() || table.length() > 200) throw new ServiceException("请明确有效数据源和表（表名最多200字）");
    }
    @Transactional(rollbackFor = Exception.class)
    public void confirmQuality(QualityRequest r) {
        String code = space(r.getSpaceId(), r.getSpaceCode(), true);
        jdbc.queryForList("SELECT space_id FROM gov_space_flow WHERE space_id=? FOR UPDATE", r.getSpaceId());
        Policy p = policy(r.getSpaceId(), code);
        if (!p.isQualityRequired()) throw new ServiceException("当前空间尚未配置质量准入");
        validateQualityTable(r.getDatasourceId(), r.getTableName());
        if (r.getReason() != null && r.getReason().length() > 1000) throw new ServiceException("原因最多1000字");
        // Bind to a visible registered metadata table, never trust an arbitrary client table identity.
        List<Map<String,Object>> visible = jdbc.queryForList("SELECT id FROM cat_table WHERE datasource_id=? AND table_name=? AND space_id=? AND (del_flag IS NULL OR del_flag=false)", r.getDatasourceId(), r.getTableName(), r.getSpaceId());
        if (visible.size() != 1) throw new ServiceException("表未唯一登记在当前空间元数据中");
        String evidence;
        if (r.isRevoke()) {
            if (r.getReason() == null || r.getReason().isBlank() || r.getReason().length() > 1000) throw new ServiceException("撤销需填写原因");
            jdbc.update("UPDATE gov_space_quality_admission SET available=false,reason=?,confirmed_by=?,confirmed_at=CURRENT_TIMESTAMP WHERE space_id=? AND datasource_id=? AND table_name=?", r.getReason(), SecurityUtils.getUserId(), r.getSpaceId(), r.getDatasourceId(), r.getTableName());
            jdbc.update("INSERT INTO gov_space_quality_admission_event(space_id,datasource_id,table_name,flow_version,basis,reason,confirmed_by) VALUES(?,?,?,?,?,?,?)", r.getSpaceId(), r.getDatasourceId(), r.getTableName(), p.getVersion(), "REVOKED", r.getReason(), SecurityUtils.getUserId());
            return;
        }
        if (r.isManual()) {
            if (r.getReason() == null || r.getReason().isBlank() || r.getReason().length() > 1000) throw new ServiceException("人工确认必须填写原因（最多1000字）");
            evidence = "MANUAL_AVAILABLE";
        } else {
            Long valid = jdbc.queryForObject("SELECT count(*) FROM col_quality_log l JOIN col_quality_task t ON t.id=l.quality_id WHERE l.id=? AND t.space_id=? AND l.end_time IS NOT NULL AND l.problem_data=0 AND l.success_flag='0' AND (l.del_flag IS NULL OR l.del_flag=false) AND EXISTS (SELECT 1 FROM col_quality_task_obj o WHERE o.task_id=t.id AND o.datasource_id=? AND o.table_name=? AND (o.del_flag IS NULL OR o.del_flag=false))", Long.class, r.getQualityLogId(), r.getSpaceId(), r.getDatasourceId(), r.getTableName());
            if (valid == null || valid != 1) throw new ServiceException("未找到同空间同表、已完成且零问题的成功质量记录");
            evidence = "CHECK_PASSED";
        }
        Long qualityLogId = r.isManual() ? null : r.getQualityLogId();
        jdbc.update("INSERT INTO gov_space_quality_admission(space_id,datasource_id,table_name,flow_version,available,basis,quality_log_id,reason,confirmed_by) VALUES(?,?,?,?,true,?,?,?,?) ON CONFLICT(space_id,datasource_id,table_name) DO UPDATE SET flow_version=EXCLUDED.flow_version,available=true,basis=EXCLUDED.basis,quality_log_id=EXCLUDED.quality_log_id,reason=EXCLUDED.reason,confirmed_by=EXCLUDED.confirmed_by,confirmed_at=CURRENT_TIMESTAMP", r.getSpaceId(), r.getDatasourceId(), r.getTableName(), p.getVersion(), evidence, qualityLogId, r.getReason(), SecurityUtils.getUserId());
        jdbc.update("INSERT INTO gov_space_quality_admission_event(space_id,datasource_id,table_name,flow_version,basis,quality_log_id,reason,confirmed_by) VALUES(?,?,?,?,?,?,?,?)", r.getSpaceId(), r.getDatasourceId(), r.getTableName(), p.getVersion(), evidence, qualityLogId, r.getReason(), SecurityUtils.getUserId());
    }
}
