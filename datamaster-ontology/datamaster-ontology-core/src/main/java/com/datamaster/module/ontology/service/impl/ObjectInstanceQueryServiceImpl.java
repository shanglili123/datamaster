package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.module.ontology.api.dto.SemanticPropertyDTO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryRespVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RelationJumpReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.dal.mapper.RelationTableMapper;
import com.datamaster.module.ontology.service.IObjectInstanceQueryService;
import com.datamaster.module.ontology.service.query.TypedQueryBuilder;
import com.datamaster.module.ontology.service.query.TypedQuerySpec;
import com.datamaster.neo4j.service.LineageDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 对象实例层查询实现 — 管理端对象浏览器
 *
 * 查询链路：解析概念表绑定 → 解析过滤条件 → 复用 checkTableAccess 表级权限
 * → DbQuery 分页查询物理表 → 返回对象实例行。
 */
@Service
@Validated
public class ObjectInstanceQueryServiceImpl implements IObjectInstanceQueryService {

    private static final Logger log = LoggerFactory.getLogger(ObjectInstanceQueryServiceImpl.class);

    /** 治理 API 入口：对象实例只读查询（表级权限校验语义） */
    private static final String ENTRANCE_ONTOLOGY_OBJECT_QUERY = "ONTOLOGY_OBJECT_QUERY";

    /** 单页最大行数上限，防止超大分页拖垮外部数据源 */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 对象血缘写入（可插拔）：LINEAGE_ENABLED=true 时存在；未开启/未部署 Neo4j 时注入为 null。
     */
    @Autowired(required = false)
    private LineageDataService lineageDataService;

    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private PropertyColumnMapper propertyColumnMapper;
    @Resource
    private PropertyMapper propertyMapper;
    @Resource
    private IDatasourceApiService datasourceApiService;
    @Resource
    private DataSourceFactory dataSourceFactory;
    @Resource
    private IAssetsTableGovernanceApiService tableGovernanceApiService;
    @Resource
    private RelationMapper relationMapper;
    @Resource
    private RelationColumnMapper relationColumnMapper;
    @Resource
    private RelationTableMapper relationTableMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource private com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext consumptionContext;
    @Resource private com.datamaster.module.governance.api.flow.ISpaceFlowApi spaceFlows;
    @Resource private com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard sourceIdentity;

    @Override
    public ObjectInstanceQueryRespVO queryObjects(ObjectInstanceQueryReqVO reqVO) {
        // 1. 解析概念与表绑定
        ConceptDO concept = resolveConcept(reqVO);
        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumptionContext.resolve(concept, reqVO.getOntologyId(), reqVO.getSpaceId(), reqVO.getSpaceCode());
        reqVO.setSpaceId(ontology.getSpaceId()); reqVO.setSpaceCode(ontology.getSpaceCode()); reqVO.setOntologyId(ontology.getId());
        ConceptTableDO binding = resolveTableBinding(reqVO, concept.getId());

        // 2. 组装属性语义映射（物理列 → 语义）
        List<SemanticPropertyDTO> properties = buildProperties(binding.getId(), concept.getId());

        // 3. 表级权限校验（复用统一治理入口）
        assertTableAccess(binding, reqVO.getSpaceId(), reqVO.getSpaceCode());

        // 4. 解析数据源并执行查询
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(binding.getDatasourceId());
        if (ds == null || Boolean.FALSE.equals(ds.getValidFlag())) {
            throw new RuntimeException("数据源不存在: id=" + binding.getDatasourceId());
        }

        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        sourceIdentity.connection(binding, property);
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            ObjectInstanceQueryRespVO result = doQuery(reqVO, concept, binding, properties, dbQuery, property);
            // Record only an admitted, successfully read/masked consumption, never a rejected attempt.
            writeObjectLineageSilently(concept, binding, ds);
            return result;
        } finally {
            if (dbQuery != null) {
                try {
                    dbQuery.close();
                } catch (Exception e) {
                    // 忽略关闭失败
                }
            }
        }
    }

    // ==================== 内部实现 ====================

    @Override
    public ObjectInstanceQueryRespVO queryRelatedObjects(RelationJumpReqVO reqVO) {
        if (reqVO == null || reqVO.getSourceValues() != null && reqVO.getSourceValues().size() > 100) throw new com.datamaster.common.exception.ServiceException("关系跳转源值最多100项");
        if (reqVO.getSourceConceptId() == null) {
            throw new RuntimeException("关系跳转缺少源概念ID");
        }
        if (reqVO.getRelationId() == null) {
            throw new RuntimeException("关系跳转缺少关系ID");
        }
        // 1. 源概念与源表绑定（用于源表权限校验 + 定位关联字段的 sourceConceptTableId）
        ConceptDO sourceConcept = conceptMapper.selectById(reqVO.getSourceConceptId());
        if (sourceConcept == null) {
            throw new RuntimeException("源概念不存在: id=" + reqVO.getSourceConceptId());
        }
        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumptionContext.resolve(sourceConcept, null, reqVO.getSpaceId(), reqVO.getSpaceCode());
        reqVO.setSpaceId(ontology.getSpaceId()); reqVO.setSpaceCode(ontology.getSpaceCode());
        ConceptTableDO sourceBinding = resolveBinding(reqVO.getSourceConceptId(), reqVO.getSourceTableBindingId());
        assertTableAccess(sourceBinding, reqVO.getSpaceId(), reqVO.getSpaceCode());

        // 2. 关系 + 关联字段绑定
        RelationDO relation = relationMapper.selectById(reqVO.getRelationId());
        if (relation == null) {
            throw new RuntimeException("关系不存在: id=" + reqVO.getRelationId());
        }
        if (!Objects.equals(relation.getOntologyId(), sourceConcept.getOntologyId()) || !Objects.equals(relation.getSourceConceptId(), sourceConcept.getId())) throw new com.datamaster.common.exception.ServiceException("关系不属于当前本体或源概念");
        List<RelationColumnDO> relationColumns = relationColumnMapper.selectByRelationId(reqVO.getRelationId());
        RelationColumnDO colBinding = resolveRelationColumn(sourceBinding.getId(), relationColumns);
        if (colBinding == null || colBinding.getTargetConceptTableId() == null
                || colBinding.getTargetColumn() == null || colBinding.getSourceColumn() == null) {
            throw new RuntimeException("关系未配置可跳转的关联字段: relationId=" + reqVO.getRelationId());
        }

        assertQueryColumns(sourceBinding, reqVO.getSpaceId(), reqVO.getSpaceCode(), java.util.List.of(colBinding.getSourceColumn()));
        // 3. 目标表绑定 + 目标概念（语义名）+ 权限校验（每个被触达物理表各自校验，d6）
        ConceptTableDO targetBinding = conceptTableMapper.selectById(colBinding.getTargetConceptTableId());
        if (targetBinding == null) {
            throw new RuntimeException("关系目标表绑定不存在: id=" + colBinding.getTargetConceptTableId());
        }
        ConceptDO targetConcept = relation.getTargetConceptId() != null
                ? conceptMapper.selectById(relation.getTargetConceptId())
                : null;
        if (targetConcept == null || !Objects.equals(targetBinding.getConceptId(), targetConcept.getId())) throw new com.datamaster.common.exception.ServiceException("关系目标绑定与概念不一致");
        consumptionContext.resolve(targetConcept, sourceConcept.getOntologyId(), reqVO.getSpaceId(), reqVO.getSpaceCode());
        assertTableAccess(targetBinding, reqVO.getSpaceId(), reqVO.getSpaceCode());

        // 4. 独立关系表先解析主体值对应的客体端点值；主体表/客体表自身存外键时直接沿字段映射查询。
        List<Object> targetValues = reqVO.getSourceValues() == null
                ? Collections.emptyList() : reqVO.getSourceValues();
        List<RelationTableDO> relationTables = relationTableMapper.selectByRelationId(reqVO.getRelationId());
        if (relationTables != null && !relationTables.isEmpty()) {
            if (relationTables.size() != 1) throw new com.datamaster.common.exception.ServiceException("关系存在多张中间表，当前无法证明完整路径，禁止只取第一张");
            RelationTableDO relationTable = relationTables.get(0);
            boolean endpointTable = samePhysicalTable(relationTable, sourceBinding)
                    || samePhysicalTable(relationTable, targetBinding);
            if (!endpointTable) {
                assertRelationTableAccess(relationTable, reqVO.getSpaceId(), reqVO.getSpaceCode());
                targetValues = queryJunctionTargetValues(relationTable, targetValues);
            }
        }

        // 5. 目标数据源 + 执行
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(targetBinding.getDatasourceId());
        if (ds == null || Boolean.FALSE.equals(ds.getValidFlag())) {
            throw new RuntimeException("数据源不存在: id=" + targetBinding.getDatasourceId());
        }
        List<SemanticPropertyDTO> targetProperties = buildProperties(targetBinding.getId(), targetConcept.getId());
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        sourceIdentity.connection(targetBinding, property);
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            return doRelatedQuery(reqVO, colBinding, targetBinding, targetConcept, targetProperties,
                    targetValues, dbQuery, property);
        } finally {
            if (dbQuery != null) {
                try {
                    dbQuery.close();
                } catch (Exception e) {
                    // 忽略关闭失败
                }
            }
        }
    }

    /**
     * 关系跳转的目标查询：必带 targetColumn IN/=(源值) 过滤，叠加类型化过滤器 + 排序 + 投影 + 分页。
     * 全程列白名单校验 + NamedParameter 防注入，不做跨表物理 JOIN（d8）。
     */
    private ObjectInstanceQueryRespVO doRelatedQuery(RelationJumpReqVO reqVO, RelationColumnDO colBinding,
                                                     ConceptTableDO targetBinding, ConceptDO targetConcept,
                                                     List<SemanticPropertyDTO> targetProperties,
                                                     List<Object> relationValues,
                                                     DbQuery dbQuery, DbQueryProperty property) {
        int pageNum = reqVO.getPageNum() == null ? 1 : reqVO.getPageNum();
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.min(reqVO.getPageSize(), MAX_PAGE_SIZE);
        if (pageNum < 1 || pageSize < 1 || pageNum > 1000000) throw new com.datamaster.common.exception.ServiceException("对象分页参数无效");
        long offset = (long) (pageNum - 1) * pageSize;

        List<String> columns = new ArrayList<>();
        java.util.Set<String> textColumns = new java.util.HashSet<>();
        try {
            for (DbColumn col : dbQuery.getTableColumns(property, targetBinding.getTableName())) {
                columns.add(col.getColName());
                if (TypedQueryBuilder.isTextType(col.getDataType())) {
                    textColumns.add(col.getColName());
                }
            }
        } catch (Exception e) {
            columns = Collections.emptyList();
        }
        assertQueryColumns(targetBinding, reqVO.getSpaceId(), reqVO.getSpaceCode(), columns);
        List<String> whitelist = columns.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(columns);

        // 类型化过滤：额外目标收敛 + 排序 + 投影 + 关键字（跨列模糊仅作用于文本列）
        TypedQuerySpec spec = TypedQueryBuilder.parseStrict(reqVO.getFilters());
        TypedQueryBuilder.Compiled compiled = TypedQueryBuilder.compileStrict(spec, whitelist, textColumns);

        Map<String, Object> queryParams = new LinkedHashMap<>(compiled.params);
        List<String> whereParts = new ArrayList<>();
        if (compiled.hasWhere()) {
            whereParts.add("(" + compiled.where + ")");
        }

        // 必带关系过滤：targetColumn IN (源值)。源值必须非空；列必须命中白名单。
        List<Object> sourceValues = relationValues == null ? Collections.emptyList() : relationValues;
        String targetCol = colBinding.getTargetColumn();
        if (!isSafeColumn(targetCol) || (!whitelist.isEmpty() && !whitelist.contains(targetCol))) {
            throw new RuntimeException("关系目标字段不属于目标对象表: " + targetCol);
        }
        if (sourceValues.isEmpty()) {
            // 独立关系表未查到端点时必须返回空集，不能退化为查询全部目标对象。
            whereParts.add("1 = 0");
        } else {
            List<String> placeholders = new ArrayList<>();
            int idx = 0;
            for (Object v : sourceValues) {
                String p = "rel" + (idx++);
                queryParams.put(p, v);
                placeholders.add(":" + p);
            }
            whereParts.add(targetCol + " IN (" + String.join(", ", placeholders) + ")");
        }

        String physicalTable = sourceIdentity.physicalTable(targetBinding, property);
        StringBuilder sql = new StringBuilder();
        if (compiled.hasProjection()) {
            sql.append("SELECT ").append(compiled.projection).append(" FROM ").append(physicalTable);
        } else {
            sql.append("SELECT ").append(String.join(", ", columns)).append(" FROM ").append(physicalTable);
        }
        if (!whereParts.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereParts));
        }
        if (compiled.hasOrderBy()) {
            sql.append(" ORDER BY ").append(compiled.orderBy);
        }

        com.datamaster.common.database.core.PageResult<Map<String, Object>> pageResult =
                dbQuery.queryByPage(sql.toString(), queryParams, offset, pageSize, 0);
        if (pageResult == null || pageResult.getData() == null || pageResult.getTotal() == null || pageResult.getTotal() < 0)
            throw new com.datamaster.common.exception.ServiceException("对象查询未取得有效分页结果，不能当作空数据");
        List<Map<String, Object>> rows = pageResult.getData();
        int total = pageResult.getTotal();
        List<String> returnedColumns = compiled.hasProjection() ? new ArrayList<>(spec.getColumns()) : columns;

        ObjectInstanceQueryRespVO respVO = new ObjectInstanceQueryRespVO();
        respVO.setConceptId(targetBinding.getConceptId());
        respVO.setConceptName(targetConcept != null ? targetConcept.getName() : null);
        respVO.setTableBindingId(targetBinding.getId());
        respVO.setTableName(targetBinding.getTableName());
        respVO.setColumns(returnedColumns);
        respVO.setProperties(targetProperties);
        respVO.setRows(maskObjectRows(targetBinding, reqVO.getSpaceId(), reqVO.getSpaceCode(), rows));
        respVO.setTotal((long) total);
        return respVO;
    }

    /** 独立关系表：按主体端点查出客体端点值，再交给目标对象查询。 */
    private List<Object> queryJunctionTargetValues(RelationTableDO table, List<Object> sourceValues) {
        if (sourceValues == null || sourceValues.isEmpty()) {
            return Collections.emptyList();
        }
        String[] endpoints = relationTableEndpointColumns(table);
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(table.getDatasourceId());
        if (ds == null || Boolean.FALSE.equals(ds.getValidFlag())) {
            throw new RuntimeException("关系表数据源不存在: id=" + table.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        ConceptTableDO relationSource = new ConceptTableDO(); relationSource.setDatasourceId(table.getDatasourceId()); relationSource.setDatabaseName(table.getDatabaseName()); relationSource.setSchemaName(table.getSchemaName()); relationSource.setTableName(table.getTableName());
        sourceIdentity.connection(relationSource, property);
        DbQuery query = dataSourceFactory.createDbQuery(property);
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            List<String> placeholders = new ArrayList<>();
            for (int i = 0; i < sourceValues.size(); i++) {
                String key = "source" + i;
                params.put(key, sourceValues.get(i));
                placeholders.add(":" + key);
            }
            String sql = "SELECT " + endpoints[1] + " AS relation_target FROM " + sourceIdentity.physicalTable(relationSource, property)
                    + " WHERE " + endpoints[0] + " IN (" + String.join(", ", placeholders) + ")";
            com.datamaster.common.database.core.PageResult<Map<String,Object>> page = query.queryByPage(sql, params, 0, 101, 0);
            if (page == null || page.getData() == null) throw new com.datamaster.common.exception.ServiceException("关系端点查询未取得有效结果，不能当作无关系");
            List<Map<String,Object>> rows = page.getData();
            if (page.getTotal() != null && page.getTotal() > 100 || rows != null && rows.size() > 100) throw new com.datamaster.common.exception.ServiceException("关系端点超过100项，请缩小源对象范围，不能截断后冒充完整关系");
            List<Object> values = new ArrayList<>();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object value = getIgnoreCase(row, "relation_target");
                    if (value != null && !values.contains(value)) values.add(value);
                }
            }
            return values;
        } finally {
            if (query != null) {
                try {
                    query.close();
                } catch (Exception ignore) {
                    // 忽略关闭失败
                }
            }
        }
    }

    private String[] relationTableEndpointColumns(RelationTableDO table) {
        try {
            JsonNode node = objectMapper.readTree(table.getColumnNames() == null ? "[]" : table.getColumnNames());
            String source = node.isArray() && node.size() > 0
                    ? node.get(0).asText(null) : node.path("sourceColumn").asText(null);
            String target = node.isArray() && node.size() > 1
                    ? node.get(1).asText(null) : node.path("targetColumn").asText(null);
            if (!isSafeColumn(source) || !isSafeColumn(target)) {
                throw new RuntimeException("关系表未正确配置主体列和客体列");
            }
            return new String[]{source, target};
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("关系表端点字段解析失败: " + e.getMessage(), e);
        }
    }

    private boolean isSafeColumn(String column) {
        return column != null && column.matches("[A-Za-z_][A-Za-z0-9_$]*");
    }

    private Object getIgnoreCase(Map<String, Object> row, String key) {
        if (row == null || key == null) return null;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private void assertRelationTableAccess(RelationTableDO table, Long spaceId, String spaceCode) {
        ConceptTableDO binding = new ConceptTableDO();
        binding.setDatasourceId(table.getDatasourceId());
        binding.setTableName(table.getTableName());
        assertTableAccess(binding, spaceId, spaceCode);
        assertQueryColumns(binding, spaceId, spaceCode, java.util.Arrays.asList(relationTableEndpointColumns(table)));
    }

    private boolean samePhysicalTable(RelationTableDO relationTable, ConceptTableDO conceptTable) {
        if (relationTable == null || conceptTable == null
                || relationTable.getDatasourceId() == null || conceptTable.getDatasourceId() == null
                || !Objects.equals(relationTable.getDatasourceId(), conceptTable.getDatasourceId())
                || relationTable.getTableName() == null || conceptTable.getTableName() == null
                || !Objects.equals(relationTable.getTableName(), conceptTable.getTableName())) {
            return false;
        }
        return Objects.equals(relationTable.getDatabaseName(), conceptTable.getDatabaseName())
                && Objects.equals(relationTable.getSchemaName(), conceptTable.getSchemaName());
    }

    private RelationColumnDO resolveRelationColumn(Long sourceBindingId, List<RelationColumnDO> mappings) {
        RelationColumnDO selected = null;
        if (mappings != null) for (RelationColumnDO mapping : mappings) {
            if (mapping == null) throw new com.datamaster.common.exception.ServiceException("关系字段映射损坏，请先维护");
            if (Objects.equals(sourceBindingId, mapping.getSourceConceptTableId())) {
                if (selected != null) throw new com.datamaster.common.exception.ServiceException("当前来源存在多条关系字段映射，不能只取第一条执行");
                selected = mapping;
            }
        }
        return selected;
    }

    /** Resolve an explicit binding, or the sole stored binding. Never guess among multiple sources. */
    private ConceptTableDO resolveBinding(Long conceptId, Long tableBindingId) {
        List<ConceptTableDO> bindings = conceptTableMapper.selectByConceptId(conceptId);
        if (bindings == null || bindings.isEmpty()) throw new com.datamaster.common.exception.ServiceException("概念未绑定物理表");
        if (tableBindingId == null && bindings.size() != 1) throw new com.datamaster.common.exception.ServiceException("概念绑定多张表，请明确选择本次对象查询的表绑定");
        ConceptTableDO selected = null;
        for (ConceptTableDO binding : bindings) {
            if (binding == null || binding.getId() == null || !Objects.equals(conceptId, binding.getConceptId())) throw new com.datamaster.common.exception.ServiceException("概念表绑定归属无效，请先维护");
            if (tableBindingId == null || tableBindingId.equals(binding.getId())) {
                if (selected != null) throw new com.datamaster.common.exception.ServiceException("概念表绑定标识不唯一");
                selected = binding;
            }
        }
        if (selected == null) throw new com.datamaster.common.exception.ServiceException("指定表绑定不属于当前概念");
        if (selected.getDatasourceId() == null || selected.getDatasourceId() <= 0 || selected.getTableName() == null || selected.getTableName().isBlank()) throw new com.datamaster.common.exception.ServiceException("对象集缺少有效数据源或物理表配置");
        return selected;
    }

    private ConceptDO resolveConcept(ObjectInstanceQueryReqVO reqVO) {
        if (reqVO == null || reqVO.getConceptId() == null) {
            throw new RuntimeException("对象实例查询缺少概念ID");
        }
        ConceptDO concept = conceptMapper.selectById(reqVO.getConceptId());
        if (concept == null) {
            throw new RuntimeException("概念不存在: id=" + reqVO.getConceptId());
        }
        return concept;
    }

    private ConceptTableDO resolveTableBinding(ObjectInstanceQueryReqVO reqVO, Long conceptId) {
        return resolveBinding(conceptId, reqVO.getTableBindingId());
    }

    private List<SemanticPropertyDTO> buildProperties(Long conceptTableId, Long conceptId) {
        List<PropertyColumnDO> propertyColumns = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        if (propertyColumns == null) throw new com.datamaster.common.exception.ServiceException("对象字段映射读取不可用，禁止回退物理字段");
        if (propertyColumns.isEmpty()) {
            return Collections.emptyList();
        }
        List<SemanticPropertyDTO> properties = new ArrayList<>();
        Map<Long, PropertyDO> propertyMap = new LinkedHashMap<>();
        for (PropertyColumnDO pc : propertyColumns) {
            if (pc == null || pc.getPropertyId() == null || pc.getColumnName() == null || pc.getColumnName().isBlank()) throw new com.datamaster.common.exception.ServiceException("对象属性映射不完整，禁止丢弃后回退其他字段");
            PropertyDO prop = propertyMap.computeIfAbsent(pc.getPropertyId(), propertyMapper::selectById);
            if (prop == null || !Objects.equals(conceptId, prop.getConceptId())) throw new com.datamaster.common.exception.ServiceException("对象映射属性不属于当前概念");
            SemanticPropertyDTO dto = new SemanticPropertyDTO();
            dto.setPropertyId(prop.getId());
            dto.setPropertyName(prop.getName());
            dto.setPropertyCode(prop.getCode());
            dto.setDataType(prop.getDataType());
            dto.setPropertyDescription(prop.getDescription());
            dto.setIsPrimary(prop.getIsPrimary());
            dto.setPhysicalColumnName(pc.getColumnName());
            properties.add(dto);
        }
        return properties;
    }

    private void assertQueryColumns(ConceptTableDO binding, Long spaceId, String spaceCode, List<String> columns) {
        spaceFlows.policy(spaceId, spaceCode);
        if (columns == null || columns.isEmpty()) throw new com.datamaster.common.exception.ServiceException("对象查询无法确认参与字段，禁止回退SELECT *，请维护映射或元数据");
        if (columns != null) for (String column : columns) if (!isSafeColumn(column)) throw new com.datamaster.common.exception.ServiceException("对象字段映射不是安全标识符");
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO(); request.setDatasourceId(binding.getDatasourceId()); request.setTableName(binding.getTableName());
        request.setSpaceId(spaceId); request.setSpaceCode(spaceCode); request.setColumnNames(columns); request.setEntrance(ENTRANCE_ONTOLOGY_OBJECT_QUERY);
        com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO response = tableGovernanceApiService.resolveTable(request);
        if (response == null || !Boolean.TRUE.equals(response.getAccessAllowed()) || response.getDeniedColumns() != null && !response.getDeniedColumns().isEmpty()) throw new com.datamaster.common.exception.ServiceException("对象查询存在无权参与的字段，请先维护映射和字段授权");
        if (response.getAllowedColumns() != null) {
            java.util.Set<String> allowed = new java.util.HashSet<>(); for (String column : response.getAllowedColumns()) if (column != null) allowed.add(column.toLowerCase(java.util.Locale.ROOT));
            for (String column : columns) if (!allowed.contains(column.toLowerCase(java.util.Locale.ROOT))) throw new com.datamaster.common.exception.ServiceException("对象查询参与字段未全部授权");
        }
        if (response.getAssetId() != null) {
            java.util.Set<String> hidden = tableGovernanceApiService.getDesensitizeHideColumns(response.getAssetId(), com.datamaster.common.utils.SecurityUtils.getUserId(), null, "2");
            if (hidden == null) throw new com.datamaster.common.exception.ServiceException("对象隐藏字段核验不可用");
            for (String column : columns) for (String denied : hidden) if (column.equalsIgnoreCase(denied)) throw new com.datamaster.common.exception.ServiceException("对象查询包含需隐藏字段，请调整映射或权限");
        }
    }

    private List<Map<String,Object>> maskObjectRows(ConceptTableDO binding, Long spaceId, String spaceCode, List<Map<String,Object>> rows) {
        java.util.Set<String> columns = new java.util.LinkedHashSet<>();
        for (Map<String,Object> row : rows) {
            if (row == null) throw new com.datamaster.common.exception.ServiceException("对象返回结构无效");
            columns.addAll(row.keySet());
        }
        AssetsTableGovernanceReqDTO request = new AssetsTableGovernanceReqDTO(); request.setDatasourceId(binding.getDatasourceId()); request.setTableName(binding.getTableName());
        request.setSpaceId(spaceId); request.setSpaceCode(spaceCode); request.setColumnNames(new ArrayList<>(columns)); request.setEntrance(ENTRANCE_ONTOLOGY_OBJECT_QUERY);
        com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO response = tableGovernanceApiService.resolveTable(request);
        if (response == null || !Boolean.TRUE.equals(response.getAccessAllowed()) || response.getDeniedColumns() != null && !response.getDeniedColumns().isEmpty()) throw new com.datamaster.common.exception.ServiceException("对象结果返回前准入已失效");
        if (response.getAllowedColumns() != null) {
            java.util.Set<String> allowed = new java.util.HashSet<>();
            for (String column : response.getAllowedColumns()) if (column != null) allowed.add(column.toLowerCase(java.util.Locale.ROOT));
            for (String column : columns) if (!allowed.contains(column.toLowerCase(java.util.Locale.ROOT))) throw new com.datamaster.common.exception.ServiceException("对象结果字段权限已失效");
        }
        if (response.getAssetId() == null || rows.isEmpty()) return rows;
        try {
            List<Map<String,Object>> masked = tableGovernanceApiService.desensitizeResultData(response.getAssetId(), rows, com.datamaster.common.utils.SecurityUtils.getUserId(), null, "2");
            if (masked == null) throw new IllegalStateException();
            return masked;
        } catch (RuntimeException failed) { throw new com.datamaster.common.exception.ServiceException("对象结果脱敏失败，禁止返回原始行"); }
    }

    private void assertTableAccess(ConceptTableDO binding, Long spaceId, String spaceCode) {
        if (!datasourceApiService.hasDatasourceSpace(binding.getDatasourceId(), spaceId, spaceCode)) throw new com.datamaster.common.exception.ServiceException("对象数据源未有效关联当前本体空间");
        AssetsTableGovernanceReqDTO reqDTO = new AssetsTableGovernanceReqDTO();
        reqDTO.setDatasourceId(binding.getDatasourceId());
        reqDTO.setTableName(binding.getTableName());
        reqDTO.setSpaceId(spaceId);
        reqDTO.setSpaceCode(spaceCode);
        reqDTO.setEntrance(ENTRANCE_ONTOLOGY_OBJECT_QUERY);
        tableGovernanceApiService.checkTableAccess(reqDTO);
    }

    /**
     * 对象血缘「数据维度」写钩子（可插拔）：
     * 实例被查询时，把「对象(概念) -[MATERIALIZES]-&gt; 物理表」落 Neo4j。
     * 写入失败仅记 warn，绝不影响对象查询主流程。
     */
    private void writeObjectLineageSilently(ConceptDO concept, ConceptTableDO binding, DatasourceRespDTO ds) {
        if (lineageDataService == null) {
            return;
        }
        // Neo4j 只是血缘旁路，不能阻塞对象数据查询。Neo4j 未启动、网络超时或写入较慢时，
        // 原来的同步调用会让对象面板一直转圈，而资产预览不经过这条链路。
        CompletableFuture.runAsync(() -> {
            try {
                String hostPort = ds.getIp() + ":" + ds.getPort();
                lineageDataService.saveObjectLineage(
                        concept.getId(),
                        concept.getOntologyId(),
                        concept.getCode(),
                        concept.getName(),
                        binding.getTableName(),
                        hostPort,
                        binding.getDatabaseName(),
                        binding.getSchemaName());
            } catch (Exception e) {
                log.warn("对象血缘写入失败，不影响查询结果: {}", e.getMessage());
            }
        });
    }

    private ObjectInstanceQueryRespVO doQuery(ObjectInstanceQueryReqVO reqVO, ConceptDO concept,
                                              ConceptTableDO binding,
                                              List<SemanticPropertyDTO> properties, DbQuery dbQuery,
                                              DbQueryProperty property) {
        int pageNum = reqVO.getPageNum() == null ? 1 : reqVO.getPageNum();
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.min(reqVO.getPageSize(), MAX_PAGE_SIZE);
        if (pageNum < 1 || pageSize < 1 || pageNum > 1000000) throw new com.datamaster.common.exception.ServiceException("对象分页参数无效");
        long offset = (long) (pageNum - 1) * pageSize;

        // Columns come from verified mappings or physical metadata; unavailable columns never fall back to SELECT *.
        List<String> columns = new ArrayList<>();
        java.util.Set<String> textColumns = new java.util.HashSet<>();
        // 本体属性映射已经是可信字段白名单，优先使用它，避免每次打开对象都再次连接外部库读取
        // information_schema。只有没有任何属性映射时才回退到物理表元数据查询。
        if (properties != null) {
            for (SemanticPropertyDTO mapped : properties) {
                if (mapped == null || StringUtils.isBlank(mapped.getPhysicalColumnName())) continue;
                columns.add(mapped.getPhysicalColumnName());
                if (TypedQueryBuilder.isTextType(mapped.getDataType())) {
                    textColumns.add(mapped.getPhysicalColumnName());
                }
            }
        }
        if (columns.isEmpty()) {
            try {
                for (DbColumn col : dbQuery.getTableColumns(property, binding.getTableName())) {
                    columns.add(col.getColName());
                    if (TypedQueryBuilder.isTextType(col.getDataType())) {
                        textColumns.add(col.getColName());
                    }
                }
            } catch (Exception e) {
                columns = Collections.emptyList();
            }
        }

        // 解析类型化过滤条件（结构化 JSON：分组/与或非/运算符/排序/投影/关键字），统一白名单校验 + NamedParameter；
        // 跨列模糊仅作用于文本列，避免数值/日期列 LIKE 类型错误
        assertQueryColumns(binding, reqVO.getSpaceId(), reqVO.getSpaceCode(), columns);
        List<String> whitelist = columns.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(columns);
        TypedQuerySpec spec = TypedQueryBuilder.parseStrict(reqVO.getFilters());
        TypedQueryBuilder.Compiled compiled = TypedQueryBuilder.compileStrict(spec, whitelist, textColumns);

        String physicalTable = sourceIdentity.physicalTable(binding, property);
        StringBuilder sql = new StringBuilder();
        if (compiled.hasProjection()) {
            sql.append("SELECT ").append(compiled.projection).append(" FROM ").append(physicalTable);
        } else {
            sql.append("SELECT ").append(String.join(", ", columns)).append(" FROM ").append(physicalTable);
        }
        if (compiled.hasWhere()) {
            sql.append(" WHERE ").append(compiled.where);
        }
        if (compiled.hasOrderBy()) {
            sql.append(" ORDER BY ").append(compiled.orderBy);
        }

        // 分页查询（内部按方言分页并统计总数，兼容 LIMIT/OFFSET 与 ROWNUM 等方言差异）
        com.datamaster.common.database.core.PageResult<Map<String, Object>> pageResult =
                dbQuery.queryByPage(sql.toString(), compiled.params, offset, pageSize, 0);
        if (pageResult == null || pageResult.getData() == null || pageResult.getTotal() == null || pageResult.getTotal() < 0)
            throw new com.datamaster.common.exception.ServiceException("对象查询未取得有效分页结果，不能当作空数据");
        List<Map<String, Object>> rows = pageResult.getData();
        int total = pageResult.getTotal();
        List<String> returnedColumns = compiled.hasProjection() ? new ArrayList<>(spec.getColumns()) : columns;

        ObjectInstanceQueryRespVO respVO = new ObjectInstanceQueryRespVO();
        respVO.setConceptId(concept.getId());
        respVO.setConceptName(concept.getName());
        respVO.setTableBindingId(binding.getId());
        respVO.setTableName(binding.getTableName());
        respVO.setColumns(returnedColumns);
        respVO.setProperties(properties);
        respVO.setRows(maskObjectRows(binding, reqVO.getSpaceId(), reqVO.getSpaceCode(), rows));
        respVO.setTotal((long) total);
        return respVO;
    }
}
