package com.datamaster.module.ontology.service;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbTableMetadata;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.ontology.api.IObjectInstanceApiService;
import com.datamaster.module.ontology.api.dto.ObjectInstanceApiDTO;
import com.datamaster.module.ontology.api.dto.SemanticPropertyDTO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本体对象实例层实现 —— 供 AI 问数、对象浏览器等调用
 *
 * 对象集 = 概念 + 命中物理表 + 属性语义映射（只读语义信息，不含权限/SQL）。
 */
@Service
public class ObjectInstanceServiceImpl implements IObjectInstanceApiService {

    private static final Logger log = LoggerFactory.getLogger(ObjectInstanceServiceImpl.class);

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

    @Override
    public List<ObjectInstanceApiDTO> listObjectSets(Long ontologyId) {
        if (ontologyId == null) {
            return Collections.emptyList();
        }

        // 1. 查该本体下所有概念
        List<ConceptDO> concepts = conceptMapper.selectList(new LambdaQueryWrapperX<ConceptDO>()
                .eq(ConceptDO::getOntologyId, ontologyId)
                .orderByAsc(ConceptDO::getSortOrder)
                .orderByDesc(ConceptDO::getId));
        if (concepts == null || concepts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 为每个概念加载表绑定，同时收集属性列映射与属性元信息
        List<ObjectInstanceApiDTO> result = new ArrayList<>();
        for (ConceptDO concept : concepts) {
            List<ConceptTableDO> bindings = conceptTableMapper.selectByConceptId(concept.getId());
            if (bindings == null || bindings.isEmpty()) {
                continue; // 概念未绑定物理表，不构成对象集
            }
            for (ConceptTableDO binding : bindings) {
                if (binding.getDatasourceId() == null || binding.getTableName() == null) {
                    continue;
                }
                ObjectInstanceApiDTO dto = new ObjectInstanceApiDTO();
                dto.setConceptId(concept.getId());
                dto.setConceptName(concept.getName());
                dto.setConceptCode(concept.getCode());
                dto.setConceptDescription(concept.getDescription());
                dto.setOntologyId(concept.getOntologyId());
                dto.setTableBindingId(binding.getId());
                dto.setDatasourceId(binding.getDatasourceId());
                dto.setTableName(binding.getTableName());
                dto.setDatabaseName(binding.getDatabaseName());
                dto.setSchemaName(binding.getSchemaName());
                dto.setProperties(buildProperties(binding.getId()));
                applyPhysicalPkFallback(dto);
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * 组装概念在某表绑定上的属性语义列表（含物理列映射）。
     */
    private List<SemanticPropertyDTO> buildProperties(Long conceptTableId) {
        List<PropertyColumnDO> propertyColumns = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        if (propertyColumns == null || propertyColumns.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PropertyDO> propertyMap = new HashMap<>();
        List<SemanticPropertyDTO> properties = new ArrayList<>();
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

    /**
     * 物理表主键兜底（对象管理行操作定位）：
     * 对象集未配置任何属性级主键时，从物理表真实主键识别，并将匹配属性的 isPrimary 回填为 true，
     * 使「对象实例」列表的修改/删除能够亮起，并按物理主键精确定位记录。
     * 仅回填已映射到概念属性的列；未映射到属性的物理主键列无法通过属性传值，仍按无可定位主键处理。
     * 解析失败静默降级，不阻断对象集列表正常返回。
     */
    private void applyPhysicalPkFallback(ObjectInstanceApiDTO dto) {
        List<SemanticPropertyDTO> properties = dto.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        boolean hasPrimary = false;
        for (SemanticPropertyDTO p : properties) {
            if (Boolean.TRUE.equals(p.getIsPrimary())) {
                hasPrimary = true;
                break;
            }
        }
        if (hasPrimary) {
            return; // 已有属性级主键，无需兜底
        }
        Set<String> pkColumns = resolvePhysicalPkColumns(dto.getDatasourceId(), dto.getTableName());
        if (pkColumns.isEmpty()) {
            return;
        }
        for (SemanticPropertyDTO p : properties) {
            if (p.getPhysicalColumnName() != null
                    && pkColumns.contains(p.getPhysicalColumnName().toLowerCase())) {
                p.setIsPrimary(true);
            }
        }
    }

    /**
     * 解析物理表真实主键列（小写集合），失败返回空集合。
     * 通过数据源连接获取表元数据（DbQuery.getTableMetadata 含主键信息），
     * 与动作执行/回退链路的主键识别能力同源。
     */
    private Set<String> resolvePhysicalPkColumns(Long datasourceId, String tableName) {
        Set<String> pkColumns = new LinkedHashSet<>();
        if (datasourceId == null || tableName == null) {
            return pkColumns;
        }
        DbQuery dbQuery = null;
        try {
            DatasourceRespDTO ds = datasourceApiService.getDatasourceById(datasourceId);
            if (ds == null) {
                return pkColumns;
            }
            DbQueryProperty property = new DbQueryProperty(
                    ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
            dbQuery = dataSourceFactory.createDbQuery(property);
            DbTableMetadata metadata = dbQuery.getTableMetadata(property, tableName);
            String primaryKey = metadata == null ? null : metadata.getPrimaryKey();
            if (StringUtils.isNotBlank(primaryKey)) {
                for (String col : primaryKey.split(",")) {
                    if (StringUtils.isNotBlank(col)) {
                        pkColumns.add(col.trim().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("对象集物理表主键解析失败，跳过主键兜底: table={}, err={}", tableName, e.getMessage());
        } finally {
            if (dbQuery != null) {
                try {
                    dbQuery.close();
                } catch (Exception closeEx) {
                    log.debug("主键解析连接关闭失败", closeEx);
                }
            }
        }
        return pkColumns;
    }
}
