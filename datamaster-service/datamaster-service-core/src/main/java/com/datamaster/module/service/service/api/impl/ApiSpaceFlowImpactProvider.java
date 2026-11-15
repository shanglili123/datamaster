package com.datamaster.module.service.service.api.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider;
import com.datamaster.module.service.dal.dataobject.api.ExecuteConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/** Inventory only. Source registration is not an admission check and never executes user SQL. */
@Component
public class ApiSpaceFlowImpactProvider implements SpaceFlowImpactProvider {
    @Resource private JdbcTemplate jdbc;
    private static final String SCOPE = " FROM svc_api WHERE (del_flag IS NULL OR del_flag='0') AND (space_id=? OR (space_id IS NULL AND space_code=?))";
    @Override public String type() { return "API"; }
    @Override public Page inventory(Long spaceId, String spaceCode, int page, int pageSize) {
        Long total = jdbc.queryForObject("SELECT count(*)" + SCOPE, Long.class, spaceId, spaceCode);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT id,name,status,api_service_type,transmit_type,config_json,space_id,space_code" + SCOPE + " ORDER BY id DESC LIMIT ? OFFSET ?", spaceId, spaceCode, pageSize, (page - 1) * pageSize);
        Page result = new Page(); result.setTotal(total == null ? 0 : total);
        for (Map<String,Object> row : rows) {
            Consumer consumer = new Consumer(); consumer.setId(((Number) row.get("id")).longValue());
            consumer.setName((String) row.get("name")); consumer.setStatus((String) row.get("status"));
            consumer.setSourceCoverage("UNKNOWN");
            consumer.setNotice("执行配置不完整或损坏，需要维护后核验来源");
            try {
                ExecuteConfig config = JSON.parseObject((String) row.get("config_json"), ExecuteConfig.class);
                if (config != null && "1".equals(config.getApiServiceType()) && config.getSourceId() != null && config.getSourceId().matches("[1-9][0-9]*")
                        && config.getTableName() != null && config.getTableName().matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
                    consumer.setDatasourceId(Long.valueOf(config.getSourceId())); consumer.setTableName(config.getTableName());
                    consumer.setSourceCoverage("DECLARED_SINGLE_TABLE");
                    consumer.setNotice("表映射已声明来源；发布新流程后仍须在实际使用时检查当前准入，不代表一定通过或一定阻断");
                } else if (config != null && "2".equals(config.getApiServiceType())) {
                    consumer.setSourceCoverage("CUSTOM_SQL_UNRESOLVED");
                    consumer.setNotice("脚本全部来源未解析；配置资产或质量严格流程后此类API将拒绝发布与执行");
                }
            } catch (RuntimeException malformed) { /* Inventory remains visible with explicit unknown coverage. */ }
            if ("4".equals(String.valueOf(row.get("api_service_type"))) || "1".equals(String.valueOf(row.get("transmit_type"))) || "2".equals(String.valueOf(row.get("transmit_type")))) {
                consumer.setDatasourceId(null); consumer.setTableName(null);
                consumer.setSourceCoverage("NON_QUERY_UNRESOLVED"); consumer.setNotice("文件/转发服务无法核验全部来源，严格空间拒绝消费，不能按单表配置预检放行");
            }
            String recordedCode = (String) row.get("space_code");
            if (recordedCode != null && !recordedCode.isBlank() && !recordedCode.equals(spaceCode)) {
                consumer.setSourceCoverage("SPACE_INCONSISTENT"); consumer.setNotice("API空间ID/编码不一致，消费前须维护，不能回退放行");
            }
            result.getRows().add(consumer);
        }
        return result;
    }
}
