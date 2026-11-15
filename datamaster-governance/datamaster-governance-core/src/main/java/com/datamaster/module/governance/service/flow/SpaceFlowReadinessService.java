package com.datamaster.module.governance.service.flow;

import com.datamaster.module.governance.api.flow.ISpaceFlowApi.Definition;
import com.datamaster.module.governance.api.flow.SpaceFlowImpactProvider.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;

/** Read-only facts against a saved candidate. Never writes admission evidence or executes source SQL. */
@Component
public class SpaceFlowReadinessService {
    @Resource private JdbcTemplate jdbc;

    public void assess(Consumer consumer, Definition draft, Long spaceId, String spaceCode, int nextVersion) {
        consumer.setMissingRequirements(new ArrayList<>()); consumer.setNextSteps(new ArrayList<>()); consumer.setPendingChecks(new ArrayList<>());
        boolean strict = draft.getNodes().contains("ASSET") || draft.getNodes().contains("QUALITY");
        if ("SPACE_INCONSISTENT".equals(consumer.getSourceCoverage())) {
            missing(consumer, "SPACE_INCONSISTENT", "维护消费对象所属空间，确保ID和编码一致");
        } else if ("NON_QUERY_UNRESOLVED".equals(consumer.getSourceCoverage())) {
            if (strict) missing(consumer, "UNVERIFIED_NON_QUERY_SOURCES", "文件/转发服务需先建设完整来源核验，或改用可核验受控查询入口");
            else consumer.getPendingChecks().add("非查询服务来源未完整登记，仍受当前调用权限约束");
        } else if ("CUSTOM_SQL_UNRESOLVED".equals(consumer.getSourceCoverage())) {
            if (strict) missing(consumer, "UNVERIFIED_SQL_SOURCES", "改用可核验单表映射，或先完成全部来源的受控解析");
            else consumer.getPendingChecks().add("脚本来源未解析，保留原路径不等于已验证安全");
        } else if (!("DECLARED_SINGLE_TABLE".equals(consumer.getSourceCoverage()) || "PUBLISHED_SOURCE_SNAPSHOT".equals(consumer.getSourceCoverage()))
                || consumer.getDatasourceId() == null || consumer.getDatasourceId() <= 0 || consumer.getTableName() == null || consumer.getTableName().isBlank()) {
            if (strict) missing(consumer, "UNVERIFIED_SOURCE", "补齐真实来源与发布版本，再重新预览；严格流程不能核验不完整来源");
            else consumer.getNextSteps().add("补齐真实来源与发布版本，再重新预览");
            consumer.getPendingChecks().add("来源快照或执行配置不完整，无法确认参与表");
        } else {
            if (draft.getNodes().contains("ASSET")) asset(consumer, spaceId, spaceCode);
            if (draft.getNodes().contains("QUALITY")) {
                Long count = jdbc.queryForObject("SELECT count(*) FROM gov_space_quality_admission WHERE space_id=? AND datasource_id=? AND table_name=? AND flow_version=? AND available=true", Long.class, spaceId, consumer.getDatasourceId(), consumer.getTableName(), nextVersion);
                if (count == null || count == 0) missing(consumer, "QUALITY_EVIDENCE_FOR_NEW_VERSION", "新版本发布后，按该版本为指定来源建立检查依据或有权人工可用性确认；不会自动沿用旧版本");
            }
            consumer.getPendingChecks().add("消费时还须核验当前来源绑定、用户及字段权限、只读账号与调用配置；此处不创建访问许可");
        }
        consumer.setReadiness(!consumer.getMissingRequirements().isEmpty() ? "MISSING_REQUIREMENTS" : "PENDING_RUNTIME_CHECKS");
    }

    private void asset(Consumer consumer, Long spaceId, String spaceCode) {
        List<Map<String,Object>> assets = jdbc.queryForList("SELECT id,status,valid_flag FROM ast_asset WHERE datasource_id=? AND table_name=? AND (del_flag IS NULL OR del_flag=false)", consumer.getDatasourceId(), consumer.getTableName());
        if (assets.size() != 1) {
            missing(consumer, assets.isEmpty() ? "ASSET_NOT_REGISTERED" : "ASSET_AMBIGUOUS", "将来源唯一登记为有效资产，不能回退元数据或猜测同名表"); return;
        }
        Map<String,Object> asset = assets.get(0);
        if (!Boolean.TRUE.equals(asset.get("valid_flag")) || !"2".equals(String.valueOf(asset.get("status")))) missing(consumer, "ASSET_NOT_PUBLISHED", "启用并发布指定资产");
        Long granted = jdbc.queryForObject("SELECT (SELECT count(*) FROM ast_asset_space_rel WHERE asset_id=? AND space_id=? AND space_code=? AND (del_flag IS NULL OR del_flag=false) AND valid_flag=true) + (SELECT count(*) FROM ast_asset_apply WHERE asset_id=? AND space_id=? AND space_code=? AND status='2' AND (del_flag IS NULL OR del_flag=false) AND valid_flag=true)", Long.class, asset.get("id"), spaceId, spaceCode, asset.get("id"), spaceId, spaceCode);
        if (granted == null || granted == 0) missing(consumer, "SPACE_ASSET_AUTHORIZATION", "为当前空间办理有效资产授权/申请，字段及用户权限仍需单独满足");
    }
    private void missing(Consumer consumer, String code, String next) {
        consumer.getMissingRequirements().add(code); consumer.getNextSteps().add(next);
    }
}
