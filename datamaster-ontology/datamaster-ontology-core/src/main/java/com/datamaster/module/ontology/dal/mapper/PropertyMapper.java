package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertyPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 本体属性 Mapper
 *
 * 对应表 ONT_PROPERTY
 */
public interface PropertyMapper extends BaseMapperX<PropertyDO> {

    default PageResult<PropertyDO> selectPage(PropertyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PropertyDO>()
                .eq(reqVO.getConceptId() != null, PropertyDO::getConceptId, reqVO.getConceptId())
                .like(StringUtils.isNotBlank(reqVO.getName()), PropertyDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getCode()), PropertyDO::getCode, reqVO.getCode())
                .eq(StringUtils.isNotBlank(reqVO.getDataType()), PropertyDO::getDataType, reqVO.getDataType())
                .orderByAsc(PropertyDO::getSortOrder)
                .orderByDesc(PropertyDO::getId));
    }
}
