package com.datamaster.module.ontology.service;

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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本体对象实例层实现 —— 供 AI 问数、对象浏览器等调用
 *
 * 对象集 = 概念 + 命中物理表 + 属性语义映射（只读语义信息，不含权限/SQL）。
 */
@Service
public class ObjectInstanceServiceImpl implements IObjectInstanceApiService {

    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private PropertyColumnMapper propertyColumnMapper;
    @Resource
    private PropertyMapper propertyMapper;
    @Resource
    private com.datamaster.module.ontology.service.analysis.MetricAnalysisAccessGuard access;

    @Override
    public List<ObjectInstanceApiDTO> listObjectSets(Long ontologyId) {
        access.ontology(ontologyId);

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
            if (concept == null || !java.util.Objects.equals(ontologyId, concept.getOntologyId())) throw new com.datamaster.common.exception.ServiceException("对象集概念归属无效");
            List<ConceptTableDO> bindings = conceptTableMapper.selectByConceptId(concept.getId());
            if (bindings == null || bindings.isEmpty()) {
                continue; // 概念未绑定物理表，不构成对象集
            }
            for (ConceptTableDO binding : bindings) {
                if (binding == null || !java.util.Objects.equals(concept.getId(), binding.getConceptId())) throw new com.datamaster.common.exception.ServiceException("对象集表绑定归属无效");
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
                dto.setProperties(buildProperties(binding.getId(), concept.getId()));
                // 对象集列表只负责返回本体绑定信息，不在这里建立外部数据库连接。
                // 之前的物理主键兜底会对每个对象集调用 getTableMetadata，数据源响应慢或不可达时
                // 会让“对象”面板一直转圈；真正查询数据时再按已配置的属性映射执行。
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * 组装概念在某表绑定上的属性语义列表（含物理列映射）。
     */
    private List<SemanticPropertyDTO> buildProperties(Long conceptTableId, Long conceptId) {
        List<PropertyColumnDO> propertyColumns = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        if (propertyColumns == null) throw new com.datamaster.common.exception.ServiceException("对象集字段映射读取不可用");
        if (propertyColumns.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PropertyDO> propertyMap = new HashMap<>();
        List<SemanticPropertyDTO> properties = new ArrayList<>();
        for (PropertyColumnDO pc : propertyColumns) {
            if (pc == null || pc.getPropertyId() == null || pc.getColumnName() == null || pc.getColumnName().isBlank()) throw new com.datamaster.common.exception.ServiceException("对象集属性映射不完整");
            PropertyDO prop = propertyMap.computeIfAbsent(pc.getPropertyId(), propertyMapper::selectById);
            if (prop == null || !java.util.Objects.equals(conceptId, prop.getConceptId())) throw new com.datamaster.common.exception.ServiceException("对象集属性不属于当前概念");
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

}
