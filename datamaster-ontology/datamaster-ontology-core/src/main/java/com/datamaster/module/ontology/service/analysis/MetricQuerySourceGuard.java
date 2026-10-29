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
        require(ds != null && Objects.equals(ds.getId(), plan.getDatasourceId())
                && "DORIS".equalsIgnoreCase(ds.getDatasourceType()), "来源数据源不存在或已非Doris");
        DbQueryProperty property;
        try { property = new DbQueryProperty(ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig()); }
        catch (RuntimeException ex) { throw new ServiceException("指标来源数据源配置不可用"); }
        policy.assertReadonlyUsername(property.getUsername());
        require(plan.getDatabaseName().equals(property.getDbName()), "发布数据库与当前数据源数据库不一致，拒绝跨库查询");
        // Never routeTo/setDbName: a snapshot cannot retarget a saved connection.
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO();
        request.setDatasourceId(plan.getDatasourceId()); request.setTableName(plan.getTableName());
        request.setSpaceId(ontology.getSpaceId()); request.setSpaceCode(ontology.getSpaceCode());
        request.setColumnNames(new ArrayList<>(plan.getUsedColumns())); request.setEntrance("ONTOLOGY_METRIC_QUERY");
        AssetsTableGovernanceRespDTO response = governance.resolveTable(request);
        assertGovernance(response, plan.getUsedColumns(), plan.getDatasourceId(), plan.getTableName());
        Set<String> hidden = governance.getDesensitizeHideColumns(response.getAssetId(), SecurityUtils.getUserId(), null, "2");
        require(hidden != null, "敏感列检查不可用");
        for (String used : plan.getUsedColumns()) {
            for (String denied : hidden) require(!used.equalsIgnoreCase(denied), "查询包含需要隐藏的敏感字段");
        }
        // Aggregation does not mask sensitive inputs. Reject any sensitivity metadata (including
        // potential ACTION_REPLACE), even for users whose masking action would currently pass.
        assertNonsensitiveColumns(plan.getUsedColumns(), assetColumnMapper.findByAssetId(response.getAssetId()));
        return property;
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
