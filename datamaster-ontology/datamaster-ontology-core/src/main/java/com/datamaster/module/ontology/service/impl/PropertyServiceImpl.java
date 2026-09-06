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

    @Override
    public PageResult<PropertyRespVO> getPropertyPage(PropertyPageReqVO pageReqVO) {
        PageResult<PropertyDO> pageResult = propertyMapper.selectPage(pageReqVO);
        List<PropertyRespVO> list = PropertyConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public PropertyRespVO getPropertyById(Long id) {
        PropertyDO property = propertyMapper.selectById(id);
        return property != null ? PropertyConvert.INSTANCE.convert(property) : null;
    }

    @Override
    public Long createProperty(PropertySaveReqVO createReqVO) {
        PropertyDO property = PropertyConvert.INSTANCE.convert(createReqVO);
        propertyMapper.insert(property);
        return property.getId();
    }

    @Override
    public Integer updateProperty(PropertySaveReqVO updateReqVO) {
        PropertyDO property = PropertyConvert.INSTANCE.convert(updateReqVO);
        return propertyMapper.updateById(property);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer setPrimaryProperties(PropertyPrimaryReqVO reqVO) {
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
        propertyColumnMapper.deleteByPropertyId(id);
        return propertyMapper.deleteById(id);
    }
}
