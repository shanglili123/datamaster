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

    @Override
    public List<PropertyColumnRespVO> getPropertyColumnsByConceptTableId(Long conceptTableId) {
        List<PropertyColumnDO> list = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        return PropertyColumnConvert.INSTANCE.convertList(list);
    }

    @Override
    public List<PropertyColumnRespVO> getPropertyColumnsByPropertyId(Long propertyId) {
        List<PropertyColumnDO> list = propertyColumnMapper.selectByPropertyId(propertyId);
        return PropertyColumnConvert.INSTANCE.convertList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSavePropertyColumns(Long conceptTableId, List<PropertyColumnSaveReqVO> list) {
        // 先删除该概念表绑定下的所有列映射
        propertyColumnMapper.deleteByConceptTableId(conceptTableId);
        // 批量插入
        if (list != null && !list.isEmpty()) {
            for (PropertyColumnSaveReqVO vo : list) {
                vo.setConceptTableId(conceptTableId);
                PropertyColumnDO entity = PropertyColumnConvert.INSTANCE.convert(vo);
                propertyColumnMapper.insert(entity);
            }
        }
    }

    @Override
    public Integer deletePropertyColumn(Long id) {
        return propertyColumnMapper.deleteById(id);
    }
}
