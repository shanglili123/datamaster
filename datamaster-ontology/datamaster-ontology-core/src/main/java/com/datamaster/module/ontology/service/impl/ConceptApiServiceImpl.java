package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.api.IConceptApiService;
import com.datamaster.module.ontology.api.dto.SemanticPropertyDTO;
import com.datamaster.module.ontology.api.dto.SemanticTableDTO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本体概念语义查询实现 — 供 AI 问数/Skill 生成调用
 *
 * 语义化表结构：概念元信息 + 属性业务语义 + 物理列映射。
 */
@Service
public class ConceptApiServiceImpl implements IConceptApiService {

    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private PropertyColumnMapper propertyColumnMapper;
    @Resource
    private PropertyMapper propertyMapper;

    @Override
    public List<SemanticTableDTO> listSemanticTablesByDatasource(Long datasourceId) {
        if (datasourceId == null) {
            return Collections.emptyList();
        }

        // 1. 查该数据源下所有概念表绑定
        List<ConceptTableDO> bindings = conceptTableMapper.selectByDatasourceId(datasourceId);
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量加载概念元信息
        Map<Long, ConceptDO> conceptMap = new HashMap<>();
        for (ConceptTableDO binding : bindings) {
            if (binding.getConceptId() != null && !conceptMap.containsKey(binding.getConceptId())) {
                ConceptDO concept = conceptMapper.selectById(binding.getConceptId());
                if (concept != null) {
                    conceptMap.put(concept.getId(), concept);
                }
            }
        }

        // 3. 为每个概念表绑定加载属性列映射，同时收集属性元信息
        Map<Long, List<PropertyColumnDO>> bindingColumnsMap = new HashMap<>();
        Map<Long, PropertyDO> propertyMap = new HashMap<>();
        for (ConceptTableDO binding : bindings) {
            if (binding.getId() == null) {
                continue;
            }
            List<PropertyColumnDO> propertyColumns = propertyColumnMapper.selectByConceptTableId(binding.getId());
            if (propertyColumns == null || propertyColumns.isEmpty()) {
                bindingColumnsMap.put(binding.getId(), Collections.emptyList());
                continue;
            }
            bindingColumnsMap.put(binding.getId(), propertyColumns);
            for (PropertyColumnDO pc : propertyColumns) {
                if (pc.getPropertyId() != null && !propertyMap.containsKey(pc.getPropertyId())) {
                    PropertyDO prop = propertyMapper.selectById(pc.getPropertyId());
                    if (prop != null) {
                        propertyMap.put(prop.getId(), prop);
                    }
                }
            }
        }

        // 4. 组装结果
        List<SemanticTableDTO> result = new ArrayList<>();
        for (ConceptTableDO binding : bindings) {
            ConceptDO concept = conceptMap.get(binding.getConceptId());
            if (concept == null) {
                continue; // 概念已删除，跳过
            }

            SemanticTableDTO table = new SemanticTableDTO();
            table.setConceptId(concept.getId());
            table.setConceptName(concept.getName());
            table.setConceptCode(concept.getCode());
            table.setConceptDescription(concept.getDescription());
            table.setDatasourceId(binding.getDatasourceId());
            table.setTableName(binding.getTableName());
            table.setDatabaseName(binding.getDatabaseName());
            table.setSchemaName(binding.getSchemaName());

            List<PropertyColumnDO> propertyColumns = bindingColumnsMap.get(binding.getId());
            List<SemanticPropertyDTO> properties = new ArrayList<>();
            if (propertyColumns != null) {
                for (PropertyColumnDO pc : propertyColumns) {
                    PropertyDO prop = propertyMap.get(pc.getPropertyId());
                    if (prop == null || pc.getColumnName() == null) {
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
            }
            table.setProperties(properties);
            result.add(table);
        }

        return result;
    }
}
