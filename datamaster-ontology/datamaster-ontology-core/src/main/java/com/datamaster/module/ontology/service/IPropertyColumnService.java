package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnRespVO;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnSaveReqVO;

import java.util.List;

/**
 * 属性字段绑定 Service 接口
 */
public interface IPropertyColumnService {

    List<PropertyColumnRespVO> getPropertyColumnsByConceptTableId(Long conceptTableId);

    List<PropertyColumnRespVO> getPropertyColumnsByPropertyId(Long propertyId);

    void batchSavePropertyColumns(Long conceptTableId, List<PropertyColumnSaveReqVO> list);

    Integer deletePropertyColumn(Long id);
}
