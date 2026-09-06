package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPageReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyRespVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertySaveReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPrimaryReqVO;

/**
 * 本体属性 Service 接口
 */
public interface IPropertyService {

    /**
     * 分页查询属性
     *
     * @param pageReqVO 分页查询参数
     * @return 属性分页结果
     */
    PageResult<PropertyRespVO> getPropertyPage(PropertyPageReqVO pageReqVO);

    /**
     * 根据编号获取属性详情
     *
     * @param id 编号
     * @return 属性详情
     */
    PropertyRespVO getPropertyById(Long id);

    /**
     * 创建属性
     *
     * @param createReqVO 创建参数
     * @return 新增编号
     */
    Long createProperty(PropertySaveReqVO createReqVO);

    /**
     * 更新属性
     *
     * @param updateReqVO 更新参数
     * @return 影响行数
     */
    Integer updateProperty(PropertySaveReqVO updateReqVO);

    /** 设置概念的主属性；普通概念一个，联合主键概念可多个。 */
    Integer setPrimaryProperties(PropertyPrimaryReqVO reqVO);

    /**
     * 删除属性
     *
     * @param id 编号
     * @return 影响行数
     */
    Integer deleteProperty(Long id);
}
