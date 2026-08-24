package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationPageReqVO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 本体关系 Mapper
 *
 * 对应表 ONT_RELATION
 */
public interface RelationMapper extends BaseMapperX<RelationDO> {

    default PageResult<RelationDO> selectPage(RelationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RelationDO>()
                .eq(reqVO.getOntologyId() != null, RelationDO::getOntologyId, reqVO.getOntologyId())
                .like(StringUtils.isNotBlank(reqVO.getName()), RelationDO::getName, reqVO.getName())
                .like(StringUtils.isNotBlank(reqVO.getCode()), RelationDO::getCode, reqVO.getCode())
                .eq(StringUtils.isNotBlank(reqVO.getRelationType()), RelationDO::getRelationType, reqVO.getRelationType())
                .orderByAsc(RelationDO::getSortOrder)
                .orderByDesc(RelationDO::getId));
    }
}
