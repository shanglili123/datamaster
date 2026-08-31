package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import java.util.List;

/**
 * 属性字段绑定 Mapper
 */
public interface PropertyColumnMapper extends BaseMapperX<PropertyColumnDO> {

    default List<PropertyColumnDO> selectByConceptTableId(Long conceptTableId) {
        return selectList(new LambdaQueryWrapperX<PropertyColumnDO>()
                .eq(PropertyColumnDO::getConceptTableId, conceptTableId)
                .orderByAsc(PropertyColumnDO::getId));
    }

    default List<PropertyColumnDO> selectByPropertyId(Long propertyId) {
        return selectList(new LambdaQueryWrapperX<PropertyColumnDO>()
                .eq(PropertyColumnDO::getPropertyId, propertyId));
    }

    default int deleteByConceptTableId(Long conceptTableId) {
        return delete(new LambdaQueryWrapperX<PropertyColumnDO>()
                .eq(PropertyColumnDO::getConceptTableId, conceptTableId));
    }

    default int deleteByPropertyId(Long propertyId) {
        return delete(new LambdaQueryWrapperX<PropertyColumnDO>()
                .eq(PropertyColumnDO::getPropertyId, propertyId));
    }
}
