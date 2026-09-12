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

    @Override
    public ObjectInstanceQueryRespVO queryObjects(ObjectInstanceQueryReqVO reqVO) {
        // 1. 解析概念与表绑定
        ConceptDO concept = resolveConcept(reqVO);
        ConceptTableDO binding = resolveTableBinding(reqVO, concept.getId());

        // 2. 组装属性语义映射（物理列 → 语义）
        List<SemanticPropertyDTO> properties = buildProperties(binding.getId());

        // 3. 表级权限校验（复用统一治理入口）
        assertTableAccess(binding, reqVO.getSpaceId(), reqVO.getSpaceCode());

        // 4. 解析数据源并执行查询
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(binding.getDatasourceId());
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + binding.getDatasourceId());
        }

        // 4.5 对象血缘「数据维度」写钩子：实例查询时把对象-物理表 MATERIALIZES 落图（可插拔）
        writeObjectLineageSilently(concept, binding, ds);

        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            return doQuery(reqVO, concept, binding, properties, dbQuery, property);
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
        ConceptTableDO sourceBinding = resolveBinding(reqVO.getSourceConceptId(), reqVO.getSourceTableBindingId());
        assertTableAccess(sourceBinding, reqVO.getSpaceId(), reqVO.getSpaceCode());

        // 2. 关系 + 关联字段绑定
        RelationDO relation = relationMapper.selectById(reqVO.getRelationId());
        if (relation == null) {
            throw new RuntimeException("关系不存在: id=" + reqVO.getRelationId());
        }
        List<RelationColumnDO> relationColumns = relationColumnMapper.selectByRelationId(reqVO.getRelationId());
        RelationColumnDO colBinding = null;
        for (RelationColumnDO rc : relationColumns) {
            if (sourceBinding.getId().equals(rc.getSourceConceptTableId())) {
                colBinding = rc;
                break;
            }
        }
        if (colBinding == null || colBinding.getTargetConceptTableId() == null
                || colBinding.getTargetColumn() == null) {
            throw new RuntimeException("关系未配置可跳转的关联字段: relationId=" + reqVO.getRelationId());
        }

        // 3. 目标表绑定 + 目标概念（语义名）+ 权限校验（每个被触达物理表各自校验，d6）
        ConceptTableDO targetBinding = conceptTableMapper.selectById(colBinding.getTargetConceptTableId());
        if (targetBinding == null) {
            throw new RuntimeException("关系目标表绑定不存在: id=" + colBinding.getTargetConceptTableId());
        }
        ConceptDO targetConcept = relation.getTargetConceptId() != null
                ? conceptMapper.selectById(relation.getTargetConceptId())
                : null;
        assertTableAccess(targetBinding, reqVO.getSpaceId(), reqVO.getSpaceCode());

        // 4. 独立关系表先解析主体值对应的客体端点值；主体表/客体表自身存外键时直接沿字段映射查询。
        List<Object> targetValues = reqVO.getSourceValues() == null
                ? Collections.emptyList() : reqVO.getSourceValues();
        List<RelationTableDO> relationTables = relationTableMapper.selectByRelationId(reqVO.getRelationId());
        if (relationTables != null && !relationTables.isEmpty()) {
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
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + targetBinding.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            List<SemanticPropertyDTO> targetProperties = buildProperties(targetBinding.getId());
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
        List<String> whitelist = columns.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(columns);

        // 类型化过滤：额外目标收敛 + 排序 + 投影 + 关键字（跨列模糊仅作用于文本列）
        TypedQuerySpec spec = TypedQueryBuilder.parse(reqVO.getFilters());
        TypedQueryBuilder.Compiled compiled = TypedQueryBuilder.compile(spec, whitelist, textColumns);

        Map<String, Object> queryParams = new LinkedHashMap<>(compiled.params);
        List<String> whereParts = new ArrayList<>();
        if (compiled.hasWhere()) {
            whereParts.add(compiled.where);
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

        StringBuilder sql = new StringBuilder();
        if (compiled.hasProjection()) {
            sql.append("SELECT ").append(compiled.projection).append(" FROM ").append(targetBinding.getTableName());
        } else {
            sql.append("SELECT * FROM ").append(targetBinding.getTableName());
        }
        if (!whereParts.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereParts));
        }
        if (compiled.hasOrderBy()) {
            sql.append(" ORDER BY ").append(compiled.orderBy);
        }

        com.datamaster.common.database.core.PageResult<Map<String, Object>> pageResult =
                dbQuery.queryByPage(sql.toString(), queryParams, offset, pageSize, 0);
        List<Map<String, Object>> rows = pageResult.getData() == null
                ? Collections.emptyList() : pageResult.getData();
        int total = pageResult.getTotal() == null ? 0 : pageResult.getTotal();
        if (columns.isEmpty() && !rows.isEmpty()) {
            columns = new ArrayList<>(rows.get(0).keySet());
        }

        ObjectInstanceQueryRespVO respVO = new ObjectInstanceQueryRespVO();
        respVO.setConceptId(targetBinding.getConceptId());
        respVO.setConceptName(targetConcept != null ? targetConcept.getName() : null);
        respVO.setTableBindingId(targetBinding.getId());
        respVO.setTableName(targetBinding.getTableName());
        respVO.setColumns(columns);
        respVO.setProperties(targetProperties);
        respVO.setRows(rows);
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
        if (ds == null) {
            throw new RuntimeException("关系表数据源不存在: id=" + table.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery query = dataSourceFactory.createDbQuery(property);
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            List<String> placeholders = new ArrayList<>();
            for (int i = 0; i < sourceValues.size(); i++) {
                String key = "source" + i;
                params.put(key, sourceValues.get(i));
                placeholders.add(":" + key);
            }
            String sql = "SELECT " + endpoints[1] + " AS relation_target FROM " + table.getTableName()
                    + " WHERE " + endpoints[0] + " IN (" + String.join(", ", placeholders) + ")";
            List<Map<String, Object>> rows = query.queryList(sql, params, 0);
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
    }

    private boolean samePhysicalTable(RelationTableDO relationTable, ConceptTableDO conceptTable) {
        if (relationTable == null || conceptTable == null
                || relationTable.getDatasourceId() == null || conceptTable.getDatasourceId() == null
                || !Objects.equals(relationTable.getDatasourceId(), conceptTable.getDatasourceId())
                || relationTable.getTableName() == null || conceptTable.getTableName() == null
                || !relationTable.getTableName().trim().equalsIgnoreCase(conceptTable.getTableName().trim())) {
            return false;
        }
        if (StringUtils.isNotBlank(relationTable.getDatabaseName())
                && StringUtils.isNotBlank(conceptTable.getDatabaseName())
                && !relationTable.getDatabaseName().trim().equalsIgnoreCase(conceptTable.getDatabaseName().trim())) {
            return false;
        }
        return StringUtils.isBlank(relationTable.getSchemaName())
                || StringUtils.isBlank(conceptTable.getSchemaName())
                || relationTable.getSchemaName().trim().equalsIgnoreCase(conceptTable.getSchemaName().trim());
    }

    /**
     * 按概念 + 可选表绑定ID 解析表绑定：指定时精确匹配，未指定取该概念第一张绑定表。
     */
    private ConceptTableDO resolveBinding(Long conceptId, Long tableBindingId) {
        List<ConceptTableDO> bindings = conceptTableMapper.selectByConceptId(conceptId);
        if (bindings == null || bindings.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        if (tableBindingId != null) {
            for (ConceptTableDO b : bindings) {
                if (tableBindingId.equals(b.getId())) {
                    return b;
                }
            }
            throw new RuntimeException("表绑定不存在: id=" + tableBindingId);
        }
        ConceptTableDO binding = bindings.get(0);
        if (binding.getDatasourceId() == null || binding.getTableName() == null) {
            throw new RuntimeException("对象集缺少数据源或物理表配置: bindingId=" + binding.getId());
        }
        return binding;
    }

    private ConceptDO resolveConcept(ObjectInstanceQueryReqVO reqVO) {
        if (reqVO.getConceptId() == null) {
            throw new RuntimeException("对象实例查询缺少概念ID");
        }
        ConceptDO concept = conceptMapper.selectById(reqVO.getConceptId());
        if (concept == null) {
            throw new RuntimeException("概念不存在: id=" + reqVO.getConceptId());
        }
        return concept;
    }

    private ConceptTableDO resolveTableBinding(ObjectInstanceQueryReqVO reqVO, Long conceptId) {
        List<ConceptTableDO> bindings = conceptTableMapper.selectByConceptId(conceptId);
        if (bindings == null || bindings.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表，无法查询对象实例: conceptId=" + conceptId);
        }
        ConceptTableDO binding;
        if (reqVO.getTableBindingId() != null) {
            binding = bindings.stream()
                    .filter(b -> reqVO.getTableBindingId().equals(b.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("表绑定不存在: id=" + reqVO.getTableBindingId()));
        } else {
            binding = bindings.get(0);
        }
        if (binding.getDatasourceId() == null || binding.getTableName() == null) {
            throw new RuntimeException("对象集缺少数据源或物理表配置: bindingId=" + binding.getId());
        }
        return binding;
    }

    private List<SemanticPropertyDTO> buildProperties(Long conceptTableId) {
        List<PropertyColumnDO> propertyColumns = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        if (propertyColumns == null || propertyColumns.isEmpty()) {
            return Collections.emptyList();
        }
        List<SemanticPropertyDTO> properties = new ArrayList<>();
        Map<Long, PropertyDO> propertyMap = new LinkedHashMap<>();
        for (PropertyColumnDO pc : propertyColumns) {
            if (pc.getPropertyId() == null || pc.getColumnName() == null) {
                continue;
            }
            PropertyDO prop = propertyMap.computeIfAbsent(pc.getPropertyId(), propertyMapper::selectById);
            if (prop == null) {
                continue;
            }
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

    private void assertTableAccess(ConceptTableDO binding, Long spaceId, String spaceCode) {
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
        long offset = (long) (pageNum - 1) * pageSize;

        // 列以元数据白名单为准（保证顺序与类型），取不到时退化为按首行 key 兜底
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
        List<String> whitelist = columns.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(columns);
        TypedQuerySpec spec = TypedQueryBuilder.parse(reqVO.getFilters());
        TypedQueryBuilder.Compiled compiled = TypedQueryBuilder.compile(spec, whitelist, textColumns);

        StringBuilder sql = new StringBuilder();
        if (compiled.hasProjection()) {
            sql.append("SELECT ").append(compiled.projection).append(" FROM ").append(binding.getTableName());
        } else {
            sql.append("SELECT * FROM ").append(binding.getTableName());
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
        List<Map<String, Object>> rows = pageResult.getData() == null
                ? Collections.emptyList() : pageResult.getData();
        int total = pageResult.getTotal() == null ? 0 : pageResult.getTotal();
        if (columns.isEmpty() && !rows.isEmpty()) {
            columns = new ArrayList<>(rows.get(0).keySet());
        }

        ObjectInstanceQueryRespVO respVO = new ObjectInstanceQueryRespVO();
        respVO.setConceptId(concept.getId());
        respVO.setConceptName(concept.getName());
        respVO.setTableBindingId(binding.getId());
        respVO.setTableName(binding.getTableName());
        respVO.setColumns(columns);
        respVO.setProperties(properties);
        respVO.setRows(rows);
        respVO.setTotal((long) total);
        return respVO;
    }
}
