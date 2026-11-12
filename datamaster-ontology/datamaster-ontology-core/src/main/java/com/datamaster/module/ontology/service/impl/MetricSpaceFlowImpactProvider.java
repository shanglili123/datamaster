package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/** Inventory immutable published metric snapshots; edited draft names/sources are not evidence. */
@Component
public class MetricSpaceFlowImpactProvider implements SpaceFlowImpactProvider {
    @Resource private JdbcTemplate jdbc;
    private static final String SCOPE = " FROM ont_metric m JOIN ont_ontology o ON o.id=m.ontology_id LEFT JOIN ont_metric_version v ON v.metric_id=m.id AND v.version=m.published_version WHERE (o.del_flag IS NULL OR o.del_flag=0) AND m.status='PUBLISHED' AND (o.space_id=? OR (o.space_id IS NULL AND o.space_code=?))";
    @Override public String type() { return "METRIC"; }
    @Override public Page inventory(Long spaceId, String spaceCode, int page, int pageSize) {
        Long total = jdbc.queryForObject("SELECT count(*)" + SCOPE, Long.class, spaceId, spaceCode);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT m.id,m.code,m.status,m.ontology_id,m.concept_id,m.published_version,v.snapshot_json,o.space_code" + SCOPE + " ORDER BY m.id DESC LIMIT ? OFFSET ?", spaceId, spaceCode, pageSize, (page - 1) * pageSize);
        Page result = new Page(); result.setTotal(total == null ? 0 : total);
        for (Map<String,Object> row : rows) {
            Consumer consumer = new Consumer(); consumer.setId(((Number) row.get("id")).longValue());
            consumer.setName((String) row.get("code")); consumer.setStatus((String) row.get("status"));
            consumer.setSourceCoverage("UNKNOWN"); consumer.setNotice("正式发布快照缺失或损坏，须先核验指标版本");
            try {
                JSONObject snapshot = JSON.parseObject((String) row.get("snapshot_json"));
                if (snapshot != null && sameId(snapshot.getLong("id"), row.get("id"))
                        && sameId(snapshot.getLong("ontologyId"), row.get("ontology_id"))
                        && sameId(snapshot.getLong("conceptId"), row.get("concept_id"))
                        && sameId(snapshot.getLong("publishedVersion"), row.get("published_version"))
                        && Objects.equals(snapshot.getString("code"), row.get("code")) && "PUBLISHED".equals(snapshot.getString("status"))) {
                    consumer.setName(snapshot.getString("name")); JSONObject source = snapshot.getJSONObject("sourceSnapshot");
                    if (source != null && source.getLong("datasourceId") != null && source.getLong("datasourceId") > 0
                            && source.getString("tableName") != null && source.getString("tableName").matches("[A-Za-z_][A-Za-z0-9_]*")
                            && "METADATA_ONLY".equals(source.getString("validationScope"))) {
                        consumer.setDatasourceId(source.getLong("datasourceId")); consumer.setTableName(source.getString("tableName"));
                        consumer.setSourceCoverage("PUBLISHED_SOURCE_SNAPSHOT");
                        consumer.setNotice("正式版本已记录来源；查询仍需核验当前绑定、权限及新流程准入，此清单不执行指标查询");
                    }
                }
            } catch (RuntimeException malformed) { /* Corrupt publication is unknown, not silently absent. */ }
            String recordedCode = (String) row.get("space_code");
            if (recordedCode != null && !recordedCode.isBlank() && !recordedCode.equals(spaceCode)) {
                consumer.setSourceCoverage("SPACE_INCONSISTENT"); consumer.setNotice("本体空间ID/编码不一致，须先维护，不能回退放行");
            }
            result.getRows().add(consumer);
        }
        return result;
    }
    private static boolean sameId(Long id, Object value) { return id != null && value instanceof Number && id.longValue() == ((Number) value).longValue(); }
}
