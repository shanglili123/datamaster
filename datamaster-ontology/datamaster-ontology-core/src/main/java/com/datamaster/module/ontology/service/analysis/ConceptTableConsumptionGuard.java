package com.datamaster.module.ontology.service.analysis;

import com.datamaster.module.assets.api.governance.dto.*;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Administrative binding/preview operations still consume data and must honor stored space policy. */
@Component
public class ConceptTableConsumptionGuard {
    @Resource private ConceptMapper concepts;
    @Resource private OntologyConsumptionContext context;
    @Resource private IDatasourceApiService datasources;
    @Resource private IAssetsTableGovernanceApiService governance;

    public OntologyDO ownership(ConceptTableDO table) {
        require(table != null && table.getConceptId() != null, "概念表绑定必须明确所属概念");
        ConceptDO concept = concepts.selectById(table.getConceptId());
        return context.resolve(concept, null, null, null);
    }
    public OntologyDO source(ConceptTableDO table) {
        OntologyDO ontology = ownership(table);
        require(table.getDatasourceId() != null && table.getDatasourceId() > 0, "概念表来源不能为空");
        require(datasources.hasDatasourceSpace(table.getDatasourceId(), ontology.getSpaceId(), ontology.getSpaceCode()), "概念表数据源未有效关联本体空间");
        identifier(table.getTableName());
        return ontology;
    }
    public void connection(ConceptTableDO table, com.datamaster.common.database.constants.DbQueryProperty property) {
        require(table != null && property != null, "绑定或注册来源配置不可用");
        require(table.getDatabaseName() != null && Objects.equals(table.getDatabaseName(), property.getDbName()), "绑定库名与注册数据源不一致，禁止预览借连接查询其他来源");
        if ("PostgreSQL".equalsIgnoreCase(property.getDbType()) || "Kingbase8".equalsIgnoreCase(property.getDbType()))
            require(table.getSchemaName() != null && Objects.equals(table.getSchemaName(), property.getSid()), "绑定schema与注册来源不一致");
    }
    public String physicalTable(ConceptTableDO table, com.datamaster.common.database.constants.DbQueryProperty property) {
        connection(table, property); identifier(table.getTableName());
        if ("PostgreSQL".equalsIgnoreCase(property.getDbType()) || "Kingbase8".equalsIgnoreCase(property.getDbType())) {
            identifier(table.getSchemaName());
            return "\"" + table.getSchemaName() + "\".\"" + table.getTableName() + "\"";
        }
        return table.getTableName();
    }
    public void admit(ConceptTableDO table, OntologyDO ontology, List<String> columns) {
        if (columns != null) {
            require(!columns.isEmpty() && columns.size() <= 1000, "预览字段缺失或超限，禁止回退SELECT *");
            for (String column : columns) identifier(column);
        }
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO();
        request.setDatasourceId(table.getDatasourceId()); request.setTableName(table.getTableName());
        request.setSpaceId(ontology.getSpaceId()); request.setSpaceCode(ontology.getSpaceCode()); request.setColumnNames(columns);
        request.setEntrance("ONTOLOGY_BINDING_PREVIEW");
        AssetsTableGovernanceRespDTO result = governance.resolveTable(request);
        require(result != null && Boolean.TRUE.equals(result.getAccessAllowed()), "概念表预览来源未取得当前准入");
        if (columns == null && result.getAllowedColumns() != null) throw new com.datamaster.common.exception.ServiceException("部分字段授权不能用于列举全部物理字段，请使用明确映射的受控对象查询");
        require(result.getDeniedColumns() == null || result.getDeniedColumns().isEmpty(), "预览包含未授权字段");
        if (columns != null && result.getAllowedColumns() != null) {
            Set<String> allowed = new HashSet<>(); for (String column : result.getAllowedColumns()) if (column != null) allowed.add(column.toLowerCase(Locale.ROOT));
            for (String column : columns) require(allowed.contains(column.toLowerCase(Locale.ROOT)), "预览字段未全部授权");
        }
        if (columns != null && result.getAssetId() != null) {
            // Hidden inputs are rejected before reading; replacements still run on returned rows below.
            Set<String> hidden = governance.getDesensitizeHideColumns(result.getAssetId(), com.datamaster.common.utils.SecurityUtils.getUserId(), null, "2");
            require(hidden != null, "预览敏感字段核验不可用");
            for (String column : columns) for (String denied : hidden) require(!column.equalsIgnoreCase(denied), "预览含需隐藏的字段，请使用受控授权查询");
        }
    }
    public List<Map<String,Object>> mask(ConceptTableDO table, OntologyDO ontology, List<Map<String,Object>> rows) {
        require(rows != null, "预览结果不可用");
        Set<String> returned = new LinkedHashSet<>();
        if (rows != null) for (Map<String,Object> row : rows) { require(row != null, "预览结果行不可用"); returned.addAll(row.keySet()); }
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO(); request.setDatasourceId(table.getDatasourceId()); request.setTableName(table.getTableName());
        request.setColumnNames(new ArrayList<>(returned));
        request.setSpaceId(ontology.getSpaceId()); request.setSpaceCode(ontology.getSpaceCode()); request.setEntrance("ONTOLOGY_BINDING_PREVIEW");
        AssetsTableGovernanceRespDTO source = governance.resolveTable(request);
        require(source != null && Boolean.TRUE.equals(source.getAccessAllowed())
                && (source.getDeniedColumns() == null || source.getDeniedColumns().isEmpty()), "预览返回前准入或字段权限已失效");
        if (source.getAllowedColumns() != null) {
            Set<String> allowed = new HashSet<>(); for (String column : source.getAllowedColumns()) if (column != null) allowed.add(column.toLowerCase(Locale.ROOT));
            for (String column : returned) require(allowed.contains(column.toLowerCase(Locale.ROOT)), "预览返回前字段权限已失效");
        }
        if (source.getAssetId() == null || rows == null || rows.isEmpty()) return rows;
        try {
            List<Map<String,Object>> masked = governance.desensitizeResultData(source.getAssetId(), rows, com.datamaster.common.utils.SecurityUtils.getUserId(), null, "2");
            require(masked != null, "预览脱敏结果不可用"); return masked;
        } catch (RuntimeException error) { throw new com.datamaster.common.exception.ServiceException("预览脱敏失败，禁止返回原始行"); }
    }
    private void identifier(String value) { require(value != null && value.matches("[A-Za-z_][A-Za-z0-9_$]*"), "概念表预览只支持单一表/列标识符，不接受表达式或限定来源"); }
}
