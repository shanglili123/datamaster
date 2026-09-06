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
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.service.IObjectInstanceQueryService;
import com.datamaster.module.ontology.service.query.TypedQueryBuilder;
import com.datamaster.module.ontology.service.query.TypedQuerySpec;
import com.datamaster.neo4j.service.LineageDataService;
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

        // 4. 目标数据源 + 执行
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(targetBinding.getDatasourceId());
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + targetBinding.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            List<SemanticPropertyDTO> targetProperties = buildProperties(targetBinding.getId());
            return doRelatedQuery(reqVO, colBinding, targetBinding, targetConcept, targetProperties, dbQuery, property);
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
        List<Object> sourceValues = reqVO.getSourceValues() == null
                ? Collections.emptyList() : reqVO.getSourceValues();
        String targetCol = colBinding.getTargetColumn();
        if (whitelist.contains(targetCol) && !sourceValues.isEmpty()) {
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
