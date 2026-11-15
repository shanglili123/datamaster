package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.PropertyConvert;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPageReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyRespVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertySaveReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPrimaryReqVO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.service.IPropertyService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * 本体属性 Service 实现
 */
@Service
@Validated
public class PropertyServiceImpl implements IPropertyService {

    @Resource
    private PropertyMapper propertyMapper;
    @Resource
    private PropertyColumnMapper propertyColumnMapper;
    @Resource private com.datamaster.module.ontology.dal.mapper.ConceptMapper concepts;
    @Resource private com.datamaster.module.ontology.service.analysis.OntologyConsumptionContext contexts;
    @Resource private com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper metadataLocks;

    private void assertConceptOwnership(Long conceptId) {
        com.datamaster.module.ontology.dal.dataobject.ConceptDO concept = concepts.selectById(conceptId);
        com.datamaster.module.ontology.dal.dataobject.OntologyDO owner = contexts.resolve(concept, null, null, null);
        if (metadataLocks.lockOntology(owner.getId()) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        com.datamaster.module.ontology.dal.dataobject.ConceptDO lockedConcept = concepts.selectById(conceptId);
        if (lockedConcept == null || !java.util.Objects.equals(owner.getId(), lockedConcept.getOntologyId())) throw new com.datamaster.common.exception.ServiceException("概念已变化，请刷新");
        contexts.resolve(lockedConcept, null, null, null);
    }

    @Override
    public PageResult<PropertyRespVO> getPropertyPage(PropertyPageReqVO pageReqVO) {
        contexts.resolve(concepts.selectById(pageReqVO.getConceptId()), null, null, null);
        PageResult<PropertyDO> pageResult = propertyMapper.selectPage(pageReqVO);
        List<PropertyRespVO> list = PropertyConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public PropertyRespVO getPropertyById(Long id) {
        PropertyDO property = propertyMapper.selectById(id);
        if (property != null) contexts.resolve(concepts.selectById(property.getConceptId()), null, null, null);
        return property != null ? PropertyConvert.INSTANCE.convert(property) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProperty(PropertySaveReqVO createReqVO) {
        if (createReqVO == null || createReqVO.getId() != null) throw new com.datamaster.common.exception.ServiceException("新建属性不得指定已有ID");
        assertConceptOwnership(createReqVO.getConceptId());
        PropertyDO property = PropertyConvert.INSTANCE.convert(createReqVO);
        if (propertyMapper.insert(property) != 1) throw new com.datamaster.common.exception.ServiceException("属性保存失败");
        return property.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateProperty(PropertySaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) throw new com.datamaster.common.exception.ServiceException("编辑属性必须明确ID");
        PropertyDO previous = propertyMapper.selectById(updateReqVO.getId());
        if (previous == null) throw new com.datamaster.common.exception.ServiceException("属性不存在");
        assertConceptOwnership(previous.getConceptId());
        if (updateReqVO.getConceptId() != null && !java.util.Objects.equals(previous.getConceptId(), updateReqVO.getConceptId())) throw new com.datamaster.common.exception.ServiceException("普通编辑不能变更属性所属概念");
        PropertyDO property = PropertyConvert.INSTANCE.convert(updateReqVO); property.setConceptId(previous.getConceptId());
        return propertyMapper.updateById(property);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer setPrimaryProperties(PropertyPrimaryReqVO reqVO) {
        if (reqVO == null || reqVO.getPropertyIds() == null || reqVO.getPropertyIds().isEmpty() || reqVO.getPropertyIds().stream().anyMatch(java.util.Objects::isNull) || reqVO.getPropertyIds().size() > 20) throw new com.datamaster.common.exception.ServiceException("主属性选择列表缺失或超过20项");
        assertConceptOwnership(reqVO.getConceptId());
        Set<Long> selectedIds = new HashSet<>(reqVO.getPropertyIds());
        List<PropertyDO> properties = propertyMapper.selectList(
                new com.datamaster.mybatis.core.query.LambdaQueryWrapperX<PropertyDO>()
                        .eq(PropertyDO::getConceptId, reqVO.getConceptId()));
        if (properties == null || properties.isEmpty()) {
            throw new RuntimeException("概念下没有可设置的属性");
        }
        Set<Long> validIds = new HashSet<>();
        for (PropertyDO property : properties) {
            validIds.add(property.getId());
        }
        if (!validIds.containsAll(selectedIds)) {
            throw new RuntimeException("所选主属性不属于当前概念");
        }
        for (Long propertyId : selectedIds) {
            if (propertyColumnMapper.selectByPropertyId(propertyId).isEmpty()) {
                throw new RuntimeException("主属性必须先绑定物理字段: propertyId=" + propertyId);
            }
        }
        propertyMapper.update(null, new LambdaUpdateWrapper<PropertyDO>()
                .set(PropertyDO::getIsPrimary, false)
                .eq(PropertyDO::getConceptId, reqVO.getConceptId()));
        return propertyMapper.update(null, new LambdaUpdateWrapper<PropertyDO>()
                .set(PropertyDO::getIsPrimary, true)
                .eq(PropertyDO::getConceptId, reqVO.getConceptId())
                .in(PropertyDO::getId, selectedIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteProperty(Long id) {
        PropertyDO previous = propertyMapper.selectById(id);
        if (previous == null) throw new com.datamaster.common.exception.ServiceException("属性不存在");
        assertConceptOwnership(previous.getConceptId());
        propertyColumnMapper.deleteByPropertyId(id);
        return propertyMapper.deleteById(id);
    }
}
