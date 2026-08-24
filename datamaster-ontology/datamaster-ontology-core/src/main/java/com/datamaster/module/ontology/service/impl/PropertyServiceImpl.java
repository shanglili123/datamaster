package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.convert.PropertyConvert;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPageReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyRespVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertySaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.service.IPropertyService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

/**
 * 本体属性 Service 实现
 */
@Service
@Validated
public class PropertyServiceImpl implements IPropertyService {

    @Resource
    private PropertyMapper propertyMapper;

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
    public Integer deleteProperty(Long id) {
        return propertyMapper.deleteById(id);
    }
}
