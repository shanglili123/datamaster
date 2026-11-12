package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.convert.PropertyColumnConvert;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnRespVO;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.service.IPropertyColumnService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 属性字段绑定 Service 实现
 */
@Service
@Validated
public class PropertyColumnServiceImpl implements IPropertyColumnService {

    @Resource
    private PropertyColumnMapper propertyColumnMapper;
    @Resource private com.datamaster.module.ontology.dal.mapper.ConceptTableMapper tableMapper;
    @Resource private com.datamaster.module.ontology.dal.mapper.PropertyMapper propertyMapper;
    @Resource private com.datamaster.module.ontology.dal.mapper.ConceptMapper conceptMapper;
    @Resource private com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper metadataLocks;
    @Resource private com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard consumption;
    @Resource private com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext contexts;

    @Override
    public List<PropertyColumnRespVO> getPropertyColumnsByConceptTableId(Long conceptTableId) {
        consumption.ownership(tableMapper.selectById(conceptTableId));
        List<PropertyColumnDO> list = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        return PropertyColumnConvert.INSTANCE.convertList(list);
    }

    @Override
    public List<PropertyColumnRespVO> getPropertyColumnsByPropertyId(Long propertyId) {
        com.datamaster.module.ontology.dal.dataobject.PropertyDO property = propertyMapper.selectById(propertyId);
        if (property == null) throw new com.datamaster.common.exception.ServiceException("属性不存在");
        contexts.resolve(conceptMapper.selectById(property.getConceptId()), null, null, null);
        List<PropertyColumnDO> list = propertyColumnMapper.selectByPropertyId(propertyId);
        return PropertyColumnConvert.INSTANCE.convertList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSavePropertyColumns(Long conceptTableId, List<PropertyColumnSaveReqVO> list) {
        if (list == null) throw new com.datamaster.common.exception.ServiceException("字段映射替换必须提供列表，清空请明确传空数组");
        com.datamaster.module.ontology.dal.dataobject.ConceptTableDO table = tableMapper.selectById(conceptTableId);
        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumption.ownership(table);
        if (metadataLocks.lockOntology(ontology.getId()) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        com.datamaster.module.ontology.dal.dataobject.ConceptTableDO lockedTable = tableMapper.selectById(conceptTableId);
        if (lockedTable == null || !java.util.Objects.equals(lockedTable.getConceptId(), table.getConceptId())) throw new com.datamaster.common.exception.ServiceException("表绑定已变化，请刷新");
        if (list != null && list.size() > 200) throw new com.datamaster.common.exception.ServiceException("字段映射最多200项");
        java.util.Set<Long> properties = new java.util.HashSet<>();
        if (list != null) for (PropertyColumnSaveReqVO mapping : list) {
            if (mapping == null || mapping.getPropertyId() == null || !properties.add(mapping.getPropertyId())) throw new com.datamaster.common.exception.ServiceException("字段映射属性不能为空或重复");
            com.datamaster.module.ontology.dal.dataobject.PropertyDO property = propertyMapper.selectById(mapping.getPropertyId());
            if (property == null || !java.util.Objects.equals(property.getConceptId(), table.getConceptId())) throw new com.datamaster.common.exception.ServiceException("字段映射属性不属于当前概念");
            if (mapping.getConceptTableId() != null && !mapping.getConceptTableId().equals(conceptTableId)) throw new com.datamaster.common.exception.ServiceException("字段映射表归属不一致");
            com.datamaster.module.ontology.service.analysis.MetricQueryBuilder.identifier(mapping.getColumnName());
        }
        // Validate the entire replacement before deleting any stored mapping.
        propertyColumnMapper.deleteByConceptTableId(conceptTableId);
        // 批量插入
        if (list != null && !list.isEmpty()) {
            for (PropertyColumnSaveReqVO vo : list) {
                PropertyColumnDO entity = PropertyColumnConvert.INSTANCE.convert(vo);
                entity.setId(null);
                entity.setConceptTableId(conceptTableId);
                if (propertyColumnMapper.insert(entity) != 1) throw new com.datamaster.common.exception.ServiceException("字段映射保存失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deletePropertyColumn(Long id) {
        PropertyColumnDO mapping = propertyColumnMapper.selectById(id);
        if (mapping == null) throw new com.datamaster.common.exception.ServiceException("字段映射不存在");
        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumption.ownership(tableMapper.selectById(mapping.getConceptTableId()));
        if (metadataLocks.lockOntology(ontology.getId()) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        return propertyColumnMapper.deleteById(id);
    }
}
