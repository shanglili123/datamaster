package com.datamaster.module.ontology.convert;

import com.datamaster.module.ontology.controller.admin.property.vo.PropertyRespVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertySaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 本体属性 Convert
 */
@Mapper
public interface PropertyConvert {

    PropertyConvert INSTANCE = Mappers.getMapper(PropertyConvert.class);

    /**
     * SaveReqVO 转 DO
     *
     * @param bean 保存 Request VO
     * @return DO
     */
    PropertyDO convert(PropertySaveReqVO bean);

    /**
     * DO 转 RespVO
     *
     * @param bean DO
     * @return Response VO
     */
    PropertyRespVO convert(PropertyDO bean);

    /**
     * DO 列表转 RespVO 列表
     *
     * @param list DO 列表
     * @return Response VO 列表
     */
    List<PropertyRespVO> convertList(List<PropertyDO> list);
}
