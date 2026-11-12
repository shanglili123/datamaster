package com.datamaster.module.ontology.service.analysis;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.assets.api.governance.dto.*;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.assets.dal.dataobject.assetColumn.AssetsAssetColumnDO;
import com.datamaster.module.assets.dal.mapper.assetColumn.AssetsAssetColumnMapper;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** All checks run before factory creation / external connection. No fallbacks. */
@Component
public class MetricQuerySourceGuard {
    @Resource private MetricQueryPolicy policy;
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private IAssetsTableGovernanceApiService governance;
    // Both types are existing assets-interface classes, not assets-core dependencies.
    @Resource private AssetsAssetColumnMapper assetColumnMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private com.datamaster.module.governance.api.flow.ISpaceFlowApi spaceFlows;
    @Resource private org.springframework.jdbc.core.JdbcTemplate jdbc;

    public DbQueryProperty authorize(MetricRespVO snapshot, MetricQueryBuilder.Plan plan, OntologyDO ontology) {
        policy.assertBinding(snapshot.getConceptTableId());
        ConceptTableDO binding = conceptTableMapper.selectById(snapshot.getConceptTableId());
        require(binding != null && Objects.equals(binding.getConceptId(), snapshot.getConceptId())
                && Objects.equals(binding.getDatasourceId(), plan.getDatasourceId())
                && Objects.equals(binding.getDatabaseName(), plan.getDatabaseName())
                && Objects.equals(binding.getTableName(), plan.getTableName())
                && Objects.equals(binding.getSchemaName(), snapshot.getSourceSnapshot().get("schemaName")),
                "当前批准绑定已改变，历史快照不可自动获得新来源授权");
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(plan.getDatasourceId());
        MetricSqlDialect dialect = MetricSqlDialect.forSource(snapshot);
        require(ds != null && !Boolean.FALSE.equals(ds.getValidFlag()) && Objects.equals(ds.getId(), plan.getDatasourceId())
                && dialect.sameType(ds.getDatasourceType()), "来源数据源不存在或类型已改变");
        DbQueryProperty property;
        try { property = new DbQueryProperty(ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig()); }
        catch (RuntimeException ex) { throw new ServiceException("指标来源数据源配置不可用"); }
        policy.assertReadonlyUsername(property.getUsername());
        require(plan.getDatabaseName().equals(property.getDbName()), "发布数据库与当前数据源数据库不一致，拒绝跨库查询");
        if (dialect.postgres()) require(Objects.equals(binding.getSchemaName(), property.getSid()), "发布schema与当前数据源配置不一致，拒绝跨schema查询");
        // Never routeTo/setDbName: a snapshot cannot retarget a saved connection.
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO();
        request.setDatasourceId(plan.getDatasourceId()); request.setTableName(plan.getTableName());
        request.setSpaceId(ontology.getSpaceId()); request.setSpaceCode(ontology.getSpaceCode());
        request.setColumnNames(new ArrayList<>(plan.getUsedColumns())); request.setEntrance("ONTOLOGY_METRIC_QUERY");
        AssetsTableGovernanceRespDTO response = governance.resolveTable(request);
        com.datamaster.module.governance.api.flow.ISpaceFlowApi.Policy flow = spaceFlows.policy(ontology.getSpaceId(), ontology.getSpaceCode());
        boolean spaceMode = "SPACE_FLOW".equals(snapshot.getSourceSnapshot().get("sourceMode"));
        // Existing publications retain strict asset checks. A new publication explicitly opts into space policy.
        if (!spaceMode || flow.isAssetRequired()) {
            assertGovernance(response, plan.getUsedColumns(), plan.getDatasourceId(), plan.getTableName());
            Set<String> hidden = governance.getDesensitizeHideColumns(response.getAssetId(), SecurityUtils.getUserId(), null, "2");
            require(hidden != null, "敏感列检查不可用");
            for (String used : plan.getUsedColumns()) for (String denied : hidden) require(!used.equalsIgnoreCase(denied), "查询包含需要隐藏的敏感字段");
            assertNonsensitiveColumns(plan.getUsedColumns(), assetColumnMapper.findByAssetId(response.getAssetId()));
        } else {
            require(datasourceApiService.hasDatasourceSpace(plan.getDatasourceId(), ontology.getSpaceId(), ontology.getSpaceCode()), "轻量指标数据源未有效关联当前空间");
            require(response != null && Boolean.TRUE.equals(response.getAccessAllowed()), "当前空间来源准入未通过");
            if (Boolean.TRUE.equals(response.getEnabled()) && "strict".equals(response.getMode())) {
                // A global strict policy cannot become a metadata bypass merely because no asset matched.
                assertGovernance(response, plan.getUsedColumns(), plan.getDatasourceId(), plan.getTableName());
                Set<String> hidden = governance.getDesensitizeHideColumns(response.getAssetId(), SecurityUtils.getUserId(), null, "2");
                require(hidden != null, "敏感列检查不可用");
                for (String used : plan.getUsedColumns()) for (String denied : hidden) require(!used.equalsIgnoreCase(denied), "查询包含需要隐藏的敏感字段");
                assertNonsensitiveColumns(plan.getUsedColumns(), assetColumnMapper.findByAssetId(response.getAssetId()));
            }
            assertMetadataSource(ontology, binding, plan);
        }
        return property;
    }

    /** Light mode uses registered metadata in the actual space, not an asset bypass or a guessed table. */
    private void assertMetadataSource(OntologyDO ontology, ConceptTableDO binding, MetricQueryBuilder.Plan plan) {
        List<Map<String,Object>> tables = jdbc.queryForList("SELECT id,safety_level_id FROM cat_table WHERE datasource_id=? AND table_name=? AND db_name=? AND COALESCE(schema_name,'')=COALESCE(?,'') AND space_id=? AND (del_flag IS NULL OR del_flag=false) AND valid_flag=true", plan.getDatasourceId(), plan.getTableName(), plan.getDatabaseName(), binding.getSchemaName(), ontology.getSpaceId());
        require(tables.size() == 1, "轻量指标来源必须唯一登记在本空间元数据中（明确库/schema）");
        require(tables.get(0).get("safety_level_id") == null, "安全分级表不能通过轻量指标路径读取，请使用严格资产授权路径");
        List<Map<String,Object>> columns = jdbc.queryForList("SELECT column_name,safety_level_id FROM cat_column WHERE table_id=? AND (del_flag IS NULL OR del_flag=false) AND valid_flag=true", tables.get(0).get("id"));
        for (String used : plan.getUsedColumns()) {
            List<Map<String,Object>> matches = new ArrayList<>();
            for (Map<String,Object> column : columns) if (used.equalsIgnoreCase(String.valueOf(column.get("column_name")))) matches.add(column);
            require(matches.size() == 1 && matches.get(0).get("safety_level_id") == null, "轻量指标参与字段未唯一登记或存在安全等级，请走严格资产授权");
        }
    }

    public static void assertGovernance(AssetsTableGovernanceRespDTO response, Set<String> used, Long datasourceId, String tableName) {
        require(response != null && Boolean.TRUE.equals(response.getEnabled()) && "strict".equals(response.getMode())
                && "ASSET".equals(response.getSource()) && response.getAssetId() != null
                && Boolean.TRUE.equals(response.getAccessAllowed()), "严格资产治理未授权该指标来源");
        require(Objects.equals(response.getDatasourceId(), datasourceId) && Objects.equals(response.getTableName(), tableName),
                "治理来源身份不一致");
        require(response.getDeniedColumns() == null || response.getDeniedColumns().isEmpty(), "存在未获授权的参与字段");
        require(response.getAllowedColumns() == null || response.getAllowedColumns().containsAll(used), "参与字段未全部获得授权");
    }

    public static void assertNonsensitiveColumns(Set<String> used, List<AssetsAssetColumnDO> registered) {
        require(registered != null, "资产字段登记不可用");
        for (String column : used) {
            int matches = 0;
            for (AssetsAssetColumnDO candidate : registered) {
                if (candidate != null && column.equalsIgnoreCase(candidate.getColumnName())) {
                    matches++;
                    require(candidate.getSensitiveLevelId() == null, "仅允许不含敏感等级的安全ADS字段参与查询");
                }
            }
            require(matches == 1, "参与字段未唯一登记为安全资产字段");
        }
    }
}
